package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.opengl.Anime4kMode
import com.example.opengl.VideoEqualizerState

/**
 * Entidad Room que representa un video importado o reproducido en la aplicación,
 * incorporando persistencia dedicada de configuraciones de audio, video, ecualizador
 * shaders de GPU y subtítulos específicas para cada video individual.
 *
 * Almacena información persistente en SQLite:
 * - uriString: Identificador único de acceso al archivo multimedia.
 * - name: Nombre del archivo de video.
 * - sizeBytes y formattedSize: Tamaño del archivo para visualización rápida.
 * - durationMs y formattedDuration: Duración total del video (ej. "04:32" o "01:20:15").
 * - lastPositionMs: Última posición de reproducción en milisegundos para reanudar.
 * - lastPlayedTimestamp: Marca de tiempo para ordenar cronológicamente la biblioteca.
 * - isCompleted: Indica si el video fue visualizado en su totalidad.
 *
 * Configuraciones por video:
 * - playbackSpeed: Velocidad de reproducción (0.25x a 2.0x).
 * - aspectRatioMode: Modo de relación de aspecto (FIT, ZOOM, STRETCH, ORIGINAL, 16:9, 4:3).
 * - audioEngine: Motor de audio preferido para este video (MEDIA3 u OBOE).
 * - audioChannelMode: Modo de canales de audio (STEREO, MONO, HAAS_3D, INVERTED).
 * - subtitlesEnabled: Si los subtítulos deben estar visibles al abrir este video.
 * - subtitleSize: Tamaño de subtítulos preferido (SMALL, MEDIUM, LARGE).
 * - externalSubtitleUri: URI del archivo de subtítulos externo asociado.
 * - externalSubtitleName: Nombre amigable del archivo de subtítulos externo.
 * - Parámetros del ecualizador y shaders GLSL (brillo, contraste, saturación, gamma, nitidez, filtro azul, FSR, Anime4K, Modo Sol).
 */
@Entity(tableName = "video_history")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val formattedSize: String = "",
    val durationMs: Long = 0L,
    val formattedDuration: String = "00:00",
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,

    // --- Configuraciones de Reproducción Persistentes por Video ---
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: String = "FIT",
    val audioEngine: String = "MEDIA3",
    val audioChannelMode: String = "STEREO",
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: String = "MEDIUM",
    val externalSubtitleUri: String? = null,
    val externalSubtitleName: String? = null,

    // --- Ecualizador de Video y Shaders OpenGL ES Persistentes por Video ---
    val eqBrightness: Float = 0.0f,
    val eqContrast: Float = 1.0f,
    val eqSaturation: Float = 1.0f,
    val eqGamma: Float = 1.0f,
    val eqSharpness: Float = 0.0f,
    val eqBlueLightFilter: Float = 0.0f,
    val eqPillarboxBlur: Boolean = true,
    val eqFsrEnabled: Boolean = false,
    val eqFsrSharpness: Float = 0.75f,
    val eqSunMode: Float = 0.0f,
    val eqAnime4kMode: Int = 0,
    val eqAnime4kStrength: Float = 0.75f,

    // Bandera explícita que indica si este video individual tiene configuraciones personalizadas activas
    val hasCustomConfig: Boolean = false
) {
    /**
     * Determina si el usuario ha guardado configuraciones personalizadas para este video
     * que difieren de los valores por defecto del sistema o si fueron marcadas explícitamente.
     */
    fun hasCustomSettings(): Boolean {
        return hasCustomConfig ||
                playbackSpeed != 1.0f ||
                aspectRatioMode != "FIT" ||
                audioChannelMode != "STEREO" ||
                audioEngine != "MEDIA3" ||
                !subtitlesEnabled ||
                subtitleSize != "MEDIUM" ||
                externalSubtitleUri != null ||
                !toEqualizerState().isDefault
    }

    /**
     * Reconstruye el objeto de estado del ecualizador y shaders a partir de los valores persistidos.
     */
    fun toEqualizerState(): VideoEqualizerState {
        val animeMode = Anime4kMode.values().find { it.id == eqAnime4kMode } ?: Anime4kMode.OFF
        return VideoEqualizerState(
            brightness = eqBrightness,
            contrast = eqContrast,
            saturation = eqSaturation,
            gamma = eqGamma,
            sharpness = eqSharpness,
            blueLightFilter = eqBlueLightFilter,
            pillarboxBlur = eqPillarboxBlur,
            fsrEnabled = eqFsrEnabled,
            fsrSharpness = eqFsrSharpness,
            sunMode = eqSunMode,
            anime4kMode = animeMode,
            anime4kStrength = eqAnime4kStrength
        )
    }
}

