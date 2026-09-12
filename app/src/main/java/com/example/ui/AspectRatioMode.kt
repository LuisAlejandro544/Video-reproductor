package com.example.ui

/**
 * AspectRatioMode - Modos de visualización y escalado de relación de aspecto de video
 *
 * - FIT: Mantiene la relación de aspecto original ajustándola a los límites de pantalla (letterbox).
 * - ZOOM: Escala recortando los bordes para llenar toda la pantalla sin distorsión geométrica.
 * - FILL: Estira el frame para abarcar la totalidad de la pantalla.
 */
enum class AspectRatioMode(val label: String) {
    FIT("Ajustar"),
    ZOOM("Zoom"),
    FILL("Estirar")
}
