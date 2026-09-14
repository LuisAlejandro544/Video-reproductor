/**
 * VulkanDeviceContext.cpp - Implementación de la gestión de dispositivos Vulkan
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#include "VulkanDeviceContext.h"
#include <android/log.h>
#include <vector>
#include <cstring>

#define LOG_TAG "VulkanDeviceContext"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool VulkanDeviceContext::init(ANativeWindow* window, std::string& outError) {
    if (!window) {
        outError = "Error: ANativeWindow es nulo.";
        LOGE("%s", outError.c_str());
        return false;
    }

    // 1. Crear instancia Vulkan
    std::vector<const char*> instanceExtensions = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };

    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Nova Video Player";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "NovaVulkanEngine";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_1;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(instanceExtensions.size());
    createInfo.ppEnabledExtensionNames = instanceExtensions.data();

    VkResult res = vkCreateInstance(&createInfo, nullptr, &instance);
    if (res != VK_SUCCESS) {
        appInfo.apiVersion = VK_API_VERSION_1_0;
        res = vkCreateInstance(&createInfo, nullptr, &instance);
    }

    if (res != VK_SUCCESS) {
        outError = "Fallo al crear VkInstance (Código: " + std::to_string(res) + ")";
        LOGE("%s", outError.c_str());
        return false;
    }

    // 2. Crear superficie con ANativeWindow
    VkAndroidSurfaceCreateInfoKHR surfaceCreateInfo{};
    surfaceCreateInfo.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surfaceCreateInfo.window = window;

    res = vkCreateAndroidSurfaceKHR(instance, &surfaceCreateInfo, nullptr, &surface);
    if (res != VK_SUCCESS) {
        outError = "Fallo al crear VkSurfaceKHR con ANativeWindow (Código: " + std::to_string(res) + ")";
        LOGE("%s", outError.c_str());
        return false;
    }

    // 3. Seleccionar GPU física
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        outError = "No se encontraron dispositivos físicos Vulkan disponibles en el hardware.";
        LOGE("%s", outError.c_str());
        return false;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    for (const auto& dev : devices) {
        uint32_t queueFamilyCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(dev, &queueFamilyCount, nullptr);
        std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, &queueFamilyCount, queueFamilies.data());

        for (uint32_t i = 0; i < queueFamilyCount; ++i) {
            VkBool32 presentSupport = false;
            vkGetPhysicalDeviceSurfaceSupportKHR(dev, i, surface, &presentSupport);

            if ((queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && presentSupport) {
                physicalDevice = dev;
                graphicsQueueFamilyIndex = i;
                break;
            }
        }
        if (physicalDevice != VK_NULL_HANDLE) {
            break;
        }
    }

    if (physicalDevice == VK_NULL_HANDLE) {
        outError = "Ninguna GPU física cuenta con soporte simultáneo de gráficos y presentación.";
        LOGE("%s", outError.c_str());
        return false;
    }

    // 4. Crear dispositivo lógico y consultar extensiones
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueFamilyIndex = graphicsQueueFamilyIndex;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.pQueuePriorities = &queuePriority;

    uint32_t extCount = 0;
    vkEnumerateDeviceExtensionProperties(physicalDevice, nullptr, &extCount, nullptr);
    std::vector<VkExtensionProperties> availableExtensions(extCount);
    vkEnumerateDeviceExtensionProperties(physicalDevice, nullptr, &extCount, availableExtensions.data());

    std::vector<const char*> enabledExtensions = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    };

    bool hasAhbExt = false;
    bool hasYcbcrExt = false;
    for (const auto& ext : availableExtensions) {
        if (strcmp(ext.extensionName, VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_EXTENSION_NAME) == 0) {
            hasAhbExt = true;
        }
        if (strcmp(ext.extensionName, VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME) == 0) {
            hasYcbcrExt = true;
        }
    }

    if (hasAhbExt) {
        enabledExtensions.push_back(VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_EXTENSION_NAME);
        enabledExtensions.push_back(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME);
        enabledExtensions.push_back(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME);
        supportsHardwareBufferExt = true;
    }
    if (hasYcbcrExt) {
        enabledExtensions.push_back(VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME);
    }

    VkPhysicalDeviceFeatures deviceFeatures{};

    VkDeviceCreateInfo devCreateInfo{};
    devCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    devCreateInfo.queueCreateInfoCount = 1;
    devCreateInfo.pQueueCreateInfos = &queueCreateInfo;
    devCreateInfo.pEnabledFeatures = &deviceFeatures;
    devCreateInfo.enabledExtensionCount = static_cast<uint32_t>(enabledExtensions.size());
    devCreateInfo.ppEnabledExtensionNames = enabledExtensions.data();

    res = vkCreateDevice(physicalDevice, &devCreateInfo, nullptr, &device);
    if (res != VK_SUCCESS) {
        outError = "Error al crear el dispositivo lógico VkDevice (Código: " + std::to_string(res) + ")";
        LOGE("%s", outError.c_str());
        return false;
    }

    vkGetDeviceQueue(device, graphicsQueueFamilyIndex, 0, &graphicsQueue);

    if (supportsHardwareBufferExt) {
        fpGetAndroidHardwareBufferPropertiesANDROID = reinterpret_cast<PFN_vkGetAndroidHardwareBufferPropertiesANDROID>(
            vkGetDeviceProcAddr(device, "vkGetAndroidHardwareBufferPropertiesANDROID")
        );
    }

    LOGI("Contexto de dispositivo Vulkan creado con éxito. Soporte AHB Zero-Copy: %s",
         supportsHardwareBufferExt ? "SÍ" : "NO");
    return true;
}

void VulkanDeviceContext::release() {
    if (device != VK_NULL_HANDLE) {
        vkDestroyDevice(device, nullptr);
        device = VK_NULL_HANDLE;
    }

    if (instance != VK_NULL_HANDLE) {
        if (surface != VK_NULL_HANDLE) {
            vkDestroySurfaceKHR(instance, surface, nullptr);
            surface = VK_NULL_HANDLE;
        }
        vkDestroyInstance(instance, nullptr);
        instance = VK_NULL_HANDLE;
    }

    physicalDevice = VK_NULL_HANDLE;
    graphicsQueue = VK_NULL_HANDLE;
    supportsHardwareBufferExt = false;
    fpGetAndroidHardwareBufferPropertiesANDROID = nullptr;
}
