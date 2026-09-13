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
            isCompleted = existing?.isCompleted ?: false,
            playbackSpeed = existing?.playbackSpeed ?: 1.0f,
            aspectRatioMode = existing?.aspectRatioMode ?: "FIT",
            audioEngine = existing?.audioEngine ?: "MEDIA3",
            audioChannelMode = existing?.audioChannelMode ?: "STEREO",
            subtitlesEnabled = existing?.subtitlesEnabled ?: true,
            subtitleSize = existing?.subtitleSize ?: "MEDIUM",
            externalSubtitleUri = existing?.externalSubtitleUri,
            externalSubtitleName = existing?.externalSubtitleName,
            eqBrightness = existing?.eqBrightness ?: 0.0f,
            eqContrast = existing?.eqContrast ?: 1.0f,
            eqSaturation = existing?.eqSaturation ?: 1.0f,
            eqGamma = existing?.eqGamma ?: 1.0f,
            eqSharpness = existing?.eqSharpness ?: 0.0f,
            eqBlueLightFilter = existing?.eqBlueLightFilter ?: 0.0f,
            eqPillarboxBlur = existing?.eqPillarboxBlur ?: true,
            eqFsrEnabled = existing?.eqFsrEnabled ?: false,
            eqFsrSharpness = existing?.eqFsrSharpness ?: 0.75f,
            eqSunMode = existing?.eqSunMode ?: 0.0f,
            eqAnime4kMode = existing?.eqAnime4kMode ?: 0,
            eqAnime4kStrength = existing?.eqAnime4kStrength ?: 0.75f
        )
        val generatedId = videoDao.insertOrUpdate(entity)
        entity.copy(id = if (entity.id == 0L) generatedId else entity.id)
    }

    /**
     * Actualiza y persiste el conjunto de configuraciones personalizadas de un video específico en Room.
     */
    suspend fun updateVideoSettings(videoEntity: VideoEntity) = withContext(Dispatchers.IO) {
        videoDao.updateVideoSettings(
            uriString = videoEntity.uriString,
            playbackSpeed = videoEntity.playbackSpeed,
            aspectRatioMode = videoEntity.aspectRatioMode,
            audioEngine = videoEntity.audioEngine,
            audioChannelMode = videoEntity.audioChannelMode,
            subtitlesEnabled = videoEntity.subtitlesEnabled,
            subtitleSize = videoEntity.subtitleSize,
            externalSubtitleUri = videoEntity.externalSubtitleUri,
            externalSubtitleName = videoEntity.externalSubtitleName,
            eqBrightness = videoEntity.eqBrightness,
            eqContrast = videoEntity.eqContrast,
            eqSaturation = videoEntity.eqSaturation,
            eqGamma = videoEntity.eqGamma,
            eqSharpness = videoEntity.eqSharpness,
            eqBlueLightFilter = videoEntity.eqBlueLightFilter,
            eqPillarboxBlur = videoEntity.eqPillarboxBlur,
            eqFsrEnabled = videoEntity.eqFsrEnabled,
            eqFsrSharpness = videoEntity.eqFsrSharpness,
            eqSunMode = videoEntity.eqSunMode,
            eqAnime4kMode = videoEntity.eqAnime4kMode,
            eqAnime4kStrength = videoEntity.eqAnime4kStrength
        )
    }

    /**
     * Obtiene la entidad persistida de un video por su URI.
     */
    suspend fun getVideoByUri(uriString: String): VideoEntity? = withContext(Dispatchers.IO) {
        videoDao.findByUri(uriString)
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
     * Modifica el nombre personalizado de un video importado por su ID.
     */
    suspend fun renameVideo(id: Long, newName: String) = withContext(Dispatchers.IO) {
        videoDao.updateName(id, newName)
    }

    /**
     * Limpia por completo la biblioteca de videos importados.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        videoDao.clearAll()
    }
}
