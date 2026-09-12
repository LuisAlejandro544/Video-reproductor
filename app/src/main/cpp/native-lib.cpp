/**
 * native-lib.cpp - Puente JNI entre Kotlin y C++ (Google Oboe)
 *
 * Implementa la interfaz JNI para la clase com.example.audio.OboeAudioEngine.
 * Permite que la capa de UI en Jetpack Compose y los interceptores de audio de video
 * transfieran paquetes PCM directamente a la tarjeta de sonido mediante Oboe (AAudio / OpenSL ES).
 *
 * Compatible con compilación en 32 y 64 bits (armeabi-v7a, arm64-v8a, x86, x86_64).
 */

#include <jni.h>
#include <android/log.h>
#include <memory>
#include "OboeAudioEngine.h"
#include "VideoColorEngine.h"

#define LOG_TAG "NovaPlayerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<OboeAudioEngine> sAudioEngine = nullptr;
static std::mutex sEngineMutex;

static std::unique_ptr<VideoColorEngine> sVideoColorEngine = nullptr;
static std::mutex sColorEngineMutex;

static OboeAudioEngine* getAudioEngine() {
    std::lock_guard<std::mutex> lock(sEngineMutex);
    if (!sAudioEngine) {
        sAudioEngine = std::make_unique<OboeAudioEngine>();
    }
    return sAudioEngine.get();
}

static VideoColorEngine* getVideoColorEngine() {
    std::lock_guard<std::mutex> lock(sColorEngineMutex);
    if (!sVideoColorEngine) {
        sVideoColorEngine = std::make_unique<VideoColorEngine>();
    }
    return sVideoColorEngine.get();
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("Módulo nativo C++ (novaplayer_native) cargado exitosamente.");
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_nativeInit(
    JNIEnv* env,
    jobject /* this */,
    jint sampleRate,
    jint channelCount
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jboolean>(engine->init(sampleRate, channelCount)) : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_nativeStart(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jboolean>(engine->start()) : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_nativePause(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jboolean>(engine->pause()) : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_nativeStop(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jboolean>(engine->stop()) : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_nativeRelease(
    JNIEnv* env,
    jobject /* this */
) {
    std::lock_guard<std::mutex> lock(sEngineMutex);
    if (sAudioEngine) {
        sAudioEngine->release();
        sAudioEngine.reset();
    }
}

JNIEXPORT jint JNICALL
Java_com_example_audio_OboeAudioEngine_nativeWrite(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray buffer,
    jint offset,
    jint length
) {
    auto engine = getAudioEngine();
    if (!engine || !buffer || length <= 0) {
        return 0;
    }

    jbyte* bytes = env->GetByteArrayElements(buffer, nullptr);
    if (!bytes) {
        return 0;
    }

    // Convertir bytes PCM (16-bit little-endian) a int16_t samples
    const int16_t* samples = reinterpret_cast<const int16_t*>(bytes + offset);
    int32_t numSamples = length / static_cast<jint>(sizeof(int16_t));

    int32_t written = engine->writeAudioData(samples, numSamples);

    env->ReleaseByteArrayElements(buffer, bytes, JNI_ABORT);
    return written * sizeof(int16_t);
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_nativeSetVolume(
    JNIEnv* env,
    jobject /* this */,
    jfloat volume
) {
    auto engine = getAudioEngine();
    if (engine) {
        engine->setVolume(volume);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_nativeIsPlaying(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jboolean>(engine->isPlaying()) : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_audio_OboeAudioEngine_nativeGetApiName(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    std::string apiName = engine ? engine->getAudioApiName() : "No inicializado";
    return env->NewStringUTF(apiName.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_audio_OboeAudioEngine_nativeGetSampleRate(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? engine->getSampleRate() : 0;
}

JNIEXPORT jint JNICALL
Java_com_example_audio_OboeAudioEngine_nativeGetChannelCount(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? engine->getChannelCount() : 0;
}

JNIEXPORT jlong JNICALL
Java_com_example_audio_OboeAudioEngine_nativeGetFramesWritten(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jlong>(engine->getFramesWritten()) : 0L;
}

// ============================================================================
// Funciones JNI para el Motor Gráfico OpenGL ES (NativeVideoFilter)
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_example_opengl_NativeVideoFilter_internalNativeInit(
    JNIEnv* env,
    jclass /* clazz */
) {
    auto engine = getVideoColorEngine();
    return engine ? static_cast<jboolean>(engine->init()) : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_opengl_NativeVideoFilter_nativeInit(
    JNIEnv* env,
    jobject /* this */
) {
    return Java_com_example_opengl_NativeVideoFilter_internalNativeInit(env, nullptr);
}

JNIEXPORT jboolean JNICALL
Java_com_example_opengl_NativeVideoFilter_internalNativeRender(
    JNIEnv* env,
    jclass /* clazz */,
    jint textureId,
    jfloatArray stMatrix,
    jfloatArray mvpMatrix,
    jfloat brightness,
    jfloat contrast,
    jfloat saturation,
    jfloat gamma,
    jfloat sharpness,
    jfloat texWidth,
    jfloat texHeight
) {
    auto engine = getVideoColorEngine();
    if (!engine || !stMatrix || !mvpMatrix) {
        return JNI_FALSE;
    }

    jfloat* stMat = env->GetFloatArrayElements(stMatrix, nullptr);
    jfloat* mvpMat = env->GetFloatArrayElements(mvpMatrix, nullptr);

    bool success = false;
    if (stMat && mvpMat) {
        success = engine->render(
            static_cast<GLuint>(textureId),
            stMat,
            mvpMat,
            brightness,
            contrast,
            saturation,
            gamma,
            sharpness,
            texWidth,
            texHeight
        );
    }

    if (stMat) env->ReleaseFloatArrayElements(stMatrix, stMat, JNI_ABORT);
    if (mvpMat) env->ReleaseFloatArrayElements(mvpMatrix, mvpMat, JNI_ABORT);

    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_opengl_NativeVideoFilter_nativeRender(
    JNIEnv* env,
    jobject /* this */,
    jint textureId,
    jfloatArray stMatrix,
    jfloatArray mvpMatrix,
    jfloat brightness,
    jfloat contrast,
    jfloat saturation,
    jfloat gamma,
    jfloat sharpness,
    jfloat texWidth,
    jfloat texHeight
) {
    return Java_com_example_opengl_NativeVideoFilter_internalNativeRender(
        env, nullptr, textureId, stMatrix, mvpMatrix, brightness, contrast,
        saturation, gamma, sharpness, texWidth, texHeight
    );
}

JNIEXPORT void JNICALL
Java_com_example_opengl_NativeVideoFilter_internalNativeRelease(
    JNIEnv* env,
    jclass /* clazz */
) {
    std::lock_guard<std::mutex> lock(sColorEngineMutex);
    if (sVideoColorEngine) {
        sVideoColorEngine->release();
        sVideoColorEngine.reset();
    }
}

JNIEXPORT void JNICALL
Java_com_example_opengl_NativeVideoFilter_nativeRelease(
    JNIEnv* env,
    jobject /* this */
) {
    Java_com_example_opengl_NativeVideoFilter_internalNativeRelease(env, nullptr);
}

} // extern "C"
