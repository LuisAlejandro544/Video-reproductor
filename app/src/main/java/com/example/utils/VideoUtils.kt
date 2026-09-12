package com.example.utils

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import com.example.model.VideoItem
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utilidades para la resolución de metadatos de video y formateo de tiempos y tamaños.
 *
 * Esta clase ayuda a obtener de forma segura el nombre del archivo, tamaño y duración real
 * a partir de la Uri proporcionada tanto por la Galería (Photo Picker) como por el Gestor de Archivos nativo (SAF).
 */
object VideoUtils {

    /**
     * Resuelve los metadatos de un archivo multimedia a partir de su Uri.
     * Utiliza ContentResolver, OpenableColumns y MediaMetadataRetriever para obtener nombre, tamaño y duración.
     */
    fun resolveVideoMetadata(context: Context, uri: Uri): VideoItem {
        var displayName = "Video sin título"
        var fileSize = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Si falla la consulta a través de ContentResolver, se extrae el último segmento de la ruta
            uri.lastPathSegment?.let { segment ->
                displayName = segment.substringAfterLast("/")
            }
        }

        val durationMs = getVideoDurationMs(context, uri)
        val formattedDuration = formatDuration(durationMs)
        val formattedSize = formatFileSize(fileSize)

        return VideoItem(
            uri = uri,
            name = displayName,
            size = fileSize,
            formattedSize = formattedSize,
            durationMs = durationMs,
            formattedDuration = formattedDuration
        )
    }

    /**
     * Extrae la duración precisa en milisegundos de un archivo multimedia utilizando MediaMetadataRetriever.
     */
    fun getVideoDurationMs(context: Context, uri: Uri): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Convierte milisegundos a una cadena de tiempo con formato legible (mm:ss o hh:mm:ss).
     */
    fun formatDuration(timeMs: Long): String {
        if (timeMs <= 0L) return "00:00"

        val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formatea un tamaño en bytes a KB, MB o GB con 1 decimal.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
