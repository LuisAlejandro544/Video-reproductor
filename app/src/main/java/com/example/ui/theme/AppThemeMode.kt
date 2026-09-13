package com.example.ui.theme

/**
 * AppThemeMode.kt - Modos de tema visual para Nova Video Player
 *
 * Permite seleccionar entre:
 * - SYSTEM: Se sincroniza automáticamente con el tema claro u oscuro del sistema operativo.
 * - LIGHT: Modo claro de alto contraste y fondo luminoso para entornos de día.
 * - DARK: Modo oscuro profundo optimizado para pantallas OLED y reproducción nocturna.
 */
enum class AppThemeMode(val displayName: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}
