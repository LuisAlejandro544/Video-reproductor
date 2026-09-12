package com.example.subtitles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Representa una pista de subtítulos disponible para la reproducción (interna o externa).
 *
 * @param id Identificador único de la pista o clave representativa.
 * @param label Nombre amigable para mostrar en la interfaz (ej: "Español (SRT)", "Pista 1: Inglés").
 * @param language Código ISO o etiqueta de idioma si está disponible.
 * @param mimeType Tipo MIME asociado (application/x-subrip o text/vtt).
 * @param isExternal Indica si fue cargada manualmente por el usuario desde un archivo .srt o .vtt.
 * @param uri URI del archivo en caso de ser subtítulo externo.
 * @param trackGroupIndex Índice del grupo de pistas en ExoPlayer (para pistas internas).
 * @param trackIndex Índice de la pista dentro del grupo (para pistas internas).
 */
data class SubtitleTrackItem(
    val id: String,
    val label: String,
    val language: String? = null,
    val mimeType: String = MimeTypes.APPLICATION_SUBRIP,
    val isExternal: Boolean = false,
    val uri: Uri? = null,
    val trackGroupIndex: Int = -1,
    val trackIndex: Int = -1
)

/**
 * Opciones de escala y tamaño de fuente para los subtítulos en pantalla.
 *
 * Se definen como fracciones de la altura visible del video para asegurar
 * proporcionalidad en teléfonos pequeños, pantallas panorámicas y orientación horizontal.
 */
enum class SubtitleSize(val label: String, val fraction: Float) {
    SMALL("Pequeño", 0.038f),
    MEDIUM("Normal", 0.050f),
    LARGE("Grande", 0.068f)
}

/**
 * Utilidades para detección y manejo de archivos de subtítulos SRT y WebVTT.
 */
@OptIn(UnstableApi::class)
object SubtitleUtils {

    /**
     * Resuelve el nombre del archivo de subtítulo a partir de su URI.
     */
    fun resolveSubtitleFileName(context: Context, uri: Uri): String {
        var fileName = "Subtítulo externo"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst() && !cursor.isNull(nameIndex)) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { segment ->
                fileName = segment.substringAfterLast("/")
            }
        }
        return fileName
    }

    /**
     * Determina el tipo MIME adecuado según la extensión (.srt o .vtt) o el tipo devuelto por el sistema.
     */
    fun detectSubtitleMimeType(fileName: String, resolvedMime: String? = null): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".vtt") || resolvedMime == "text/vtt" -> MimeTypes.TEXT_VTT
            lowerName.endsWith(".srt") || resolvedMime == "application/x-subrip" -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.APPLICATION_SUBRIP // Fallback seguro a SubRip (.srt)
        }
    }
}
