/**
 * VulkanVideoEngine.cpp - Orquestador modular del motor de renderizado Vulkan 1.1+
 *
 * Delega responsabilidades a:
 * - VulkanDeviceContext (dispositivos, instancia y extensiones AHB).
 * - VulkanSwapchainManager (swapchain, render pass y framebuffers).
 * - VulkanPipelineManager (shaders SPIR-V y pipeline gráfico).
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#include "VulkanVideoEngine.h"
#include <android/log.h>
#include <algorithm>

#define LOG_TAG "VulkanVideoEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

VulkanVideoEngine::VulkanVideoEngine()
    : mInitialized(false),
      mLastError("No inicializado"),
      mWidth(0),
      mHeight(0),
      mNativeWindow(nullptr),
      mCommandPool(VK_NULL_HANDLE),
      mImageAvailableSemaphore(VK_NULL_HANDLE),
      mRenderFinishedSemaphore(VK_NULL_HANDLE),
      mInFlightFence(VK_NULL_HANDLE) {
}

VulkanVideoEngine::~VulkanVideoEngine() {
    release();
}

bool VulkanVideoEngine::isInitialized() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mInitialized;
}

std::string VulkanVideoEngine::getLastError() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mLastError;
}

bool VulkanVideoEngine::isHardwareBufferExtensionSupported() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mContext.supportsHardwareBufferExt;
}

bool VulkanVideoEngine::init(ANativeWindow* window, int width, int height) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mInitialized) {
        release();
    }

    if (!window) {
        mLastError = "Error: ANativeWindow es nulo.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    mNativeWindow = window;
    mWidth = std::max(1, width);
    mHeight = std::max(1, height);

    LOGI("Iniciando motor modular Vulkan 1.1+ con dimensiones: %dx%d", mWidth, mHeight);

    // 1. Inicializar contexto de dispositivo
    if (!mContext.init(window, mLastError)) {
        return false;
    }

    // 2. Crear Swapchain
    if (!mSwapchainMgr.createSwapchain(mContext.physicalDevice, mContext.device, mContext.surface, mWidth, mHeight, mLastError)) {
        return false;
    }

    // 3. Crear RenderPass
    if (!mSwapchainMgr.createRenderPass(mContext.device, mLastError)) {
        return false;
    }

    // 4. Crear Graphics Pipeline
    if (!mPipelineMgr.createPipeline(mContext.device, mSwapchainMgr.renderPass, mSwapchainMgr.swapchainExtent, mLastError)) {
        return false;
    }

    // 5. Crear Framebuffers
    if (!mSwapchainMgr.createFramebuffers(mContext.device, mLastError)) {
        return false;
    }

    // 6. Configurar colas de comandos y sincronización
    if (!createCommandPoolAndBuffers()) {
        return false;
    }

    if (!createSyncObjects()) {
        return false;
    }

    mInitialized = true;
    mLastError = "Vulkan 1.1+ inicializado correctamente.";
    LOGI("Motor Vulkan modular inicializado con éxito.");
    return true;
}

bool VulkanVideoEngine::createCommandPoolAndBuffers() {
    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = mContext.graphicsQueueFamilyIndex;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

    VkResult res = vkCreateCommandPool(mContext.device, &poolInfo, nullptr, &mCommandPool);
    if (res != VK_SUCCESS) {
        mLastError = "Error creando VkCommandPool (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    mCommandBuffers.resize(mSwapchainMgr.swapchainFramebuffers.size());
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = mCommandPool;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = static_cast<uint32_t>(mCommandBuffers.size());

    res = vkAllocateCommandBuffers(mContext.device, &allocInfo, mCommandBuffers.data());
    if (res != VK_SUCCESS) {
        mLastError = "Error asignando VkCommandBuffer.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::createSyncObjects() {
    VkSemaphoreCreateInfo semaphoreInfo{};
    semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;

    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;

    if (vkCreateSemaphore(mContext.device, &semaphoreInfo, nullptr, &mImageAvailableSemaphore) != VK_SUCCESS ||
        vkCreateSemaphore(mContext.device, &semaphoreInfo, nullptr, &mRenderFinishedSemaphore) != VK_SUCCESS ||
        vkCreateFence(mContext.device, &fenceInfo, nullptr, &mInFlightFence) != VK_SUCCESS) {
        mLastError = "Error creando objetos de sincronización Vulkan (Semáforos / Fences).";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::resize(int width, int height) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!mInitialized || mContext.device == VK_NULL_HANDLE) {
        return false;
    }

    mWidth = std::max(1, width);
    mHeight = std::max(1, height);

    vkDeviceWaitIdle(mContext.device);

    mSwapchainMgr.cleanupSwapchain(mContext.device);

    if (!mSwapchainMgr.createSwapchain(mContext.physicalDevice, mContext.device, mContext.surface, mWidth, mHeight, mLastError)) {
        return false;
    }

    if (!mSwapchainMgr.createFramebuffers(mContext.device, mLastError)) {
        return false;
    }

    LOGI("Swapchain redimensionado a %dx%d.", mWidth, mHeight);
    return true;
}

bool VulkanVideoEngine::importHardwareBuffer(AHardwareBuffer* hardwareBuffer) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!mInitialized || !hardwareBuffer) {
        return false;
    }

    if (!mContext.supportsHardwareBufferExt || !mContext.fpGetAndroidHardwareBufferPropertiesANDROID) {
        mLastError = "Zero-Copy no disponible: el controlador no expone VK_ANDROID_external_memory_android_hardware_buffer.";
        return false;
    }

    VkAndroidHardwareBufferPropertiesANDROID bufferProps{};
    bufferProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;

    VkResult res = mContext.fpGetAndroidHardwareBufferPropertiesANDROID(mContext.device, hardwareBuffer, &bufferProps);
    if (res != VK_SUCCESS) {
        mLastError = "Fallo en vkGetAndroidHardwareBufferPropertiesANDROID (Código: " + std::to_string(res) + ")";
        LOGW("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::renderFrame(
    float brightness,
    float contrast,
    float saturation,
    float gamma,
    float sharpness,
    float blueLightFilter,
    float sunMode
) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!mInitialized || mContext.device == VK_NULL_HANDLE || mSwapchainMgr.swapchain == VK_NULL_HANDLE) {
        return false;
    }

    // Esperar a que el frame previo concluya
    vkWaitForFences(mContext.device, 1, &mInFlightFence, VK_TRUE, UINT64_MAX);

    uint32_t imageIndex = 0;
    VkResult res = vkAcquireNextImageKHR(
        mContext.device,
        mSwapchainMgr.swapchain,
        UINT64_MAX,
        mImageAvailableSemaphore,
        VK_NULL_HANDLE,
        &imageIndex
    );

    if (res == VK_ERROR_OUT_OF_DATE_KHR) {
        return resize(mWidth, mHeight);
    } else if (res != VK_SUCCESS && res != VK_SUBOPTIMAL_KHR) {
        mLastError = "Fallo adquiriendo imagen de Swapchain (Código: " + std::to_string(res) + ")";
        return false;
    }

    vkResetFences(mContext.device, 1, &mInFlightFence);

    VkCommandBuffer cmd = mCommandBuffers[imageIndex];
    vkResetCommandBuffer(cmd, 0);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    vkBeginCommandBuffer(cmd, &beginInfo);

    VkRenderPassBeginInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    renderPassInfo.renderPass = mSwapchainMgr.renderPass;
    renderPassInfo.framebuffer = mSwapchainMgr.swapchainFramebuffers[imageIndex];
    renderPassInfo.renderArea.offset = {0, 0};
    renderPassInfo.renderArea.extent = mSwapchainMgr.swapchainExtent;

    VkClearValue clearColor = {{{0.0f, 0.0f, 0.0f, 1.0f}}};
    renderPassInfo.clearValueCount = 1;
    renderPassInfo.pClearValues = &clearColor;

    vkCmdBeginRenderPass(cmd, &renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, mPipelineMgr.graphicsPipeline);

    // Dibujar quad en pantalla completa (4 vértices triangle strip)
    vkCmdDraw(cmd, 4, 1, 0, 0);

    vkCmdEndRenderPass(cmd);
    vkEndCommandBuffer(cmd);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    VkSemaphore waitSemaphores[] = {mImageAvailableSemaphore};
    VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
    submitInfo.waitSemaphoreCount = 1;
    submitInfo.pWaitSemaphores = waitSemaphores;
    submitInfo.pWaitDstStageMask = waitStages;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &cmd;

    VkSemaphore signalSemaphores[] = {mRenderFinishedSemaphore};
    submitInfo.signalSemaphoreCount = 1;
    submitInfo.pSignalSemaphores = signalSemaphores;

    if (vkQueueSubmit(mContext.graphicsQueue, 1, &submitInfo, mInFlightFence) != VK_SUCCESS) {
        mLastError = "Error en vkQueueSubmit.";
        return false;
    }

    VkPresentInfoKHR presentInfo{};
    presentInfo.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = signalSemaphores;
    VkSwapchainKHR swapchains[] = {mSwapchainMgr.swapchain};
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = swapchains;
    presentInfo.pImageIndices = &imageIndex;

    res = vkQueuePresentKHR(mContext.graphicsQueue, &presentInfo);
    if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
        resize(mWidth, mHeight);
    } else if (res != VK_SUCCESS) {
        mLastError = "Fallo en vkQueuePresentKHR (Código: " + std::to_string(res) + ")";
        return false;
    }

    return true;
}

void VulkanVideoEngine::release() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mContext.device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(mContext.device);

        if (mImageAvailableSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(mContext.device, mImageAvailableSemaphore, nullptr);
            mImageAvailableSemaphore = VK_NULL_HANDLE;
        }
        if (mRenderFinishedSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(mContext.device, mRenderFinishedSemaphore, nullptr);
            mRenderFinishedSemaphore = VK_NULL_HANDLE;
        }
        if (mInFlightFence != VK_NULL_HANDLE) {
            vkDestroyFence(mContext.device, mInFlightFence, nullptr);
            mInFlightFence = VK_NULL_HANDLE;
        }
        if (mCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(mContext.device, mCommandPool, nullptr);
            mCommandPool = VK_NULL_HANDLE;
        }

        mSwapchainMgr.release(mContext.device);
        mPipelineMgr.release(mContext.device);
    }

    mContext.release();

    mInitialized = false;
    mNativeWindow = nullptr;
    mLastError = "Motor Vulkan liberado correctamente.";
    LOGI("Motor Vulkan modular liberado por completo.");
}
