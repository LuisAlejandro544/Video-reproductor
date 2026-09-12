package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.VideoItem
import com.example.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio que desacopla la fuente de datos local Room de la lógica de interfaz (ViewModel / UI).
 *
 * Responsabilidades:
 * - Registrar o actualizar archivos de video cuando el usuario los importa desde la Galería o Gestor de Archivos.
 * - Actualizar la posición de reproducción y duración exacta una vez que el motor de video comienza a reproducirlo.
 * - Proveer un flujo reactivo (Flow) para observar la lista de videos importados y vistos.
 */
class VideoRepository(private val videoDao: VideoDao) {

    /**
     * Flujo reactivo de todos los videos ordenados por última reproducción/importación.
     */
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()

    /**
     * Registra un video recién importado o actualiza su marca temporal si ya existía.
     */
    suspend fun recordImportedVideo(context: Context, videoItem: VideoItem): VideoEntity = withContext(Dispatchers.IO) {
        val existing = videoDao.findByUri(videoItem.uri.toString())
        val finalDurationMs = if (videoItem.durationMs > 0L) {
            videoItem.durationMs
        } else {
            existing?.durationMs?.takeIf { it > 0L } ?: VideoUtils.getVideoDurationMs(context, videoItem.uri)
        }
        val formattedDuration = VideoUtils.formatDuration(finalDurationMs)

        val entity = VideoEntity(
            id = existing?.id ?: 0L,
            uriString = videoItem.uri.toString(),
            name = videoItem.name,
            sizeBytes = videoItem.size,
            formattedSize = videoItem.formattedSize,
            durationMs = finalDurationMs,
            formattedDuration = formattedDuration,
            lastPositionMs = existing?.lastPositionMs ?: 0L,
            lastPlayedTimestamp = System.currentTimeMillis(),
            isCompleted = existing?.isCompleted ?: false
        )
        val generatedId = videoDao.insertOrUpdate(entity)
        entity.copy(id = if (entity.id == 0L) generatedId else entity.id)
    }

    /**
     * Actualiza el progreso de reproducción de un video mientras se reproduce o al pausar.
     */
    suspend fun updatePlaybackProgress(
        uriString: String,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        val formattedDuration = if (durationMs > 0L) VideoUtils.formatDuration(durationMs) else ""
        val isCompleted = durationMs > 0L && positionMs >= (durationMs - 3000L) // Completado si restan menos de 3 seg
        videoDao.updatePlaybackProgress(
            uriString = uriString,
            positionMs = positionMs,
            durationMs = durationMs,
            formattedDuration = formattedDuration,
            timestamp = System.currentTimeMillis(),
            isCompleted = isCompleted
        )
    }

    /**
     * Elimina un video individual de la biblioteca/historial por su ID.
     */
    suspend fun deleteVideo(id: Long) = withContext(Dispatchers.IO) {
        videoDao.deleteById(id)
    }

    /**
     * Limpia por completo la biblioteca de videos importados.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        videoDao.clearAll()
    }
}
