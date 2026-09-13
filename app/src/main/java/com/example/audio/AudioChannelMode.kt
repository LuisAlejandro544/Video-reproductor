package com.example.audio

/**
 * Modo de canal de audio para procesamiento y enrutamiento en tiempo real.
 *
 * Opciones disponibles:
 * - STEREO: Reproducción estéreo nativa (canales izquierdo y derecho independientes).
 * - MONO: Mezcla mono sumada ((L + R) / 2) balanceada en ambos oídos.
 * - SPATIAL_HAAS: Pseudo-estéreo y espacializador tridimensional mediante el Efecto Haas
 *   (micro-retardo acústico interaural de ~16ms con modulación en C++), ideal para videos
 *   grabados con pista mono o sonido plano.
 */
enum class AudioChannelMode(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String
) {
    STEREO(
        id = 0,
        title = "Estéreo Nativo",
        subtitle = "Separación real L / R",
        description = "Mantiene los canales izquierdo y derecho independientes tal como fue grabado el video, ideal para películas y música con mezcla estéreo."
    ),
    MONO(
        id = 1,
        title = "Mono Combinado",
        subtitle = "Suma centrada (L + R) / 2",
        description = "Combina ambos canales y los emite equilibrados por ambos oídos. Resuelve videos con audio grabado en un solo canal o cuando se usa un solo auricular."
    ),
    SPATIAL_HAAS(
        id = 2,
        title = "Pseudo-Estéreo Espacial",
        subtitle = "Efecto Haas acústico 3D",
        description = "Convierte audios grabados en mono o planos en un campo sonoro tridimensional amplio utilizando micro-retardo acústico de ~16 ms calculado en tiempo real por el motor C++."
    );

    companion object {
        fun fromId(id: Int): AudioChannelMode {
            return values().firstOrNull { it.id == id } ?: STEREO
        }
    }
}
