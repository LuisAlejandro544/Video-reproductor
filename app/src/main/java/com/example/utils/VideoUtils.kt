package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.collection.LruCache
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utilidades para la resolución de metadatos de video, persistencia de URIs y formateo.
 *
 * Esta clase garantiza que los videos importados desde la Galería (Photo Picker) o SAF
 * mantengan acceso de lectura permanente a lo largo de reinicios de la app,
 * resolviendo de forma segura nombre, tamaño y duración.
 */
object VideoUtils {

    private const val TAG = "VideoUtils"
    private const val IMPORTED_VIDEOS_DIR = "imported_videos"

    /**
     * Asegura que una URI de video sea persistente a lo largo de reinicios del sistema y de la app.
     *
     * 1. Si la URI ya tiene esquema "file", ya es permanente en disco local.
     * 2. Si proviene de SAF (OpenDocument), intenta tomar permisos persistentes con ContentResolver.
     * 3. Si proviene del Photo Picker (URIs temporales como content://media/picker/...) o si
     *    takePersistableUriPermission falla, crea una copia protegida en context.filesDir/imported_videos/
     *    y retorna una URI "file://" permanente que jamás expirará ni lanzará SecurityException.
     */
    fun persistUriOrCopy(context: Context, uri: Uri): Uri {
        // Si ya es un archivo local del sistema de archivos, es 100% persistente
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri
        }

        val uriString = uri.toString()
        val isPhotoPicker = uriString.contains("photopicker", ignoreCase = true) ||
                uriString.contains("com.android.providers.media.photopicker", ignoreCase = true)

        // Si no es de Photo Picker, intentar tomar permisos persistentes estándar de SAF
        var hasPersistablePermission = false
        if (!isPhotoPicker) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                hasPersistablePermission = true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo otorgar takePersistableUriPermission para $uri: ${e.message}")
            }
        }

        // Si se obtuvieron permisos persistentes reales en SAF, no necesitamos duplicar almacenamiento
        if (hasPersistablePermission && !isPhotoPicker) {
            return uri
        }

        // Si es una URI efímera (Photo Picker) o no se pudieron persistir los permisos,
        // copiamos el archivo al almacenamiento interno de la app para garantizar disponibilidad eterna
        return try {
            val storageDir = File(context.filesDir, IMPORTED_VIDEOS_DIR).apply {
                if (!exists()) mkdirs()
            }

            val originalName = queryDisplayName(context, uri)
            // Sanitizar nombre de archivo para evitar caracteres inválidos en Linux/Android
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = File(storageDir, "${System.currentTimeMillis()}_$safeName")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                Log.i(TAG, "Video copiado exitosamente a almacenamiento interno: ${destFile.absolutePath}")
                Uri.fromFile(destFile)
            } else {
                Log.w(TAG, "El archivo destino quedó vacío tras la copia, usando URI original.")
                uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando video a almacenamiento interno: ${e.message}", e)
            uri
        }
    }

    /**
     * Verifica si una URI sigue siendo accesible para lectura por parte del ContentResolver o File.
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path ?: return false
                val file = File(path)
                file.exists() && file.canRead()
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    true
                } ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Elimina el archivo local si pertenece a la carpeta interna de videos importados.
     */
    fun deleteImportedFileIfLocal(context: Context, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path
                if (path != null && path.contains(IMPORTED_VIDEOS_DIR)) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando archivo local importado: ${e.message}")
        }
    }

    /**
     * Elimina todos los videos guardados en la carpeta privada de importaciones.
     */
    fun clearImportedFiles(context: Context) {
        try {
            val storageDir = File(context.filesDir, IMPORTED_VIDEOS_DIR)
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error vaciando carpeta de videos importados: ${e.message}")
        }
    }

    /**
     * Obtiene el nombre amigable del archivo a partir de ContentResolver o su ruta.
     */
    private fun queryDisplayName(context: Context, uri: Uri): String {
        var displayName = "video.mp4"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { segment ->
                displayName = segment.substringAfterLast("/")
            }
        }
        return displayName
    }

    /**
     * Resuelve los metadatos de un archivo multimedia a partir de su Uri.
     * Utiliza ContentResolver, OpenableColumns y MediaMetadataRetriever para obtener nombre, tamaño y duración.
     */
    fun resolveVideoMetadata(context: Context, uri: Uri): VideoItem {
        var displayName = queryDisplayName(context, uri)
        var fileSize = 0L

        try {
            if (uri.scheme.equals("file", ignoreCase = true)) {
                uri.path?.let { p ->
                    val f = File(p)
                    if (f.exists()) {
                        fileSize = f.length()
                        displayName = f.name
                    }
                }
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

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

    /**
     * Caché LRU en memoria para miniaturas de video generadas, evitando recalcular fotogramas en scroll.
     */
    private val thumbnailCache = LruCache<String, Bitmap>(35)

    /**
     * Carga de forma asíncrona un fotograma (Bitmap) exactamente en la posición donde se dejó el video (positionMs).
     *
     * - Si positionMs > 0, extrae el frame exacto de pausa.
     * - Si positionMs == 0, extrae el primer frame representativo.
     * - Se ejecuta en Dispatchers.IO para no bloquear la interfaz y escala la imagen eficientemente.
     */
    suspend fun loadThumbnailAtPosition(
        context: Context,
        uri: Uri,
        positionMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${uri}_$positionMs"
        thumbnailCache.get(cacheKey)?.let { return@withContext it }

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path ?: return@withContext null
                retriever.setDataSource(path)
            } else {
                retriever.setDataSource(context, uri)
            }

            // MediaMetadataRetriever espera el tiempo en microsegundos (us = ms * 1000)
            val targetTimeUs = (positionMs.coerceAtLeast(0L)) * 1000L

            val frame: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // En API 27+ getScaledFrameAtTime genera el bitmap directamente en el tamaño requerido con mínimo consumo
                retriever.getScaledFrameAtTime(
                    targetTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    280,
                    160
                ) ?: retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                // Fallback para API 26 (Android 8.0)
                retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }

            if (frame != null) {
                thumbnailCache.put(cacheKey, frame)
            }
            frame
        } catch (e: Exception) {
            Log.w(TAG, "Error extrayendo miniatura en $positionMs ms para $uri: ${e.message}")
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
    }
}

