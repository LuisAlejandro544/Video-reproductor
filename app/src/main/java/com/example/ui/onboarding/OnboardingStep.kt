package com.example.ui.onboarding

/**
 * OnboardingStep.kt - Pasos secuenciales del Asistente de Bienvenida y Configuración Inicial.
 *
 * Flujo:
 * 1. WELCOME_PERMISSIONS: Bienvenida y solicitud interactiva de permisos de lectura de medios (Android 8 a 14+).
 * 2. AUDIO_ENGINE: Selección de motor de audio (Media3 vs Google Oboe C++) con detalle de pros y contras.
 * 3. GRAPHICS_ENGINE: Selección de motor gráfico (OpenGL ES vs Vulkan experimental, con advertencia de desarrollo).
 * 4. THEME_COLOR: Elección de apariencia visual (Modo Oscuro, Claro, Sistema y Material You dinámico).
 * 5. MESSAGING_SCAN: Elección entre escaneo de carpetas de mensajería (WhatsApp/Telegram) o Modo Privado (solo importar).
 * 6. SUMMARY: Resumen final de configuración con botón de inicio.
 */
enum class OnboardingStep(
    val title: String,
    val subtitle: String,
    val stepIndex: Int
) {
    WELCOME_PERMISSIONS(
        title = "Bienvenido a Nova Player",
        subtitle = "Permisos de almacenamiento para tus videos",
        stepIndex = 1
    ),
    AUDIO_ENGINE(
        title = "Motor de Audio",
        subtitle = "Elige la tecnología de reproducción acústica",
        stepIndex = 2
    ),
    GRAPHICS_ENGINE(
        title = "Motor Gráfico",
        subtitle = "Aceleración de video por hardware",
        stepIndex = 3
    ),
    THEME_COLOR(
        title = "Tema y Colores",
        subtitle = "Personaliza la apariencia a tu estilo",
        stepIndex = 4
    ),
    MESSAGING_SCAN(
        title = "Escaneo de Videos",
        subtitle = "Mensajería (WhatsApp y Telegram) vs Modo Privado",
        stepIndex = 5
    ),
    SUMMARY(
        title = "Todo Listo",
        subtitle = "Resumen de tu configuración inicial",
        stepIndex = 6
    )
}
