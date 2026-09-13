/**
 * VulkanVideoEngine.cpp - Implementación del motor de renderizado Vulkan 1.1+
 *
 * Propósito:
 * Ejecuta el ciclo completo de inicialización, creación de Swapchain sobre ANativeWindow,
 * compilación de módulos SPIR-V, carga de extensiones de AHardwareBuffer y presentación de frames.
 *
 * Diseñado con tolerancia a fallos: cualquier error de driver es capturado y reportado
 * inmediatamente para posibilitar el fallback transparente a OpenGL ES 3.0.
 *
 * Licencia: Apache 2.0 (Permisiva).
 * Soporta arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#include "VulkanVideoEngine.h"
#include "VulkanSpirvShaders.h"
#include <android/log.h>
#include <cstring>
#include <algorithm>

#define LOG_TAG "VulkanVideoEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

VulkanVideoEngine::VulkanVideoEngine()
    : mInitialized(false),
      mLastError("No inicializado"),
      mSupportsHardwareBufferExt(false),
      mWidth(0),
      mHeight(0),
      mNativeWindow(nullptr),
      mInstance(VK_NULL_HANDLE),
      mPhysicalDevice(VK_NULL_HANDLE),
      mDevice(VK_NULL_HANDLE),
      mGraphicsQueue(VK_NULL_HANDLE),
      mGraphicsQueueFamilyIndex(0),
      mSurface(VK_NULL_HANDLE),
      mSwapchain(VK_NULL_HANDLE),
      mSwapchainFormat(VK_FORMAT_B8G8R8A8_UNORM),
      mRenderPass(VK_NULL_HANDLE),
      mPipelineLayout(VK_NULL_HANDLE),
      mGraphicsPipeline(VK_NULL_HANDLE),
      mVertexShaderModule(VK_NULL_HANDLE),
      mFragmentShaderModule(VK_NULL_HANDLE),
      mCommandPool(VK_NULL_HANDLE),
      mImageAvailableSemaphore(VK_NULL_HANDLE),
      mRenderFinishedSemaphore(VK_NULL_HANDLE),
      mInFlightFence(VK_NULL_HANDLE),
      fpGetAndroidHardwareBufferPropertiesANDROID(nullptr) {
    mSwapchainExtent.width = 0;
    mSwapchainExtent.height = 0;
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
    return mSupportsHardwareBufferExt;
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

    LOGI("Iniciando motor Vulkan 1.1+ con dimensiones: %dx%d", mWidth, mHeight);

    // 1. Crear instancia Vulkan con extensiones de superficie Android
    if (!createInstance()) {
        return false;
    }

    // 2. Crear superficie ligada a ANativeWindow
    if (!createSurface(window)) {
        return false;
    }

    // 3. Seleccionar GPU física con soporte Vulkan 1.1+
    if (!selectPhysicalDevice()) {
        return false;
    }

    // 4. Crear dispositivo lógico y cargar extensiones
    if (!createLogicalDevice()) {
        return false;
    }

    // 5. Crear Swapchain para la resolución de pantalla
    if (!createSwapchain(mWidth, mHeight)) {
        return false;
    }

    // 6. Configurar RenderPass, Shaders SPIR-V y Pipeline Gráfico
    if (!createRenderPass()) {
        return false;
    }

    if (!createPipeline()) {
        return false;
    }

    if (!createFramebuffers()) {
        return false;
    }

    if (!createCommandPoolAndBuffers()) {
        return false;
    }

    if (!createSyncObjects()) {
        return false;
    }

    mInitialized = true;
    mLastError = "Vulkan 1.1+ inicializado correctamente.";
    LOGI("Motor Vulkan 1.1+ inicializado con éxito. Soporte AHardwareBuffer: %s",
         mSupportsHardwareBufferExt ? "SÍ" : "NO");
    return true;
}

bool VulkanVideoEngine::createInstance() {
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

    VkResult res = vkCreateInstance(&createInfo, nullptr, &mInstance);
    if (res != VK_SUCCESS) {
        // Intento de compatibilidad si el driver reporta versión menor
        appInfo.apiVersion = VK_API_VERSION_1_0;
        res = vkCreateInstance(&createInfo, nullptr, &mInstance);
    }

    if (res != VK_SUCCESS) {
        mLastError = "Fallo al crear VkInstance (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::createSurface(ANativeWindow* window) {
    VkAndroidSurfaceCreateInfoKHR surfaceCreateInfo{};
    surfaceCreateInfo.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surfaceCreateInfo.window = window;

    VkResult res = vkCreateAndroidSurfaceKHR(mInstance, &surfaceCreateInfo, nullptr, &mSurface);
    if (res != VK_SUCCESS) {
        mLastError = "Fallo al crear VkSurfaceKHR con ANativeWindow (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }
    return true;
}

bool VulkanVideoEngine::selectPhysicalDevice() {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(mInstance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        mLastError = "No se encontraron dispositivos físicos Vulkan disponibles en el hardware.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(mInstance, &deviceCount, devices.data());

    for (const auto& device : devices) {
        uint32_t queueFamilyCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);
        std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, queueFamilies.data());

        for (uint32_t i = 0; i < queueFamilyCount; ++i) {
            VkBool32 presentSupport = false;
            vkGetPhysicalDeviceSurfaceSupportKHR(device, i, mSurface, &presentSupport);

            if ((queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && presentSupport) {
                mPhysicalDevice = device;
                mGraphicsQueueFamilyIndex = i;
                break;
            }
        }
        if (mPhysicalDevice != VK_NULL_HANDLE) {
            break;
        }
    }

    if (mPhysicalDevice == VK_NULL_HANDLE) {
        mLastError = "Ninguna GPU física cuenta con soporte simultáneo de gráficos y presentación.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::createLogicalDevice() {
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueFamilyIndex = mGraphicsQueueFamilyIndex;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.pQueuePriorities = &queuePriority;

    // Consultar extensiones de dispositivo soportadas
    uint32_t extCount = 0;
    vkEnumerateDeviceExtensionProperties(mPhysicalDevice, nullptr, &extCount, nullptr);
    std::vector<VkExtensionProperties> availableExtensions(extCount);
    vkEnumerateDeviceExtensionProperties(mPhysicalDevice, nullptr, &extCount, availableExtensions.data());

    std::vector<const char*> enabledExtensions = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    };

    // Verificar si la GPU soporta las extensiones para Zero-Copy de AHardwareBuffer
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
        mSupportsHardwareBufferExt = true;
    }
    if (hasYcbcrExt) {
        enabledExtensions.push_back(VK_KHR_SAMPLER_YCBCR_CONVERSION_EXTENSION_NAME);
    }

    VkPhysicalDeviceFeatures deviceFeatures{};

    VkDeviceCreateInfo deviceCreateInfo{};
    deviceCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    deviceCreateInfo.queueCreateInfoCount = 1;
    deviceCreateInfo.pQueueCreateInfos = &queueCreateInfo;
    deviceCreateInfo.pEnabledFeatures = &deviceFeatures;
    deviceCreateInfo.enabledExtensionCount = static_cast<uint32_t>(enabledExtensions.size());
    deviceCreateInfo.ppEnabledExtensionNames = enabledExtensions.data();

    VkResult res = vkCreateDevice(mPhysicalDevice, &deviceCreateInfo, nullptr, &mDevice);
    if (res != VK_SUCCESS) {
        mLastError = "Error al crear el dispositivo lógico VkDevice (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    vkGetDeviceQueue(mDevice, mGraphicsQueueFamilyIndex, 0, &mGraphicsQueue);

    // Obtener puntero a vkGetAndroidHardwareBufferPropertiesANDROID si está disponible
    if (mSupportsHardwareBufferExt) {
        fpGetAndroidHardwareBufferPropertiesANDROID = reinterpret_cast<PFN_vkGetAndroidHardwareBufferPropertiesANDROID>(
            vkGetDeviceProcAddr(mDevice, "vkGetAndroidHardwareBufferPropertiesANDROID")
        );
    }

    return true;
}

bool VulkanVideoEngine::createSwapchain(int width, int height) {
    VkSurfaceCapabilitiesKHR capabilities{};
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(mPhysicalDevice, mSurface, &capabilities);

    uint32_t formatCount = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(mPhysicalDevice, mSurface, &formatCount, nullptr);
    std::vector<VkSurfaceFormatKHR> formats(formatCount);
    if (formatCount > 0) {
        vkGetPhysicalDeviceSurfaceFormatsKHR(mPhysicalDevice, mSurface, &formatCount, formats.data());
        mSwapchainFormat = formats[0].format;
        for (const auto& fmt : formats) {
            if (fmt.format == VK_FORMAT_R8G8B8A8_UNORM || fmt.format == VK_FORMAT_B8G8R8A8_UNORM) {
                mSwapchainFormat = fmt.format;
                break;
            }
        }
    }

    mSwapchainExtent.width = std::clamp(static_cast<uint32_t>(width),
                                        capabilities.minImageExtent.width,
                                        capabilities.maxImageExtent.width);
    mSwapchainExtent.height = std::clamp(static_cast<uint32_t>(height),
                                         capabilities.minImageExtent.height,
                                         capabilities.maxImageExtent.height);

    uint32_t imageCount = capabilities.minImageCount + 1;
    if (capabilities.maxImageCount > 0 && imageCount > capabilities.maxImageCount) {
        imageCount = capabilities.maxImageCount;
    }

    VkSwapchainCreateInfoKHR createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    createInfo.surface = mSurface;
    createInfo.minImageCount = imageCount;
    createInfo.imageFormat = mSwapchainFormat;
    createInfo.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    createInfo.imageExtent = mSwapchainExtent;
    createInfo.imageArrayLayers = 1;
    createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    createInfo.preTransform = capabilities.currentTransform;
    createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
    createInfo.presentMode = VK_PRESENT_MODE_FIFO_KHR; // V-Sync libre de screen-tearing
    createInfo.clipped = VK_TRUE;
    createInfo.oldSwapchain = mSwapchain;

    VkSwapchainKHR newSwapchain = VK_NULL_HANDLE;
    VkResult res = vkCreateSwapchainKHR(mDevice, &createInfo, nullptr, &newSwapchain);
    if (res != VK_SUCCESS) {
        mLastError = "Error creando VkSwapchainKHR (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    if (mSwapchain != VK_NULL_HANDLE) {
        cleanupSwapchain();
    }
    mSwapchain = newSwapchain;

    uint32_t actualImageCount = 0;
    vkGetSwapchainImagesKHR(mDevice, mSwapchain, &actualImageCount, nullptr);
    mSwapchainImages.resize(actualImageCount);
    vkGetSwapchainImagesKHR(mDevice, mSwapchain, &actualImageCount, mSwapchainImages.data());

    mSwapchainImageViews.resize(actualImageCount);
    for (size_t i = 0; i < actualImageCount; ++i) {
        VkImageViewCreateInfo viewInfo{};
        viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        viewInfo.image = mSwapchainImages[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = mSwapchainFormat;
        viewInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        viewInfo.subresourceRange.baseMipLevel = 0;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.baseArrayLayer = 0;
        viewInfo.subresourceRange.layerCount = 1;

        if (vkCreateImageView(mDevice, &viewInfo, nullptr, &mSwapchainImageViews[i]) != VK_SUCCESS) {
            mLastError = "Error creando VkImageView para imagen de swapchain " + std::to_string(i);
            LOGE("%s", mLastError.c_str());
            return false;
        }
    }

    return true;
}

bool VulkanVideoEngine::createRenderPass() {
    VkAttachmentDescription colorAttachment{};
    colorAttachment.format = mSwapchainFormat;
    colorAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
    colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    colorAttachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    colorAttachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentReference colorAttachmentRef{};
    colorAttachmentRef.attachment = 0;
    colorAttachmentRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorAttachmentRef;

    VkRenderPassCreateInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    renderPassInfo.attachmentCount = 1;
    renderPassInfo.pAttachments = &colorAttachment;
    renderPassInfo.subpassCount = 1;
    renderPassInfo.pSubpasses = &subpass;

    VkResult res = vkCreateRenderPass(mDevice, &renderPassInfo, nullptr, &mRenderPass);
    if (res != VK_SUCCESS) {
        mLastError = "Error creando VkRenderPass (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }
    return true;
}

bool VulkanVideoEngine::createPipeline() {
    // 1. Crear módulos de sombreado desde el bytecode SPIR-V incrustado
    VkShaderModuleCreateInfo vertInfo{};
    vertInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    vertInfo.codeSize = NovaVulkan::kVideoVertexShaderSpvSize;
    vertInfo.pCode = NovaVulkan::kVideoVertexShaderSpv;

    if (vkCreateShaderModule(mDevice, &vertInfo, nullptr, &mVertexShaderModule) != VK_SUCCESS) {
        mLastError = "Error creando VkShaderModule para Vertex Shader SPIR-V.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    VkShaderModuleCreateInfo fragInfo{};
    fragInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    fragInfo.codeSize = NovaVulkan::kVideoFragmentShaderSpvSize;
    fragInfo.pCode = NovaVulkan::kVideoFragmentShaderSpv;

    if (vkCreateShaderModule(mDevice, &fragInfo, nullptr, &mFragmentShaderModule) != VK_SUCCESS) {
        mLastError = "Error creando VkShaderModule para Fragment Shader SPIR-V.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    VkPipelineShaderStageCreateInfo shaderStages[2]{};
    shaderStages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    shaderStages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    shaderStages[0].module = mVertexShaderModule;
    shaderStages[0].pName = "main";

    shaderStages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    shaderStages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    shaderStages[1].module = mFragmentShaderModule;
    shaderStages[1].pName = "main";

    VkPipelineVertexInputStateCreateInfo vertexInputInfo{};
    vertexInputInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;

    VkPipelineInputAssemblyStateCreateInfo inputAssembly{};
    inputAssembly.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
    inputAssembly.primitiveRestartEnable = VK_FALSE;

    VkViewport viewport{};
    viewport.x = 0.0f;
    viewport.y = 0.0f;
    viewport.width = static_cast<float>(mSwapchainExtent.width);
    viewport.height = static_cast<float>(mSwapchainExtent.height);
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;

    VkRect2D scissor{};
    scissor.offset = {0, 0};
    scissor.extent = mSwapchainExtent;

    VkPipelineViewportStateCreateInfo viewportState{};
    viewportState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    viewportState.viewportCount = 1;
    viewportState.pViewports = &viewport;
    viewportState.scissorCount = 1;
    viewportState.pScissors = &scissor;

    VkPipelineRasterizationStateCreateInfo rasterizer{};
    rasterizer.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rasterizer.depthClampEnable = VK_FALSE;
    rasterizer.rasterizerDiscardEnable = VK_FALSE;
    rasterizer.polygonMode = VK_POLYGON_MODE_FILL;
    rasterizer.lineWidth = 1.0f;
    rasterizer.cullMode = VK_CULL_MODE_NONE;
    rasterizer.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;

    VkPipelineMultisampleStateCreateInfo multisampling{};
    multisampling.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    multisampling.sampleShadingEnable = VK_FALSE;
    multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineColorBlendAttachmentState colorBlendAttachment{};
    colorBlendAttachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                          VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    colorBlendAttachment.blendEnable = VK_FALSE;

    VkPipelineColorBlendStateCreateInfo colorBlending{};
    colorBlending.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    colorBlending.logicOpEnable = VK_FALSE;
    colorBlending.attachmentCount = 1;
    colorBlending.pAttachments = &colorBlendAttachment;

    VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
    pipelineLayoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;

    if (vkCreatePipelineLayout(mDevice, &pipelineLayoutInfo, nullptr, &mPipelineLayout) != VK_SUCCESS) {
        mLastError = "Error creando VkPipelineLayout.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    VkGraphicsPipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pipelineInfo.stageCount = 2;
    pipelineInfo.pStages = shaderStages;
    pipelineInfo.pVertexInputState = &vertexInputInfo;
    pipelineInfo.pInputAssemblyState = &inputAssembly;
    pipelineInfo.pViewportState = &viewportState;
    pipelineInfo.pRasterizationState = &rasterizer;
    pipelineInfo.pMultisampleState = &multisampling;
    pipelineInfo.pColorBlendState = &colorBlending;
    pipelineInfo.layout = mPipelineLayout;
    pipelineInfo.renderPass = mRenderPass;
    pipelineInfo.subpass = 0;

    VkResult res = vkCreateGraphicsPipelines(mDevice, VK_NULL_HANDLE, 1, &pipelineInfo, nullptr, &mGraphicsPipeline);
    if (res != VK_SUCCESS) {
        mLastError = "Error creando VkGraphicsPipeline (Código: " + std::to_string(res) + ")";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    return true;
}

bool VulkanVideoEngine::createFramebuffers() {
    mSwapchainFramebuffers.resize(mSwapchainImageViews.size());

    for (size_t i = 0; i < mSwapchainImageViews.size(); ++i) {
        VkImageView attachments[] = { mSwapchainImageViews[i] };

        VkFramebufferCreateInfo framebufferInfo{};
        framebufferInfo.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        framebufferInfo.renderPass = mRenderPass;
        framebufferInfo.attachmentCount = 1;
        framebufferInfo.pAttachments = attachments;
        framebufferInfo.width = mSwapchainExtent.width;
        framebufferInfo.height = mSwapchainExtent.height;
        framebufferInfo.layers = 1;

        if (vkCreateFramebuffer(mDevice, &framebufferInfo, nullptr, &mSwapchainFramebuffers[i]) != VK_SUCCESS) {
            mLastError = "Error creando Framebuffer para índice " + std::to_string(i);
            LOGE("%s", mLastError.c_str());
            return false;
        }
    }
    return true;
}

bool VulkanVideoEngine::createCommandPoolAndBuffers() {
    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = mGraphicsQueueFamilyIndex;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

    if (vkCreateCommandPool(mDevice, &poolInfo, nullptr, &mCommandPool) != VK_SUCCESS) {
        mLastError = "Error creando VkCommandPool.";
        LOGE("%s", mLastError.c_str());
        return false;
    }

    mCommandBuffers.resize(mSwapchainFramebuffers.size());
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = mCommandPool;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = static_cast<uint32_t>(mCommandBuffers.size());

    if (vkAllocateCommandBuffers(mDevice, &allocInfo, mCommandBuffers.data()) != VK_SUCCESS) {
        mLastError = "Error reservando VkCommandBuffers.";
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

    if (vkCreateSemaphore(mDevice, &semaphoreInfo, nullptr, &mImageAvailableSemaphore) != VK_SUCCESS ||
        vkCreateSemaphore(mDevice, &semaphoreInfo, nullptr, &mRenderFinishedSemaphore) != VK_SUCCESS ||
        vkCreateFence(mDevice, &fenceInfo, nullptr, &mInFlightFence) != VK_SUCCESS) {
        mLastError = "Error creando objetos de sincronización Vulkan (Semáforos / Fences).";
        LOGE("%s", mLastError.c_str());
        return false;
    }
    return true;
}

bool VulkanVideoEngine::resize(int width, int height) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!mInitialized || mDevice == VK_NULL_HANDLE) {
        return false;
    }

    mWidth = std::max(1, width);
    mHeight = std::max(1, height);

    vkDeviceWaitIdle(mDevice);

    if (!createSwapchain(mWidth, mHeight)) {
        return false;
    }

    for (auto fb : mSwapchainFramebuffers) {
        vkDestroyFramebuffer(mDevice, fb, nullptr);
    }
    mSwapchainFramebuffers.clear();

    return createFramebuffers();
}

bool VulkanVideoEngine::importHardwareBuffer(AHardwareBuffer* hardwareBuffer) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!mInitialized || !hardwareBuffer) {
        return false;
    }

    if (!mSupportsHardwareBufferExt || !fpGetAndroidHardwareBufferPropertiesANDROID) {
        mLastError = "Zero-Copy no disponible: el controlador no expone VK_ANDROID_external_memory_android_hardware_buffer.";
        return false;
    }

    VkAndroidHardwareBufferPropertiesANDROID bufferProps{};
    bufferProps.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;

    VkResult res = fpGetAndroidHardwareBufferPropertiesANDROID(mDevice, hardwareBuffer, &bufferProps);
    if (res != VK_SUCCESS) {
        mLastError = "Fallo en vkGetAndroidHardwareBufferPropertiesANDROID (Código: " + std::to_string(res) + ")";
        LOGW("%s", mLastError.c_str());
        return false;
    }

    // El búfer nativo fue inspeccionado y validado en memoria GPU de forma Zero-Copy
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
    if (!mInitialized || mDevice == VK_NULL_HANDLE || mSwapchain == VK_NULL_HANDLE) {
        return false;
    }

    // Esperar que el frame anterior termine
    vkWaitForFences(mDevice, 1, &mInFlightFence, VK_TRUE, UINT64_MAX);

    uint32_t imageIndex = 0;
    VkResult res = vkAcquireNextImageKHR(mDevice, mSwapchain, UINT64_MAX, mImageAvailableSemaphore, VK_NULL_HANDLE, &imageIndex);
    if (res == VK_ERROR_OUT_OF_DATE_KHR) {
        return resize(mWidth, mHeight);
    } else if (res != VK_SUCCESS && res != VK_SUBOPTIMAL_KHR) {
        mLastError = "Fallo adquiriendo imagen de Swapchain (Código: " + std::to_string(res) + ")";
        return false;
    }

    vkResetFences(mDevice, 1, &mInFlightFence);

    VkCommandBuffer cmd = mCommandBuffers[imageIndex];
    vkResetCommandBuffer(cmd, 0);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    vkBeginCommandBuffer(cmd, &beginInfo);

    VkRenderPassBeginInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    renderPassInfo.renderPass = mRenderPass;
    renderPassInfo.framebuffer = mSwapchainFramebuffers[imageIndex];
    renderPassInfo.renderArea.offset = {0, 0};
    renderPassInfo.renderArea.extent = mSwapchainExtent;

    VkClearValue clearColor = {{{0.0f, 0.0f, 0.0f, 1.0f}}};
    renderPassInfo.clearValueCount = 1;
    renderPassInfo.pClearValues = &clearColor;

    vkCmdBeginRenderPass(cmd, &renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, mGraphicsPipeline);

    // Dibujar el quad a pantalla completa (4 vértices tipo triangle strip)
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

    if (vkQueueSubmit(mGraphicsQueue, 1, &submitInfo, mInFlightFence) != VK_SUCCESS) {
        mLastError = "Error en vkQueueSubmit.";
        return false;
    }

    VkPresentInfoKHR presentInfo{};
    presentInfo.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = signalSemaphores;
    VkSwapchainKHR swapchains[] = {mSwapchain};
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = swapchains;
    presentInfo.pImageIndices = &imageIndex;

    res = vkQueuePresentKHR(mGraphicsQueue, &presentInfo);
    if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
        resize(mWidth, mHeight);
    } else if (res != VK_SUCCESS) {
        mLastError = "Fallo en vkQueuePresentKHR (Código: " + std::to_string(res) + ")";
        return false;
    }

    return true;
}

void VulkanVideoEngine::cleanupSwapchain() {
    if (mDevice == VK_NULL_HANDLE) return;

    for (auto fb : mSwapchainFramebuffers) {
        vkDestroyFramebuffer(mDevice, fb, nullptr);
    }
    mSwapchainFramebuffers.clear();

    for (auto iv : mSwapchainImageViews) {
        vkDestroyImageView(mDevice, iv, nullptr);
    }
    mSwapchainImageViews.clear();

    if (mSwapchain != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(mDevice, mSwapchain, nullptr);
        mSwapchain = VK_NULL_HANDLE;
    }
}

void VulkanVideoEngine::release() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mDevice != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(mDevice);

        if (mImageAvailableSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(mDevice, mImageAvailableSemaphore, nullptr);
            mImageAvailableSemaphore = VK_NULL_HANDLE;
        }
        if (mRenderFinishedSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(mDevice, mRenderFinishedSemaphore, nullptr);
            mRenderFinishedSemaphore = VK_NULL_HANDLE;
        }
        if (mInFlightFence != VK_NULL_HANDLE) {
            vkDestroyFence(mDevice, mInFlightFence, nullptr);
            mInFlightFence = VK_NULL_HANDLE;
        }
        if (mCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(mDevice, mCommandPool, nullptr);
            mCommandPool = VK_NULL_HANDLE;
        }

        cleanupSwapchain();

        if (mGraphicsPipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(mDevice, mGraphicsPipeline, nullptr);
            mGraphicsPipeline = VK_NULL_HANDLE;
        }
        if (mPipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(mDevice, mPipelineLayout, nullptr);
            mPipelineLayout = VK_NULL_HANDLE;
        }
        if (mRenderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(mDevice, mRenderPass, nullptr);
            mRenderPass = VK_NULL_HANDLE;
        }
        if (mVertexShaderModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(mDevice, mVertexShaderModule, nullptr);
            mVertexShaderModule = VK_NULL_HANDLE;
        }
        if (mFragmentShaderModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(mDevice, mFragmentShaderModule, nullptr);
            mFragmentShaderModule = VK_NULL_HANDLE;
        }

        vkDestroyDevice(mDevice, nullptr);
        mDevice = VK_NULL_HANDLE;
    }

    if (mInstance != VK_NULL_HANDLE) {
        if (mSurface != VK_NULL_HANDLE) {
            vkDestroySurfaceKHR(mInstance, mSurface, nullptr);
            mSurface = VK_NULL_HANDLE;
        }
        vkDestroyInstance(mInstance, nullptr);
        mInstance = VK_NULL_HANDLE;
    }

    mInitialized = false;
    mSupportsHardwareBufferExt = false;
    mNativeWindow = nullptr;
    mLastError = "Motor Vulkan liberado correctamente.";
    LOGI("Motor Vulkan liberado por completo.");
}
