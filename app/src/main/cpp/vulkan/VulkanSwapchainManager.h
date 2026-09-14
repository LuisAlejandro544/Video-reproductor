/**
 * VulkanSwapchainManager.h - Gestor de Swapchain, RenderPass y Framebuffers
 *
 * Responsabilidades:
 * - Creación y recreación dinámica de la cadena de intercambio (VkSwapchainKHR) ante rotaciones.
 * - Creación de vistas de imagen (VkImageView) y pase de renderizado (VkRenderPass).
 * - Creación y destrucción de búferes de cuadro (VkFramebuffer).
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#ifndef NOVA_VULKAN_SWAPCHAIN_MANAGER_H
#define NOVA_VULKAN_SWAPCHAIN_MANAGER_H

#include <vulkan/vulkan.h>
#include <vector>
#include <string>

struct VulkanSwapchainManager {
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkFormat swapchainFormat = VK_FORMAT_B8G8R8A8_UNORM;
    VkExtent2D swapchainExtent{0, 0};

    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    std::vector<VkFramebuffer> swapchainFramebuffers;
    VkRenderPass renderPass = VK_NULL_HANDLE;

    bool createSwapchain(
        VkPhysicalDevice physicalDevice,
        VkDevice device,
        VkSurfaceKHR surface,
        int width,
        int height,
        std::string& outError
    );

    bool createRenderPass(VkDevice device, std::string& outError);
    bool createFramebuffers(VkDevice device, std::string& outError);

    void cleanupSwapchain(VkDevice device);
    void release(VkDevice device);
};

#endif // NOVA_VULKAN_SWAPCHAIN_MANAGER_H
