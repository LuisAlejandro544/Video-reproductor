#include "OboeAudioEngine.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>

#define LOG_TAG "OboeAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Tamaño máximo del buffer de audio interno (aproximadamente 500ms de audio estéreo a 48kHz)
static constexpr size_t MAX_BUFFER_SAMPLES = 48000 * 2;

OboeAudioEngine::OboeAudioEngine()
    : mSampleRate(48000)
    , mChannelCount(2)
    , mVolume(1.0f)
    , mIsPlaying(false)
    , mReadIndex(0)
    , mFramesWritten(0) {
    mAudioBuffer.reserve(MAX_BUFFER_SAMPLES);
}

OboeAudioEngine::~OboeAudioEngine() {
    release();
}

bool OboeAudioEngine::init(int32_t sampleRate, int32_t channelCount) {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    
    if (sampleRate > 0) {
        mSampleRate = sampleRate;
    }
    if (channelCount > 0) {
        mChannelCount = channelCount;
    }

    mAudioBuffer.clear();
    mReadIndex = 0;
    mFramesWritten = 0;

    LOGI("Inicializando OboeAudioEngine - SampleRate: %d, Canales: %d", mSampleRate, mChannelCount);
    return openStream();
}

bool OboeAudioEngine::openStream() {
    closeStream();

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::I16)
        ->setChannelCount(mChannelCount)
        ->setSampleRate(mSampleRate)
        ->setCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Error abriendo flujo Oboe: %s", oboe::convertToText(result));
        return false;
    }

    LOGI("Flujo Oboe abierto exitosamente. API usada: %s, FramesPerBurst: %d",
         getAudioApiName().c_str(), mStream->getFramesPerBurst());
    return true;
}

void OboeAudioEngine::closeStream() {
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}

bool OboeAudioEngine::start() {
    if (!mStream) {
        if (!openStream()) return false;
    }
    
    oboe::Result result = mStream->requestStart();
    if (result == oboe::Result::OK) {
        mIsPlaying = true;
        LOGI("Flujo Oboe iniciado.");
        return true;
    } else {
        LOGE("Error al iniciar flujo Oboe: %s", oboe::convertToText(result));
        return false;
    }
}

bool OboeAudioEngine::pause() {
    if (mStream && mIsPlaying) {
        oboe::Result result = mStream->requestPause();
        mIsPlaying = false;
        LOGI("Flujo Oboe pausado: %s", oboe::convertToText(result));
        return result == oboe::Result::OK;
    }
    return true;
}

bool OboeAudioEngine::stop() {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mAudioBuffer.clear();
    mReadIndex = 0;
    
    if (mStream) {
        oboe::Result result = mStream->requestStop();
        mIsPlaying = false;
        LOGI("Flujo Oboe detenido: %s", oboe::convertToText(result));
        return result == oboe::Result::OK;
    }
    return true;
}

void OboeAudioEngine::release() {
    stop();
    closeStream();
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mAudioBuffer.clear();
    mReadIndex = 0;
}

int32_t OboeAudioEngine::writeAudioData(const int16_t* audioData, int32_t numSamples) {
    if (!audioData || numSamples <= 0) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(mBufferMutex);

    // Si el índice de lectura avanzó, compactamos el buffer para no consumir memoria infinita
    if (mReadIndex > 0) {
        if (mReadIndex >= mAudioBuffer.size()) {
            mAudioBuffer.clear();
            mReadIndex = 0;
        } else if (mReadIndex > 8192) {
            mAudioBuffer.erase(mAudioBuffer.begin(), mAudioBuffer.begin() + mReadIndex);
            mReadIndex = 0;
        }
    }

    // Limitar el buffer para evitar sobrellenado durante saltos o pausas prolongadas
    size_t availableCapacity = MAX_BUFFER_SAMPLES > mAudioBuffer.size() ?
                               MAX_BUFFER_SAMPLES - mAudioBuffer.size() : 0;
    size_t samplesToInsert = std::min(static_cast<size_t>(numSamples), availableCapacity);

    if (samplesToInsert > 0) {
        mAudioBuffer.insert(mAudioBuffer.end(), audioData, audioData + samplesToInsert);
        mFramesWritten += (samplesToInsert / mChannelCount);
    }

    return static_cast<int32_t>(samplesToInsert);
}

void OboeAudioEngine::setVolume(float volume) {
    mVolume = std::max(0.0f, std::min(1.0f, volume));
}

bool OboeAudioEngine::isPlaying() const {
    return mIsPlaying;
}

std::string OboeAudioEngine::getAudioApiName() const {
    if (!mStream) return "None";
    switch (mStream->getAudioApi()) {
        case oboe::AudioApi::AAudio:
            return "AAudio (Nativo Android 8+)";
        case oboe::AudioApi::OpenSLES:
            return "OpenSL ES (Fallback)";
        default:
            return "Desconocido";
    }
}

int32_t OboeAudioEngine::getSampleRate() const {
    return mStream ? mStream->getSampleRate() : mSampleRate;
}

int32_t OboeAudioEngine::getChannelCount() const {
    return mStream ? mStream->getChannelCount() : mChannelCount;
}

int64_t OboeAudioEngine::getFramesWritten() const {
    return mFramesWritten;
}

oboe::DataCallbackResult OboeAudioEngine::onAudioReady(
    oboe::AudioStream* audioStream,
    void* audioData,
    int32_t numFrames
) {
    int16_t* outputBuffer = static_cast<int16_t*>(audioData);
    int32_t totalSamplesNeeded = numFrames * mChannelCount;

    std::lock_guard<std::mutex> lock(mBufferMutex);

    size_t samplesAvailable = (mAudioBuffer.size() > mReadIndex) ? (mAudioBuffer.size() - mReadIndex) : 0;
    size_t samplesToCopy = std::min(static_cast<size_t>(totalSamplesNeeded), samplesAvailable);

    for (size_t i = 0; i < samplesToCopy; ++i) {
        float sample = static_cast<float>(mAudioBuffer[mReadIndex + i]) * mVolume;
        // Clamp a 16-bit signed integer
        sample = std::max(-32768.0f, std::min(32767.0f, sample));
        outputBuffer[i] = static_cast<int16_t>(sample);
    }
    mReadIndex += samplesToCopy;

    // Si faltan muestras para completar el frame requerido por el hardware, rellenar con silencio
    if (samplesToCopy < static_cast<size_t>(totalSamplesNeeded)) {
        std::memset(outputBuffer + samplesToCopy, 0, (totalSamplesNeeded - samplesToCopy) * sizeof(int16_t));
    }

    return oboe::DataCallbackResult::Continue;
}

void OboeAudioEngine::onErrorBeforeClose(oboe::AudioStream* audioStream, oboe::Result error) {
    LOGW("Oboe onErrorBeforeClose: %s", oboe::convertToText(error));
}

void OboeAudioEngine::onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result error) {
    LOGE("Oboe onErrorAfterClose: %s. Reabriendo flujo...", oboe::convertToText(error));
    openStream();
}
