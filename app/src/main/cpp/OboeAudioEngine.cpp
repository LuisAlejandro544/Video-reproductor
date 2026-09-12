#include "OboeAudioEngine.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>

#define LOG_TAG "OboeAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Tamaño del buffer circular estático (~2 segundos de audio estéreo a 48kHz = 192,000 muestras = 384 KB)
// Garantiza cero reasignaciones de memoria en tiempo de reproducción y absorbe ráfagas de decodificación
static constexpr size_t RING_BUFFER_CAPACITY = 48000 * 2 * 2;

OboeAudioEngine::OboeAudioEngine()
    : mSampleRate(48000)
    , mChannelCount(2)
    , mVolume(1.0f)
    , mIsPlaying(false)
    , mCompressorEnabled(false)
    , mCompressorIntensity(0.8f)
    , mVoiceClarityEnabled(false)
    , mVoiceClarityGain(0.75f)
    , mEnvelope(0.0f)
    , mVoicePrevLowPass(0.0f)
    , mWriteIndex(0)
    , mReadIndex(0)
    , mAvailableSamples(0)
    , mFramesWritten(0) {
    mRingBuffer.assign(RING_BUFFER_CAPACITY, 0);
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

    mWriteIndex = 0;
    mReadIndex = 0;
    mAvailableSamples = 0;
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
        ->setUsage(oboe::Usage::Media)
        ->setContentType(oboe::ContentType::Movie)
        ->setFormat(oboe::AudioFormat::I16)
        ->setChannelCount(mChannelCount)
        ->setSampleRate(mSampleRate)
        ->setCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Error abriendo flujo Oboe: %s", oboe::convertToText(result));
        return false;
    }

    LOGI("Flujo Oboe abierto exitosamente. API: %s, FramesPerBurst: %d, Usage: Media, ContentType: Movie",
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
    
    oboe::StreamState state = mStream->getState();
    if (state == oboe::StreamState::Started) {
        mIsPlaying = true;
        return true;
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

void OboeAudioEngine::flush() {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mWriteIndex = 0;
    mReadIndex = 0;
    mAvailableSamples = 0;
    LOGI("Flujo Oboe: buffer vaciado (flush) instantáneamente sin detener hardware.");
}

bool OboeAudioEngine::stop() {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mWriteIndex = 0;
    mReadIndex = 0;
    mAvailableSamples = 0;
    
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
    mWriteIndex = 0;
    mReadIndex = 0;
    mAvailableSamples = 0;
}

int32_t OboeAudioEngine::writeAudioData(const int16_t* audioData, int32_t numSamples) {
    if (!audioData || numSamples <= 0) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(mBufferMutex);

    size_t freeSpace = (RING_BUFFER_CAPACITY > mAvailableSamples) ? (RING_BUFFER_CAPACITY - mAvailableSamples) : 0;
    size_t samplesToInsert = std::min(static_cast<size_t>(numSamples), freeSpace);

    for (size_t i = 0; i < samplesToInsert; ++i) {
        mRingBuffer[(mWriteIndex + i) % RING_BUFFER_CAPACITY] = audioData[i];
    }
    mWriteIndex = (mWriteIndex + samplesToInsert) % RING_BUFFER_CAPACITY;
    mAvailableSamples += samplesToInsert;
    mFramesWritten += (samplesToInsert / mChannelCount);

    return static_cast<int32_t>(samplesToInsert);
}

void OboeAudioEngine::setVolume(float volume) {
    mVolume = std::max(0.0f, std::min(1.0f, volume));
}

void OboeAudioEngine::setDynamicCompressor(bool enabled, float intensity) {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mCompressorEnabled = enabled;
    mCompressorIntensity = std::max(0.0f, std::min(1.0f, intensity));
    LOGI("Compresor Dinámico (Night Mode): %s (Intensidad: %.2f)", enabled ? "ON" : "OFF", mCompressorIntensity);
}

void OboeAudioEngine::setVoiceClarity(bool enabled, float gain) {
    std::lock_guard<std::mutex> lock(mBufferMutex);
    mVoiceClarityEnabled = enabled;
    mVoiceClarityGain = std::max(0.0f, std::min(1.0f, gain));
    LOGI("Modo Voces Claras: %s (Ganancia: %.2f)", enabled ? "ON" : "OFF", mVoiceClarityGain);
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

    size_t samplesToCopy = std::min(static_cast<size_t>(totalSamplesNeeded), mAvailableSamples);

    // Compensación de volumen maestro (1.40f) para igualar sonoridad con AudioTrack de Media3
    const float masterGain = mVolume * 1.40f;

    for (size_t i = 0; i < samplesToCopy; ++i) {
        int16_t rawSample = mRingBuffer[(mReadIndex + i) % RING_BUFFER_CAPACITY];
        float sample = static_cast<float>(rawSample) * masterGain;

        // 1. Realce de Diálogos / Voces Claras (Peaking en banda vocal 1.5 kHz - 3.5 kHz)
        if (mVoiceClarityEnabled && mVoiceClarityGain > 0.01f) {
            float lowPass = 0.72f * mVoicePrevLowPass + 0.28f * sample;
            mVoicePrevLowPass = lowPass;
            float voiceBand = sample - lowPass;
            sample += voiceBand * (mVoiceClarityGain * 1.35f);
        }

        // 2. Compresor Dinámico / Modo Nocturno (DRC - atenúa picos/explosiones, eleva susurros)
        if (mCompressorEnabled && mCompressorIntensity > 0.01f) {
            float absSample = std::abs(sample);
            if (absSample > mEnvelope) {
                mEnvelope = 0.08f * absSample + 0.92f * mEnvelope;
            } else {
                mEnvelope = 0.002f * absSample + 0.998f * mEnvelope;
            }

            float threshold = 9500.0f * (1.0f - mCompressorIntensity * 0.35f);
            if (mEnvelope > threshold) {
                float excess = mEnvelope - threshold;
                float ratio = 3.5f + mCompressorIntensity * 4.5f;
                float compressedEnvelope = threshold + (excess / ratio);
                float gainReduction = compressedEnvelope / std::max(1.0f, mEnvelope);
                sample *= gainReduction;
            } else if (mEnvelope > 80.0f && mEnvelope < threshold * 0.45f) {
                float quietBoost = 1.0f + (mCompressorIntensity * 0.65f) * (1.0f - (mEnvelope / (threshold * 0.45f)));
                sample *= quietBoost;
            }
        }

        // Clamp a 16-bit signed integer con protección suave contra distorsión
        sample = std::max(-32767.0f, std::min(32767.0f, sample));
        outputBuffer[i] = static_cast<int16_t>(sample);
    }
    mReadIndex = (mReadIndex + samplesToCopy) % RING_BUFFER_CAPACITY;
    mAvailableSamples -= samplesToCopy;

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
