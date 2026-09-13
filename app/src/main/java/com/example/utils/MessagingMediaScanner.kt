package com.example.utils

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.VideoRepository
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MessagingMediaScanner.kt - Escáner de videos en carpetas de aplicaciones de mensajería (WhatsApp y Telegram).
 *
 * Propósito:
 * Permite al usuario descubrir automáticamente los videos recibidos en aplicaciones de mensajería
 * reconocidas e incorporarlos a la biblioteca local de Nova Player sin necesidad de importarlos manualmente
 * uno por uno.
 *
 * Funcionalidad:
 * 1. Verifica de forma segura si los permisos de lectura de medios (READ_MEDIA_VIDEO en Android 13+ o
 *    READ_EXTERNAL_STORAGE en Android 8-12) se encuentran concedidos.
 * 2. Consulta el proveedor de contenidos MediaStore de Android con proyección optimizada de metadatos
 *    (URI, nombre de archivo, tamaño, duración y ruta física/relativa).
 * 3. Filtra específicamente videos originados en directorios de WhatsApp ("WhatsApp Video", "com.whatsapp")
 *    y Telegram ("Telegram", "Telegram Video", "org.telegram.messenger").
 * 4. Registra los elementos encontrados en la base de datos local Room de forma idempotente (evitando duplicados).
 */
object MessagingMediaScanner {

    private const val TAG = "MessagingMediaScanner"

    /**
     * Verifica si la aplicación cuenta con los permisos necesarios para inspeccionar los videos del almacenamiento.
     */
    fun hasMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Escanea e indexa videos provenientes de WhatsApp y Telegram.
     * Retorna el número de nuevos videos encontrados e incorporados a la biblioteca.
     */
    suspend fun scanMessagingVideos(context: Context, repository: VideoRepository): Int = withContext(Dispatchers.IO) {
        if (!hasMediaPermission(context)) {
            Log.w(TAG, "Permiso de lectura de medios no concedido. Omitiendo escaneo de mensajería.")
            return@withContext 0
        }

        var importedCount = 0
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                val durationCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val rawName = if (nameCol != -1) c.getString(nameCol) else null
                    val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                    val durationMs = if (durationCol != -1) c.getLong(durationCol) else 0L
                    val dataPath = if (dataCol != -1) c.getString(dataCol) ?: "" else ""

                    // Identificar si la ruta o nombre pertenece a WhatsApp o Telegram
                    val isWhatsApp = dataPath.contains("WhatsApp", ignoreCase = true) ||
                            dataPath.contains("com.whatsapp", ignoreCase = true) ||
                            (rawName != null && rawName.startsWith("VID-WA", ignoreCase = true))

                    val isTelegram = dataPath.contains("Telegram", ignoreCase = true) ||
                            dataPath.contains("org.telegram", ignoreCase = true)

                    if (isWhatsApp || isTelegram) {
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val sourceLabel = if (isWhatsApp) "WhatsApp" else "Telegram"
                        val displayName = rawName ?: "Video $sourceLabel ${File(dataPath).name}"

                        val videoItem = VideoItem(
                            uri = contentUri,
                            name = displayName,
                            size = size,
                            formattedSize = VideoUtils.formatFileSize(size),
                            durationMs = durationMs,
                            formattedDuration = VideoUtils.formatDuration(durationMs)
                        )

                        // Registrar en Room de forma segura
                        val existing = repository.getVideoByUri(contentUri.toString())
                        if (existing == null) {
                            repository.recordImportedVideo(context, videoItem)
                            importedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el escaneo de videos de mensajería: ${e.message}", e)
        }

        Log.i(TAG, "Escaneo de mensajería finalizado. Nuevos videos agregados: $importedCount")
        importedCount
    }
}
