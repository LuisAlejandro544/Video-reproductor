/**
 * VulkanDeviceContext.h - Contexto de inicialización de GPU y dispositivos Vulkan
 *
 * Responsabilidades:
 * - Creación de la instancia VkInstance con extensiones de superficie Android.
 * - Creación de VkSurfaceKHR ligada a ANativeWindow.
 * - Selección de GPU física con soporte de gráficos y presentación.
 * - Creación del dispositivo lógico VkDevice y colas de comandos.
 * - Detección de extensiones para Zero-Copy (AHardwareBuffer y YCbCr Sampler).
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#ifndef NOVA_VULKAN_DEVICE_CONTEXT_H
#define NOVA_VULKAN_DEVICE_CONTEXT_H

#include <android/native_window.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <string>

struct VulkanDeviceContext {
    VkInstance instance = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkQueue graphicsQueue = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamilyIndex = 0;
    bool supportsHardwareBufferExt = false;
    PFN_vkGetAndroidHardwareBufferPropertiesANDROID fpGetAndroidHardwareBufferPropertiesANDROID = nullptr;

    bool init(ANativeWindow* window, std::string& outError);
    void release();
};

#endif // NOVA_VULKAN_DEVICE_CONTEXT_H
