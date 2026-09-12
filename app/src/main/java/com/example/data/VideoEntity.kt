package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un video importado o reproducido en la aplicación.
 *
 * Almacena información persistente en SQLite:
 * - uriString: Identificador único de acceso al archivo multimedia.
 * - name: Nombre del archivo de video.
 * - sizeBytes y formattedSize: Tamaño del archivo para visualización rápida.
 * - durationMs y formattedDuration: Duración total del video (ej. "04:32" o "01:20:15").
 * - lastPositionMs: Última posición de reproducción en milisegundos para reanudar.
 * - lastPlayedTimestamp: Marca de tiempo para ordenar cronológicamente la biblioteca.
 * - isCompleted: Indica si el video fue visualizado en su totalidad.
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
    val isCompleted: Boolean = false
)
