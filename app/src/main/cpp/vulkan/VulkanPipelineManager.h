/**
 * VulkanPipelineManager.h - Gestor de Pipeline Gráfico y Shaders SPIR-V
 *
 * Responsabilidades:
 * - Compilación y enlace de módulos de shaders SPIR-V (Vertex y Fragment).
 * - Creación de DescriptorSetLayout y PipelineLayout.
 * - Creación del Graphics Pipeline optimizado para renderizado de video en quad completo.
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#ifndef NOVA_VULKAN_PIPELINE_MANAGER_H
#define NOVA_VULKAN_PIPELINE_MANAGER_H

#include <vulkan/vulkan.h>
#include <string>

struct VulkanPipelineManager {
    VkDescriptorSetLayout descriptorSetLayout = VK_NULL_HANDLE;
    VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
    VkPipeline graphicsPipeline = VK_NULL_HANDLE;

    bool createPipeline(
        VkDevice device,
        VkRenderPass renderPass,
        VkExtent2D extent,
        std::string& outError
    );

    void release(VkDevice device);

private:
    VkShaderModule createShaderModule(VkDevice device, const uint32_t* code, size_t sizeBytes);
};

#endif // NOVA_VULKAN_PIPELINE_MANAGER_H
