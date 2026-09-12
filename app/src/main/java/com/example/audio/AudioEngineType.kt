package com.example.audio

/**
 * AudioEngineType - Define el motor de audio seleccionado para la reproducción de video
 *
 * - OBOE: Motor nativo en C++ de Google (AAudio en Android 8.0+ / OpenSL ES en legacy).
 *         Ofrece latencia ultra baja, control directo de búfer y alto rendimiento.
 * - MEDIA3: Motor estándar de Android basado en AudioTrack y pipeline de ExoPlayer.
 */
enum class AudioEngineType(val title: String, val description: String) {
    OBOE(
        title = "Google Oboe (Nativo C++)",
        description = "Baja latencia con AAudio nativo. Rendimiento superior sin microcortes."
    ),
    MEDIA3(
        title = "Media3 (AudioTrack Estándar)",
        description = "Canal de audio por defecto de Android con compatibilidad universal."
    )
}
