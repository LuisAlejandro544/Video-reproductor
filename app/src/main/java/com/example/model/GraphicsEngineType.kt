package com.example.model

/**
 * GraphicsEngineType.kt - Tipos de motor gráfico para renderizado de video en Nova Player.
 *
 * Opciones disponibles:
 * - OPENGL_ES: Motor de renderizado estándar basado en OpenGL ES 3.0+. Ofrece máxima compatibilidad,
 *   madurez total y estabilidad con todos los shaders de mejora de imagen (Sun Mode, FSR, Anime4K, Pillarbox Blur).
 * - VULKAN: Motor experimental de bajo nivel (Vulkan 1.1+). Diseñado para reducir la sobrecarga del procesador
 *   y comunicarse directamente con la GPU. Se encuentra en desarrollo activo de funciones.
 */
enum class GraphicsEngineType(
    val displayName: String,
    val description: String,
    val isExperimental: Boolean
) {
    OPENGL_ES(
        displayName = "OpenGL ES 3.0+",
        description = "Estable, probado y 100% compatible con todos los efectos de imagen.",
        isExperimental = false
    ),
    VULKAN(
        displayName = "Vulkan 1.1+ (Bajo nivel)",
        description = "Menor consumo de CPU y acceso directo a la GPU. En desarrollo de funciones.",
        isExperimental = true
    )
}
