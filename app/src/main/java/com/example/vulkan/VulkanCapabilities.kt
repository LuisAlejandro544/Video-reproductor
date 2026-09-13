package com.example.vulkan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * VulkanCapabilities.kt - Verificación de capacidades de Vulkan en tiempo de ejecución
 *
 * Propósito:
 * Evalúa de forma exhaustiva y segura si el dispositivo cuenta con soporte de hardware
 * para la API gráfica Vulkan 1.1+ (Fase 6 del Roadmap).
 *
 * Realiza una doble verificación:
 * 1. Nivel del Sistema Operativo Android: Consulta a PackageManager por FEATURE_VULKAN_HARDWARE_VERSION
 *    y FEATURE_VULKAN_HARDWARE_LEVEL.
 * 2. Nivel Nativo C++ (JNI): Carga y consulta directa al driver de la GPU mediante Vulkan Loader
 *    (vkEnumerateInstanceVersion, vkCreateInstance y vkGetPhysicalDeviceProperties).
 *
 * Permite garantizar una degradación elegante (graceful fallback) hacia OpenGL ES si
 * el dispositivo no cuenta con Vulkan 1.1+ o tiene controladores inestables.
 */
data class VulkanStatus(
    val isSupported: Boolean,
    val isVulkan11OrHigher: Boolean,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val apiVersionString: String,
    val hardwareLevel: Int,
    val hardwareLevelString: String,
    val deviceName: String,
    val driverVersionString: String,
    val nativeDiagnostics: String
)

object VulkanCapabilities {

    private const val TAG = "VulkanCapabilities"

    // Versión codificada de Vulkan 1.1: major=1, minor=1, patch=0 -> 0x00401000
    const val VULKAN_VERSION_1_1 = 0x00401000

    init {
        try {
            System.loadLibrary("novaplayer_native")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar la librería nativa novaplayer_native", e)
        }
    }

    /**
     * Consulta el estado completo de capacidades de Vulkan en el dispositivo.
     */
    fun checkCapabilities(context: Context): VulkanStatus {
        val pm = context.packageManager

        // 1. Verificación en PackageManager de Android
        val features = pm.systemAvailableFeatures
        val vulkanFeature = features.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
        val rawVersion = vulkanFeature?.version ?: 0

        val isSupported = rawVersion > 0
        val isVulkan11OrHigher = rawVersion >= VULKAN_VERSION_1_1

        val major = (rawVersion shr 22) and 0x3FF
        val minor = (rawVersion shr 12) and 0x3FF
        val patch = rawVersion and 0xFFF

        val apiVersionString = if (isSupported) {
            "v$major.$minor.$patch"
        } else {
            "No soportado"
        }

        val levelFeature = features.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
        val hardwareLevel = levelFeature?.version ?: 0
        val hardwareLevelString = when {
            !isSupported -> "Incompatible"
            hardwareLevel >= 1 -> "Nivel 1 (Avanzado)"
            else -> "Nivel 0 (Básico)"
        }

        // 2. Consulta nativa al driver C++
        val nativeResult = try {
            nativeQueryVulkanDriver()
        } catch (e: Throwable) {
            Log.w(TAG, "Error consultando driver nativo de Vulkan: ${e.message}")
            "Error JNI: ${e.message}"
        }

        // Parsear respuesta estructurada del driver nativo C++
        // Formato devuelto por C++: "OK|GPU_NAME|DRIVER_VERSION|EXTRA_INFO" o "ERROR|MOTIVO"
        var nativeDeviceName = "Desconocido"
        var nativeDriverVersion = "N/A"
        var nativeDiagnostics = nativeResult

        if (nativeResult.startsWith("OK|")) {
            val parts = nativeResult.split("|")
            if (parts.size >= 3) {
                nativeDeviceName = parts[1]
                nativeDriverVersion = parts[2]
                nativeDiagnostics = if (parts.size >= 4) parts[3] else "Controlador Vulkan verificado con éxito."
            }
        }

        return VulkanStatus(
            isSupported = isSupported,
            isVulkan11OrHigher = isVulkan11OrHigher,
            major = major,
            minor = minor,
            patch = patch,
            apiVersionString = apiVersionString,
            hardwareLevel = hardwareLevel,
            hardwareLevelString = hardwareLevelString,
            deviceName = if (nativeDeviceName != "Desconocido") nativeDeviceName else Build.HARDWARE,
            driverVersionString = nativeDriverVersion,
            nativeDiagnostics = nativeDiagnostics
        )
    }

    /**
     * Puente nativo JNI para consultar directamente al controlador Vulkan en C++.
     */
    @JvmStatic
    external fun nativeQueryVulkanDriver(): String
}
