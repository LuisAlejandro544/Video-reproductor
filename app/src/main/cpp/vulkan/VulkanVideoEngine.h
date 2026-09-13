/**
 * VulkanVideoEngine.h - Motor nativo de renderizado gráfico Vulkan 1.1+ para Nova Video Player
 *
 * Propósito:
 * Implementa el pipeline gráfico de bajo nivel mediante la API Vulkan para procesar
 * y presentar tramas de video con consumo mínimo de CPU y sin sobrecalentar el terminal.
 *
 * Características clave:
 * 1. Conexión de Video en Memoria Compartida (Zero-Copy con AHardwareBuffer):
 *    Importa tramas decodificadas por hardware directamente en memoria GPU mediante
 *    VK_ANDROID_external_memory_android_hardware_buffer y sampler YCbCr (Vulkan 1.1).
 * 2. Carga y ejecución de sombreadores en bytecode binario SPIR-V (VulkanSpirvShaders.h).
 * 3. Gestión de ciclo de vida con ANativeWindow (SurfaceView en Android).
 * 4. Manejo de errores detallado y compatibilidad con el Sistema de Fallback Automático.
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#ifndef NOVA_VULKAN_VIDEO_ENGINE_H
#define NOVA_VULKAN_VIDEO_ENGINE_H

#include <android/native_window.h>
#include <android/hardware_buffer.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <string>
#include <vector>
#include <mutex>
#include <memory>

class VulkanVideoEngine {
public:
    VulkanVideoEngine();
    ~VulkanVideoEngine();

    /**
     * Inicializa la instancia, el dispositivo lógico y la superficie Vulkan conectada a ANativeWindow.
     * Si el hardware o los controladores no cumplen los requisitos, retorna false y guarda el motivo
     * en getLastError() para que la capa de UI ejecute la Degradación Elegante a OpenGL ES.
     */
    bool init(ANativeWindow* window, int width, int height);

    /**
     * Reconfigura el Swapchain y el viewport al rotar la pantalla o alterar la relación de aspecto.
     */
    bool resize(int width, int height);

    /**
     * Importa un búfer de hardware AHardwareBuffer como textura Vulkan (Zero-Copy)
     * mediante la extensión VK_ANDROID_external_memory_android_hardware_buffer.
     */
    bool importHardwareBuffer(AHardwareBuffer* hardwareBuffer);

    /**
     * Ejecuta una pasada de renderizado aplicando los parámetros visuales
     * (brillo, contraste, saturación, gamma, nitidez, luz azul, modo sol).
     */
    bool renderFrame(
        float brightness,
        float contrast,
        float saturation,
        float gamma,
        float sharpness,
        float blueLightFilter,
        float sunMode
    );

    /**
     * Libera todos los recursos Vulkan asignados (Swapchain, Pipeline, Device, Instance).
     */
    void release();

    /**
     * Devuelve true si el motor se encuentra listo para renderizar.
     */
    bool isInitialized() const;

    /**
     * Devuelve el último mensaje de error registrado en caso de fallo de inicialización o renderizado.
     */
    std::string getLastError() const;

    /**
     * Informa si el hardware actual soporta la extensión VK_ANDROID_external_memory_android_hardware_buffer.
     */
    bool isHardwareBufferExtensionSupported() const;

private:
    bool createInstance();
    bool selectPhysicalDevice();
    bool createLogicalDevice();
    bool createSurface(ANativeWindow* window);
    bool createSwapchain(int width, int height);
    bool createRenderPass();
    bool createPipeline();
    bool createFramebuffers();
    bool createCommandPoolAndBuffers();
    bool createSyncObjects();

    void cleanupSwapchain();

    mutable std::mutex mMutex;
    bool mInitialized;
    std::string mLastError;
    bool mSupportsHardwareBufferExt;

    int mWidth;
    int mHeight;

    ANativeWindow* mNativeWindow;

    // Componentes principales de Vulkan
    VkInstance mInstance;
    VkPhysicalDevice mPhysicalDevice;
    VkDevice mDevice;
    VkQueue mGraphicsQueue;
    uint32_t mGraphicsQueueFamilyIndex;

    VkSurfaceKHR mSurface;
    VkSwapchainKHR mSwapchain;
    VkFormat mSwapchainFormat;
    VkExtent2D mSwapchainExtent;

    std::vector<VkImage> mSwapchainImages;
    std::vector<VkImageView> mSwapchainImageViews;
    std::vector<VkFramebuffer> mSwapchainFramebuffers;

    VkRenderPass mRenderPass;
    VkPipelineLayout mPipelineLayout;
    VkPipeline mGraphicsPipeline;

    VkShaderModule mVertexShaderModule;
    VkShaderModule mFragmentShaderModule;

    VkCommandPool mCommandPool;
    std::vector<VkCommandBuffer> mCommandBuffers;

    VkSemaphore mImageAvailableSemaphore;
    VkSemaphore mRenderFinishedSemaphore;
    VkFence mInFlightFence;

    // Punteros a extensiones nativas de AHardwareBuffer
    PFN_vkGetAndroidHardwareBufferPropertiesANDROID fpGetAndroidHardwareBufferPropertiesANDROID;
};

#endif // NOVA_VULKAN_VIDEO_ENGINE_H
