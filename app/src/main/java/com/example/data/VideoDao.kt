package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión reactiva de videos importados y vistos.
 *
 * Utiliza Kotlin Coroutines y Flow para emitir cambios en tiempo real hacia la interfaz Compose.
 */
@Dao
interface VideoDao {

    /**
     * Obtiene el listado completo de videos ordenados por última interacción descendente.
     */
    @Query("SELECT * FROM video_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    /**
     * Busca un video por su ID primario para obtener sus configuraciones actuales exactas.
     */
    @Query("SELECT * FROM video_history WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): VideoEntity?

    /**
     * Busca un video por su URI para verificar si ya fue importado previamente.
     */
    @Query("SELECT * FROM video_history WHERE uriString = :uriString LIMIT 1")
    suspend fun findByUri(uriString: String): VideoEntity?

    /**
     * Inserta un video en la base de datos o lo actualiza si coincide la clave primaria.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(video: VideoEntity): Long

    /**
     * Actualiza una entidad de video completa en la base de datos coincidiendo su PrimaryKey.
     */
    @Update
    suspend fun update(video: VideoEntity)

    /**
     * Actualiza únicamente la marca de tiempo de última reproducción al abrir un video desde el historial.
     */
    @Query("UPDATE video_history SET lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    /**
     * Actualiza el progreso de reproducción y marca de tiempo de un video.
     */
    @Query("""
        UPDATE video_history 
        SET lastPositionMs = :positionMs, 
            durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END,
            formattedDuration = CASE WHEN :formattedDuration != '' THEN :formattedDuration ELSE formattedDuration END,
            lastPlayedTimestamp = :timestamp,
            isCompleted = :isCompleted
        WHERE uriString = :uriString OR id = :id
    """)
    suspend fun updatePlaybackProgress(
        id: Long = 0L,
        uriString: String,
        positionMs: Long,
        durationMs: Long,
        formattedDuration: String,
        timestamp: Long,
        isCompleted: Boolean
    )

    /**
     * Elimina un registro de video individual por su ID.
     */
    @Query("DELETE FROM video_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Modifica el nombre personalizado de un video importado por su ID.
     */
    @Query("UPDATE video_history SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    /**
     * Actualiza el conjunto de configuraciones personalizadas de un video específico:
     * velocidad, relación de aspecto, motor de audio, canal de audio, subtítulos y ecualizador/shaders.
     */
    @Query("""
        UPDATE video_history 
        SET playbackSpeed = :playbackSpeed,
            aspectRatioMode = :aspectRatioMode,
            audioEngine = :audioEngine,
            audioChannelMode = :audioChannelMode,
            subtitlesEnabled = :subtitlesEnabled,
            subtitleSize = :subtitleSize,
            externalSubtitleUri = :externalSubtitleUri,
            externalSubtitleName = :externalSubtitleName,
            eqBrightness = :eqBrightness,
            eqContrast = :eqContrast,
            eqSaturation = :eqSaturation,
            eqGamma = :eqGamma,
            eqSharpness = :eqSharpness,
            eqBlueLightFilter = :eqBlueLightFilter,
            eqPillarboxBlur = :eqPillarboxBlur,
            eqFsrEnabled = :eqFsrEnabled,
            eqFsrSharpness = :eqFsrSharpness,
            eqSunMode = :eqSunMode,
            eqAnime4kMode = :eqAnime4kMode,
            eqAnime4kStrength = :eqAnime4kStrength,
            hasCustomConfig = 1
        WHERE id = :id OR uriString = :uriString
    """)
    suspend fun updateVideoSettings(
        id: Long = 0L,
        uriString: String,
        playbackSpeed: Float,
        aspectRatioMode: String,
        audioEngine: String,
        audioChannelMode: String,
        subtitlesEnabled: Boolean,
        subtitleSize: String,
        externalSubtitleUri: String?,
        externalSubtitleName: String?,
        eqBrightness: Float,
        eqContrast: Float,
        eqSaturation: Float,
        eqGamma: Float,
        eqSharpness: Float,
        eqBlueLightFilter: Float,
        eqPillarboxBlur: Boolean,
        eqFsrEnabled: Boolean,
        eqFsrSharpness: Float,
        eqSunMode: Float,
        eqAnime4kMode: Int,
        eqAnime4kStrength: Float
    )

    /**
     * Limpia todo el historial de videos importados.
     */
    @Query("DELETE FROM video_history")
    suspend fun clearAll()
}
