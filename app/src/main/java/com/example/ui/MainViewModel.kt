package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VideoEntity
import com.example.data.VideoRepository
import com.example.model.VideoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MainViewModel - Administrador de estado central para la biblioteca de videos y reproducción.
 *
 * Utiliza AndroidViewModel para acceder al contexto de aplicación de forma segura e instanciar
 * la base de datos local Room a través de VideoRepository.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository

    /**
     * Flujo observable con el historial completo de videos importados y vistos.
     */
    val importedVideos: StateFlow<List<VideoEntity>>

    init {
        val database = AppDatabase.getInstance(application)
        repository = VideoRepository(database.videoDao())
        importedVideos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Registra un video importado desde el selector (Galería o Gestor de Archivos)
     * y obtiene su última posición guardada (si ya había sido visto antes).
     */
    fun onVideoSelected(videoItem: VideoItem, onReady: (VideoItem, Long) -> Unit) {
        viewModelScope.launch {
            val entity = repository.recordImportedVideo(getApplication(), videoItem)
            val updatedItem = videoItem.copy(
                durationMs = entity.durationMs,
                formattedDuration = entity.formattedDuration
            )
            onReady(updatedItem, entity.lastPositionMs)
        }
    }

    /**
     * Permite reanudar o reproducir directamente un video existente en la lista de importados.
     */
    fun playFromHistory(entity: VideoEntity, onPlay: (VideoItem, Long) -> Unit) {
        val uri = Uri.parse(entity.uriString)
        val videoItem = VideoItem(
            uri = uri,
            name = entity.name,
            size = entity.sizeBytes,
            formattedSize = entity.formattedSize,
            durationMs = entity.durationMs,
            formattedDuration = entity.formattedDuration
        )
        viewModelScope.launch {
            // Actualizar marca de tiempo como video recientemente abierto
            repository.recordImportedVideo(getApplication(), videoItem)
            onPlay(videoItem, entity.lastPositionMs)
        }
    }

    /**
     * Actualiza la posición y duración durante la reproducción en tiempo real.
     */
    fun updatePlaybackProgress(uriString: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.updatePlaybackProgress(uriString, positionMs, durationMs)
        }
    }

    /**
     * Elimina un archivo del historial e importados.
     */
    fun deleteVideo(id: Long) {
        viewModelScope.launch {
            repository.deleteVideo(id)
        }
    }

    /**
     * Vacía el historial de videos.
     */
    fun clearAllVideos() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
