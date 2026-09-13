package com.example.ui.settings

/**
 * SettingsSubScreen.kt - Navegación modular por categorías de Ajustes
 *
 * Propósito:
 * Define las pantallas independientes del flujo de configuración tipo Hub-and-Spoke.
 * Cada valor representa una sección técnica enfocada con su propia interfaz y controles,
 * optimizando la experiencia táctil en pantallas móviles y evitando un scroll interminable.
 */
enum class SettingsSubScreen {
    HUB,            // Menú principal con accesos a cada apartado
    AUDIO_ENGINE,   // Selección entre Oboe C++ y Media3
    AUDIO_CHANNELS, // Enrutamiento Estéreo / Mono / Pseudo-Estéreo Haas
    AUDIO_TEST,     // Verificación y prueba senoidal de sonido en tiempo real
    TELEMETRY,      // Métricas nativas de hardware, buffers y CPU
    ABOUT           // Información de arquitectura, 32/64 bits y distribución Uptodown
}
