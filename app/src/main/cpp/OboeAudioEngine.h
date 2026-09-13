#ifndef OBOE_AUDIO_ENGINE_H
#define OBOE_AUDIO_ENGINE_H

#include <oboe/Oboe.h>
#include <vector>
#include <mutex>
#include <memory>
#include <string>

/**
 * OboeAudioEngine - Motor nativo de audio de ultra baja latencia
 *
 * Utiliza la biblioteca oficial Google Oboe (Licencia Apache 2.0).
 * Diseñado para compatibilidad en arquitecturas de 32 bits (armeabi-v7a, x86)
 * y 64 bits (arm64-v8a, x86_64).
 *
 * En Android 8.0+ (API 26+), utiliza AAudio automáticamente para el rendimiento
 * más rápido de la tarjeta de sonido. En hardware legacy hace fallback a OpenSL ES.
 */
class OboeAudioEngine : public oboe::AudioStreamCallback {
public:
    OboeAudioEngine();
    ~OboeAudioEngine();

    bool init(int32_t sampleRate, int32_t channelCount);
    bool start();
    bool pause();
    bool stop();
    void flush();
    void release();

    int32_t writeAudioData(const int16_t* audioData, int32_t numSamples);
    void setVolume(float volume);

    // DSP: Compresor Dinámico (Night Mode) y Realce de Diálogos (Voice Clarity)
    void setDynamicCompressor(bool enabled, float intensity);
    void setVoiceClarity(bool enabled, float gain);
    bool isDynamicCompressorEnabled() const { return mCompressorEnabled; }
    bool isVoiceClarityEnabled() const { return mVoiceClarityEnabled; }

    // DSP: Modo de Canales en tiempo real (0 = Estéreo Nativo, 1 = Mono Combinado, 2 = Pseudo-Estéreo Haas)
    void setChannelMode(int32_t mode);
    int32_t getChannelMode() const { return mChannelMode; }

    bool isPlaying() const;
    std::string getAudioApiName() const;
    int32_t getSampleRate() const;
    int32_t getChannelCount() const;
    int64_t getFramesWritten() const;

    // Métodos de AudioStreamCallback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames
    ) override;

    void onErrorBeforeClose(oboe::AudioStream* audioStream, oboe::Result error) override;
    void onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result error) override;

private:
    bool openStream();
    void closeStream();

    std::shared_ptr<oboe::AudioStream> mStream;
    int32_t mSampleRate;
    int32_t mChannelCount;
    float mVolume;
    bool mIsPlaying;

    // Parámetros DSP en tiempo real
    bool mCompressorEnabled;
    float mCompressorIntensity;
    bool mVoiceClarityEnabled;
    float mVoiceClarityGain;
    float mEnvelope;
    float mVoicePrevLowPass;

    int32_t mChannelMode;             // 0 = Stereo, 1 = Mono, 2 = Spatial Haas
    std::vector<float> mHaasBuffer;   // Buffer de retardo interaural para pseudo-estéreo
    size_t mHaasIndex;

    std::mutex mBufferMutex;
    std::vector<int16_t> mRingBuffer;
    size_t mWriteIndex;
    size_t mReadIndex;
    size_t mAvailableSamples;
    int64_t mFramesWritten;
};

#endif // OBOE_AUDIO_ENGINE_H
