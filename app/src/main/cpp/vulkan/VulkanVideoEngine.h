/**
 * VulkanVideoEngine.h - Motor nativo de renderizado gráfico Vulkan 1.1+ para Nova Video Player
 *
 * Arquitectura modular desacoplada:
 * - [VulkanDeviceContext]: Gestión de VkInstance, VkSurfaceKHR, VkPhysicalDevice y VkDevice.
 * - [VulkanSwapchainManager]: Creación y redimensionamiento dinámico de VkSwapchainKHR, Views y Framebuffers.
 * - [VulkanPipelineManager]: Módulos SPIR-V Vertex/Fragment y VkPipeline.
 * - [VulkanVideoEngine]: Orquestador de sincronización, colas de comandos y renderizado Zero-Copy.
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

#include "VulkanDeviceContext.h"
#include "VulkanSwapchainManager.h"
#include "VulkanPipelineManager.h"

class VulkanVideoEngine {
public:
    VulkanVideoEngine();
    ~VulkanVideoEngine();

    /**
     * Inicializa el dispositivo, Swapchain y pipeline gráfico en ANativeWindow.
     * Retorna false si el hardware no soporta Vulkan, habilitando el fallback a OpenGL ES.
     */
    bool init(ANativeWindow* window, int width, int height);

    /**
     * Reconfigura el Swapchain ante rotaciones de pantalla o cambios de relación de aspecto.
     */
    bool resize(int width, int height);

    /**
     * Importa tramas de video en memoria compartida (Zero-Copy) mediante AHardwareBuffer.
     */
    bool importHardwareBuffer(AHardwareBuffer* hardwareBuffer);

    /**
     * Ejecuta una pasada de renderizado aplicando los parámetros visuales.
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
     * Libera todos los recursos Vulkan asignados de forma ordenada.
     */
    void release();

    /**
     * Informa si el motor está inicializado y listo para renderizar.
     */
    bool isInitialized() const;

    /**
     * Devuelve el último mensaje de error registrado.
     */
    std::string getLastError() const;

    /**
     * Informa si el hardware soporta la extensión VK_ANDROID_external_memory_android_hardware_buffer.
     */
    bool isHardwareBufferExtensionSupported() const;

private:
    bool createCommandPoolAndBuffers();
    bool createSyncObjects();

    mutable std::mutex mMutex;
    bool mInitialized;
    std::string mLastError;

    int mWidth;
    int mHeight;
    ANativeWindow* mNativeWindow;

    // Submódulos modulares
    VulkanDeviceContext mContext;
    VulkanSwapchainManager mSwapchainMgr;
    VulkanPipelineManager mPipelineMgr;

    // Objetos de comandos y sincronización
    VkCommandPool mCommandPool;
    std::vector<VkCommandBuffer> mCommandBuffers;

    VkSemaphore mImageAvailableSemaphore;
    VkSemaphore mRenderFinishedSemaphore;
    VkFence mInFlightFence;
};

#endif // NOVA_VULKAN_VIDEO_ENGINE_H
