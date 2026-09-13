package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngineType
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.VideoEntity
import com.example.data.VideoRepository
import com.example.model.VideoItem
import com.example.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainViewModel - Administrador de estado central para la biblioteca de videos y reproducción.
 *
 * Utiliza AndroidViewModel para acceder al contexto de aplicación de forma segura e instanciar
 * la base de datos local Room a través de VideoRepository, así como persistir las preferencias
 * de usuario (como el motor de audio Media3 vs Oboe) en AppPreferences.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    private val appPreferences = AppPreferences.getInstance(application)

    /**
     * Flujo observable con el historial completo de videos importados y vistos.
     */
    val importedVideos: StateFlow<List<VideoEntity>>

    /**
     * Motor de audio activo con persistencia duradera en disco (Media3 por defecto).
     */
    private val _selectedAudioEngine = MutableStateFlow(appPreferences.selectedAudioEngine)
    val selectedAudioEngine: StateFlow<AudioEngineType> = _selectedAudioEngine.asStateFlow()

    /**
     * Actualiza y persiste la selección de motor de audio del usuario.
     */
    fun setAudioEngine(engine: AudioEngineType) {
        appPreferences.selectedAudioEngine = engine
        _selectedAudioEngine.value = engine
    }

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
     * Importa un nuevo video de forma segura asegurando persistencia de lectura eterna:
     * Si la URI proviene de Photo Picker o no soporta permisos persistentes, se almacena
     * una copia protegida en almacenamiento local privado antes de registrar en Room.
     */
    fun importVideo(
        context: Context,
        rawUri: Uri,
        onReady: (VideoItem, Long, VideoEntity) -> Unit
    ) {
        viewModelScope.launch {
            val persistentUri = withContext(Dispatchers.IO) {
                VideoUtils.persistUriOrCopy(context, rawUri)
            }
            val videoItem = VideoUtils.resolveVideoMetadata(context, persistentUri)
            val entity = repository.recordImportedVideo(getApplication(), videoItem)
            val updatedItem = videoItem.copy(
                durationMs = entity.durationMs,
                formattedDuration = entity.formattedDuration
            )
            onReady(updatedItem, entity.lastPositionMs, entity)
        }
    }

    /**
     * Registra un video ya resuelto y obtiene su última posición guardada.
     */
    fun onVideoSelected(videoItem: VideoItem, onReady: (VideoItem, Long, VideoEntity) -> Unit) {
        viewModelScope.launch {
            val entity = repository.recordImportedVideo(getApplication(), videoItem)
            val updatedItem = videoItem.copy(
                durationMs = entity.durationMs,
                formattedDuration = entity.formattedDuration
            )
            onReady(updatedItem, entity.lastPositionMs, entity)
        }
    }

    /**
     * Permite reanudar o reproducir directamente un video existente en la lista de importados.
     * Si el video era una URI temporal antigua cuyo permiso expiró antes de la actualización,
     * notifica a través de onError para que el usuario pueda volver a seleccionarlo.
     */
    fun playFromHistory(
        entity: VideoEntity,
        onError: ((String) -> Unit)? = null,
        onPlay: (VideoItem, Long, VideoEntity) -> Unit
    ) {
        val uri = Uri.parse(entity.uriString)
        val isAccessible = VideoUtils.isUriAccessible(getApplication(), uri)

        if (!isAccessible) {
            onError?.invoke("El permiso temporal de este archivo expiró en el sistema. Por favor reimpórtalo desde la Galería.")
            return
        }

        val videoItem = VideoItem(
            uri = uri,
            name = entity.name,
            size = entity.sizeBytes,
            formattedSize = entity.formattedSize,
            durationMs = entity.durationMs,
            formattedDuration = entity.formattedDuration
        )
        viewModelScope.launch {
            // Actualizar marca de tiempo como video recientemente abierto preservando ajustes
            val updatedEntity = repository.recordImportedVideo(getApplication(), videoItem)
            onPlay(videoItem, updatedEntity.lastPositionMs, updatedEntity)
        }
    }

    /**
     * Guarda y sincroniza en Room todas las configuraciones personalizadas para un video individual.
     */
    fun saveVideoSettings(videoEntity: VideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVideoSettings(videoEntity)
        }
    }

    /**
     * Actualiza la posición y duración durante la reproducción en segundo plano.
     * Se ejecuta estrictamente en Dispatchers.IO para no interferir con la fluidez del hilo principal (UI).
     */
    fun updatePlaybackProgress(uriString: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlaybackProgress(uriString, positionMs, durationMs)
        }
    }

    /**
     * Elimina un archivo del historial e importados, eliminando también la copia física local si existía.
     */
    fun deleteVideo(id: Long, uriString: String? = null) {
        viewModelScope.launch {
            uriString?.let { uri ->
                withContext(Dispatchers.IO) {
                    VideoUtils.deleteImportedFileIfLocal(getApplication(), uri)
                }
            }
            repository.deleteVideo(id)
        }
    }

    /**
     * Modifica el nombre personalizado de un video importado.
     * Utiliza el motor nativo de Rust (NovaRustCore.sanitizeTitle) para sanitizar
     * y validar el nuevo nombre de forma segura antes de persistirlo en Room.
     */
    fun renameVideo(id: Long, rawNewName: String) {
        viewModelScope.launch {
            val sanitizedName = com.example.rust.NovaRustCore.sanitizeTitle(rawNewName)
            repository.renameVideo(id, sanitizedName)
        }
    }

    /**
     * Vacía el historial de videos y limpia la carpeta interna de archivos importados.
     */
    fun clearAllVideos() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                VideoUtils.clearImportedFiles(getApplication())
            }
            repository.clearHistory()
        }
    }
}

