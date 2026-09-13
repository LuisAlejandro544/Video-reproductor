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
#include <string>
#include <vector>
#include <vulkan/vulkan.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include "OboeAudioEngine.h"
#include "VideoColorEngine.h"
#include "vulkan/VulkanVideoEngine.h"

#define LOG_TAG "NovaPlayerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<OboeAudioEngine> sAudioEngine = nullptr;
static std::mutex sEngineMutex;

static std::unique_ptr<VideoColorEngine> sVideoColorEngine = nullptr;
static std::mutex sColorEngineMutex;

static std::unique_ptr<VulkanVideoEngine> sVulkanVideoEngine = nullptr;
static std::mutex sVulkanEngineMutex;

static VulkanVideoEngine* getVulkanVideoEngine() {
    std::lock_guard<std::mutex> lock(sVulkanEngineMutex);
    if (!sVulkanVideoEngine) {
        sVulkanVideoEngine = std::make_unique<VulkanVideoEngine>();
    }
    return sVulkanVideoEngine.get();
}

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
Java_com_example_audio_OboeAudioEngine_nativeFlush(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    if (engine) {
        engine->flush();
    }
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

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_nativeSetDynamicCompressor(
    JNIEnv* env,
    jobject /* this */,
    jboolean enabled,
    jfloat intensity
) {
    auto engine = getAudioEngine();
    if (engine) {
        engine->setDynamicCompressor(enabled == JNI_TRUE, static_cast<float>(intensity));
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_nativeSetVoiceClarity(
    JNIEnv* env,
    jobject /* this */,
    jboolean enabled,
    jfloat gain
) {
    auto engine = getAudioEngine();
    if (engine) {
        engine->setVoiceClarity(enabled == JNI_TRUE, static_cast<float>(gain));
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_nativeSetChannelMode(
    JNIEnv* env,
    jobject /* this */,
    jint mode
) {
    auto engine = getAudioEngine();
    if (engine) {
        engine->setChannelMode(static_cast<int32_t>(mode));
    }
}

JNIEXPORT jint JNICALL
Java_com_example_audio_OboeAudioEngine_nativeGetChannelMode(
    JNIEnv* env,
    jobject /* this */
) {
    auto engine = getAudioEngine();
    return engine ? static_cast<jint>(engine->getChannelMode()) : 0;
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
    jfloat texHeight,
    jfloat blueLightFilter,
    jfloat blurRadius,
    jfloat backgroundDim,
    jfloat fsrEnabled,
    jfloat fsrSharpness,
    jfloat sunMode,
    jfloat anime4kMode,
    jfloat anime4kStrength
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
            texHeight,
            blueLightFilter,
            blurRadius,
            backgroundDim,
            fsrEnabled,
            fsrSharpness,
            sunMode,
            anime4kMode,
            anime4kStrength
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
    jfloat texHeight,
    jfloat blueLightFilter,
    jfloat blurRadius,
    jfloat backgroundDim,
    jfloat fsrEnabled,
    jfloat fsrSharpness,
    jfloat sunMode,
    jfloat anime4kMode,
    jfloat anime4kStrength
) {
    return Java_com_example_opengl_NativeVideoFilter_internalNativeRender(
        env, nullptr, textureId, stMatrix, mvpMatrix, brightness, contrast,
        saturation, gamma, sharpness, texWidth, texHeight,
        blueLightFilter, blurRadius, backgroundDim,
        fsrEnabled, fsrSharpness, sunMode,
        anime4kMode, anime4kStrength
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

/**
 * Consulta de estado y capacidades de Vulkan a nivel de driver nativo.
 *
 * Determina la versión del loader mediante vkGetInstanceProcAddr, inicializa una
 * instancia temporal ligera y recupera las propiedades físicas de la GPU (nombre del
 * dispositivo y versión del driver de hardware).
 */
JNIEXPORT jstring JNICALL
Java_com_example_vulkan_VulkanCapabilities_nativeQueryVulkanDriver(
    JNIEnv* env,
    jclass /* clazz */
) {
    // 1. Obtener función vkEnumerateInstanceVersion dinámicamente si está disponible
    auto pfnEnumerateInstanceVersion = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
        vkGetInstanceProcAddr(nullptr, "vkEnumerateInstanceVersion")
    );

    uint32_t instanceVersion = VK_API_VERSION_1_0;
    if (pfnEnumerateInstanceVersion) {
        VkResult res = pfnEnumerateInstanceVersion(&instanceVersion);
        if (res != VK_SUCCESS) {
            instanceVersion = VK_API_VERSION_1_0;
        }
    }

    uint32_t major = VK_VERSION_MAJOR(instanceVersion);
    uint32_t minor = VK_VERSION_MINOR(instanceVersion);
    uint32_t patch = VK_VERSION_PATCH(instanceVersion);

    // 2. Crear instancia temporal básica de Vulkan para enumerar GPUs físicas
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Nova Video Player";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "NovaVulkanEngine";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = instanceVersion;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = 0;
    createInfo.ppEnabledExtensionNames = nullptr;
    createInfo.enabledLayerCount = 0;
    createInfo.ppEnabledLayerNames = nullptr;

    VkInstance instance = VK_NULL_HANDLE;
    VkResult res = vkCreateInstance(&createInfo, nullptr, &instance);
    if (res != VK_SUCCESS) {
        // Fallback a API 1.0 si falló la versión solicitada
        appInfo.apiVersion = VK_API_VERSION_1_0;
        res = vkCreateInstance(&createInfo, nullptr, &instance);
    }

    if (res != VK_SUCCESS) {
        std::string err = "ERROR|No se pudo inicializar instancia Vulkan (Código VkResult: " + std::to_string(res) + ")";
        return env->NewStringUTF(err.c_str());
    }

    // 3. Enumerar GPUs disponibles
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);

    if (deviceCount == 0) {
        vkDestroyInstance(instance, nullptr);
        return env->NewStringUTF("ERROR|No se detectaron dispositivos físicos Vulkan en el hardware.");
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    VkPhysicalDeviceProperties props{};
    vkGetPhysicalDeviceProperties(devices[0], &props);

    std::string deviceName = props.deviceName;
    uint32_t driverMajor = VK_VERSION_MAJOR(props.driverVersion);
    uint32_t driverMinor = VK_VERSION_MINOR(props.driverVersion);
    uint32_t driverPatch = VK_VERSION_PATCH(props.driverVersion);
    std::string driverVer = "v" + std::to_string(driverMajor) + "." + std::to_string(driverMinor) + "." + std::to_string(driverPatch);

    std::string extraInfo = "Instancia: v" + std::to_string(major) + "." + std::to_string(minor) + "." + std::to_string(patch) +
                            " | GPUs disponibles: " + std::to_string(deviceCount);

    vkDestroyInstance(instance, nullptr);

    std::string response = "OK|" + deviceName + "|" + driverVer + "|" + extraInfo;
    return env->NewStringUTF(response.c_str());
}

// ============================================================================
// Funciones JNI para el Motor Gráfico Vulkan 1.1+ (NativeVulkanVideoEngine)
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeInit(
    JNIEnv* env,
    jclass /* clazz */,
    jobject surface,
    jint width,
    jint height
) {
    if (!surface) {
        LOGE("Surface proporcionado a Vulkan es nulo.");
        return JNI_FALSE;
    }

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        LOGE("No se pudo obtener ANativeWindow desde Surface.");
        return JNI_FALSE;
    }

    auto engine = getVulkanVideoEngine();
    bool success = engine ? engine->init(window, static_cast<int>(width), static_cast<int>(height)) : false;

    // Liberar referencia local de ANativeWindow_fromSurface
    ANativeWindow_release(window);
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeResize(
    JNIEnv* env,
    jclass /* clazz */,
    jint width,
    jint height
) {
    auto engine = getVulkanVideoEngine();
    return (engine && engine->resize(static_cast<int>(width), static_cast<int>(height))) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeImportHardwareBuffer(
    JNIEnv* env,
    jclass /* clazz */,
    jobject hardwareBufferObj
) {
    if (!hardwareBufferObj) {
        return JNI_FALSE;
    }

    AHardwareBuffer* buffer = AHardwareBuffer_fromHardwareBuffer(env, hardwareBufferObj);
    if (!buffer) {
        return JNI_FALSE;
    }

    auto engine = getVulkanVideoEngine();
    bool success = engine ? engine->importHardwareBuffer(buffer) : false;

    AHardwareBuffer_release(buffer);
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeRender(
    JNIEnv* env,
    jclass /* clazz */,
    jfloat brightness,
    jfloat contrast,
    jfloat saturation,
    jfloat gamma,
    jfloat sharpness,
    jfloat blueLightFilter,
    jfloat sunMode
) {
    auto engine = getVulkanVideoEngine();
    if (!engine) {
        return JNI_FALSE;
    }

    bool success = engine->renderFrame(
        static_cast<float>(brightness),
        static_cast<float>(contrast),
        static_cast<float>(saturation),
        static_cast<float>(gamma),
        static_cast<float>(sharpness),
        static_cast<float>(blueLightFilter),
        static_cast<float>(sunMode)
    );

    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeRelease(
    JNIEnv* env,
    jclass /* clazz */
) {
    std::lock_guard<std::mutex> lock(sVulkanEngineMutex);
    if (sVulkanVideoEngine) {
        sVulkanVideoEngine->release();
        sVulkanVideoEngine.reset();
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeGetLastError(
    JNIEnv* env,
    jclass /* clazz */
) {
    auto engine = getVulkanVideoEngine();
    std::string err = engine ? engine->getLastError() : "Motor Vulkan no disponible.";
    return env->NewStringUTF(err.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_vulkan_NativeVulkanVideoEngine_nativeIsHardwareBufferSupported(
    JNIEnv* env,
    jclass /* clazz */
) {
    auto engine = getVulkanVideoEngine();
    return (engine && engine->isHardwareBufferExtensionSupported()) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
