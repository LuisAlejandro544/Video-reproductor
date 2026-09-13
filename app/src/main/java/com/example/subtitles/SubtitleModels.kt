package com.example.subtitles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.example.rust.AssDialogueItem
import com.example.rust.AssFullResult
import com.example.rust.AssScriptInfo
import com.example.rust.AssStyleItem
import com.example.rust.NovaRustCore
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Representa una pista de subtítulos disponible para la reproducción (interna o externa).
 *
 * @param id Identificador único de la pista o clave representativa.
 * @param label Nombre amigable para mostrar en la interfaz (ej: "Español (SRT)", "Pista 1: Inglés").
 * @param language Código ISO o etiqueta de idioma si está disponible.
 * @param mimeType Tipo MIME asociado (application/x-subrip, text/vtt o text/x-ssa).
 * @param isExternal Indica si fue cargada manualmente por el usuario desde un archivo .srt, .vtt, .ass o .ssa.
 * @param uri URI del archivo en caso de ser subtítulo externo.
 * @param trackGroupIndex Índice del grupo de pistas en ExoPlayer (para pistas internas).
 * @param trackIndex Índice de la pista dentro del grupo (para pistas internas).
 * @param assInfo Metadatos de subtítulos SSA/ASS analizados mediante el motor nativo en Rust.
 * @param assStyles Listado de estilos SSA/ASS analizados para renderizado tipográfico de precisión.
 * @param assDialogues Listado de eventos de diálogo con posicionamiento y overrides de color/estilo.
 */
data class SubtitleTrackItem(
    val id: String,
    val label: String,
    val language: String? = null,
    val mimeType: String = MimeTypes.APPLICATION_SUBRIP,
    val isExternal: Boolean = false,
    val uri: Uri? = null,
    val trackGroupIndex: Int = -1,
    val trackIndex: Int = -1,
    val assInfo: AssScriptInfo? = null,
    val assStyles: List<AssStyleItem> = emptyList(),
    val assDialogues: List<AssDialogueItem> = emptyList()
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
 * Utilidades para detección y manejo de archivos de subtítulos SRT, WebVTT y SSA/ASS.
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
     * Determina el tipo MIME adecuado según la extensión (.srt, .vtt, .ass, .ssa) o el tipo devuelto por el sistema.
     */
    fun detectSubtitleMimeType(fileName: String, resolvedMime: String? = null): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".ass") || lowerName.endsWith(".ssa") || resolvedMime == MimeTypes.TEXT_SSA -> MimeTypes.TEXT_SSA
            lowerName.endsWith(".vtt") || resolvedMime == MimeTypes.TEXT_VTT -> MimeTypes.TEXT_VTT
            lowerName.endsWith(".srt") || resolvedMime == MimeTypes.APPLICATION_SUBRIP -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.APPLICATION_SUBRIP // Fallback seguro a SubRip (.srt)
        }
    }

    /**
     * Si el archivo es SSA/ASS, lee su cabecera e invoca al motor nativo de Rust para extraer metadatos.
     */
    fun inspectAssMetadataIfApplicable(context: Context, uri: Uri, fileName: String): AssScriptInfo? {
        val lower = fileName.lowercase()
        if (!lower.endsWith(".ass") && !lower.endsWith(".ssa")) {
            return null
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                // Leer hasta 300 líneas para extraer Script Info y estilos con ultra-bajo impacto de memoria
                var line: String? = reader.readLine()
                var count = 0
                while (line != null && count < 300) {
                    sb.appendLine(line)
                    line = reader.readLine()
                    count++
                }
                NovaRustCore.parseAssSubtitles(sb.toString())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Carga y parsea completamente el script SSA/ASS utilizando el motor en Rust,
     * obteniendo estilos tipográficos, colores y líneas de diálogo formateadas.
     */
    fun loadFullAssTrackIfApplicable(context: Context, uri: Uri, fileName: String): AssFullResult? {
        val lower = fileName.lowercase()
        if (!lower.endsWith(".ass") && !lower.endsWith(".ssa")) {
            return null
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val content = stream.bufferedReader().readText()
                NovaRustCore.parseAssFull(content)
            }
        } catch (e: Exception) {
            null
        }
    }
}

