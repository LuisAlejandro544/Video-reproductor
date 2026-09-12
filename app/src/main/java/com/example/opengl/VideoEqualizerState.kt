package com.example.opengl

/**
 * VideoEqualizerState - Estado y Presets del Ecualizador de Video en Tiempo Real
 *
 * Administra los parámetros de postprocesamiento aplicados por GPU a través del fragment shader:
 * - Brillo (Brightness): Desplazamiento aditivo de luz [-0.5f a 0.5f].
 * - Contraste (Contrast): Factor de escala con pivote en gris medio [0.5f a 2.0f].
 * - Saturación (Saturation): Factor de intensidad cromática Rec. 709 [0.0f a 2.0f].
 * - Corrección Gamma (Gamma): Curva exponencial de luminancia perceptual [0.5f a 2.0f].
 * - Nitidez (Sharpness): Coeficiente del kernel Laplaciano para realce de bordes [0.0f a 1.5f].
 */
data class VideoEqualizerState(
    val brightness: Float = 0.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val gamma: Float = 1.0f,
    val sharpness: Float = 0.0f
) {
    val isDefault: Boolean
        get() = brightness == 0.0f &&
                contrast == 1.0f &&
                saturation == 1.0f &&
                gamma == 1.0f &&
                sharpness == 0.0f

    companion object {
        val DEFAULT = VideoEqualizerState()

        val PRESETS = mapOf(
            "Normal" to DEFAULT,
            "Vívido" to VideoEqualizerState(
                brightness = 0.04f,
                contrast = 1.18f,
                saturation = 1.35f,
                gamma = 1.05f,
                sharpness = 0.35f
            ),
            "Cine" to VideoEqualizerState(
                brightness = -0.02f,
                contrast = 1.12f,
                saturation = 1.10f,
                gamma = 0.95f,
                sharpness = 0.20f
            ),
            "Nocturno" to VideoEqualizerState(
                brightness = -0.10f,
                contrast = 0.92f,
                saturation = 0.85f,
                gamma = 0.90f,
                sharpness = 0.0f
            ),
            "Alto Contraste" to VideoEqualizerState(
                brightness = 0.02f,
                contrast = 1.45f,
                saturation = 1.20f,
                gamma = 1.0f,
                sharpness = 0.50f
            ),
            "Blanco y Negro" to VideoEqualizerState(
                brightness = 0.0f,
                contrast = 1.15f,
                saturation = 0.0f,
                gamma = 1.0f,
                sharpness = 0.25f
            )
        )
    }
}
