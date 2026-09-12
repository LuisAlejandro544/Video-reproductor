package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
     * Actualiza el progreso de reproducción y marca de tiempo de un video.
     */
    @Query("""
        UPDATE video_history 
        SET lastPositionMs = :positionMs, 
            durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END,
            formattedDuration = CASE WHEN :formattedDuration != '' THEN :formattedDuration ELSE formattedDuration END,
            lastPlayedTimestamp = :timestamp,
            isCompleted = :isCompleted
        WHERE uriString = :uriString
    """)
    suspend fun updatePlaybackProgress(
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
     * Limpia todo el historial de videos importados.
     */
    @Query("DELETE FROM video_history")
    suspend fun clearAll()
}
