package com.example.vulkan

import android.hardware.HardwareBuffer
import android.util.Log
import android.view.Surface

/**
 * NativeVulkanVideoEngine.kt - Puente JNI con el motor gráfico Vulkan 1.1+ (C++)
 *
 * Propósito:
 * Permite a la capa de UI de Jetpack Compose interactuar directamente con el pipeline
 * Vulkan nativo compilado en C++ (VulkanVideoEngine.cpp).
 *
 * Características:
 * - Inicialización y redimensionado de Swapchain sobre ANativeWindow (SurfaceView).
 * - Soporte para importación de AHardwareBuffer (Zero-Copy) sin duplicación en RAM.
 * - Ejecución de sombreadores SPIR-V para filtros de color, brillo, contraste y modo sol.
 * - Consulta de diagnósticos de error en tiempo real para activar la degradación elegante (fallback).
 *
 * Licencia: Apache 2.0. Compatible con arquitecturas de 32 y 64 bits (armeabi-v7a, arm64-v8a, x86, x86_64).
 */
object NativeVulkanVideoEngine {

    private const val TAG = "NativeVulkanEngine"

    init {
        try {
            System.loadLibrary("novaplayer_native")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar la librería novaplayer_native para Vulkan: ${e.message}", e)
        }
    }

    /**
     * Inicializa el contexto Vulkan con la superficie SurfaceView provista por Android.
     */
    fun init(surface: Surface, width: Int, height: Int): Boolean {
        return try {
            nativeInit(surface, width, height)
        } catch (e: Throwable) {
            Log.e(TAG, "Error fatal en nativeInit de Vulkan: ${e.message}", e)
            false
        }
    }

    /**
     * Redimensiona el Swapchain cuando la orientación o resolución de pantalla cambia.
     */
    fun resize(width: Int, height: Int): Boolean {
        return try {
            nativeResize(width, height)
        } catch (e: Throwable) {
            Log.w(TAG, "Error en nativeResize: ${e.message}")
            false
        }
    }

    /**
     * Importa un HardwareBuffer en la memoria de Vulkan para procesamiento Zero-Copy.
     */
    fun importHardwareBuffer(hardwareBuffer: HardwareBuffer): Boolean {
        return try {
            nativeImportHardwareBuffer(hardwareBuffer)
        } catch (e: Throwable) {
            Log.w(TAG, "Error importando HardwareBuffer a Vulkan: ${e.message}")
            false
        }
    }

    /**
     * Ejecuta una pasada de renderizado con los parámetros del ecualizador visual.
     */
    fun render(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        gamma: Float,
        sharpness: Float,
        blueLightFilter: Float,
        sunMode: Float
    ): Boolean {
        return try {
            nativeRender(
                brightness,
                contrast,
                saturation,
                gamma,
                sharpness,
                blueLightFilter,
                sunMode
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error en nativeRender de Vulkan: ${e.message}")
            false
        }
    }

    /**
     * Libera de forma segura todos los recursos Vulkan asignados en la GPU.
     */
    fun release() {
        try {
            nativeRelease()
        } catch (e: Throwable) {
            Log.e(TAG, "Error liberando motor Vulkan: ${e.message}")
        }
    }

    /**
     * Obtiene el último mensaje o código de error nativo reportado por el motor.
     */
    fun getLastError(): String {
        return try {
            nativeGetLastError()
        } catch (e: Throwable) {
            "Excepción consultando error: ${e.message}"
        }
    }

    /**
     * Comprueba si el hardware y driver actual soportan AHardwareBuffer en Vulkan.
     */
    fun isHardwareBufferSupported(): Boolean {
        return try {
            nativeIsHardwareBufferSupported()
        } catch (e: Throwable) {
            false
        }
    }

    // Funciones nativas implementadas en native-lib.cpp
    @JvmStatic
    private external fun nativeInit(surface: Surface, width: Int, height: Int): Boolean

    @JvmStatic
    private external fun nativeResize(width: Int, height: Int): Boolean

    @JvmStatic
    private external fun nativeImportHardwareBuffer(hardwareBuffer: HardwareBuffer): Boolean

    @JvmStatic
    private external fun nativeRender(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        gamma: Float,
        sharpness: Float,
        blueLightFilter: Float,
        sunMode: Float
    ): Boolean

    @JvmStatic
    private external fun nativeRelease()

    @JvmStatic
    private external fun nativeGetLastError(): String

    @JvmStatic
    private external fun nativeIsHardwareBufferSupported(): Boolean
}
