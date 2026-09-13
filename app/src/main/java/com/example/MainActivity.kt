package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioEngineType
import com.example.model.VideoItem
import com.example.ui.MainViewModel
import com.example.ui.SettingsScreen
import com.example.ui.VideoImportScreen
import com.example.ui.VideoPlayerScreen
import com.example.ui.VideoSourceDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.VideoUtils

/**
 * Pantallas principales de navegación de la aplicación
 */
enum class AppScreen {
    HOME,
    PLAYER,
    SETTINGS
}

/**
 * Actividad Principal (MainActivity)
 *
 * Administra la navegación entre:
 * 1. Pantalla de Bienvenida, Biblioteca e Importación (HOME) con persistencia local Room.
 * 2. Pantalla de Reproducción multimedia estilo PC (PLAYER) con motor Google Oboe C++.
 * 3. Pantalla completa e independiente de Configuración y Telemetría de Audio (SETTINGS).
 *
 * Se utiliza el sistema nativo de contratos de Activity Result:
 * - PickVisualMedia: Para la Galería de Fotos/Videos del sistema.
 * - OpenDocument: Para el Gestor de Archivos nativo de Android (SAF), permitiendo explorar todo el almacenamiento.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Permitir que el reproductor dibuje de borde a borde incluso en dispositivos con notch/corte de pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            // Forzamos el tema oscuro característico de reproductores de PC
            MyApplicationTheme(darkTheme = true) {
                MainVideoApp()
            }
        }
    }
}

/**
 * Composable contenedor principal que gestiona el estado de reproducción y navegación.
 */
@Composable
fun MainVideoApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Observar la lista de videos importados y vistos desde la base de datos Room
    val importedVideos by viewModel.importedVideos.collectAsStateWithLifecycle()

    // Observar el motor de audio persistido en disco (Media3 por defecto inicial)
    val selectedAudioEngine by viewModel.selectedAudioEngine.collectAsStateWithLifecycle()

    // Destino actual y previo de navegación
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

    // Bloquear en orientación vertical siempre que estemos en HOME o SETTINGS
    androidx.compose.runtime.LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.HOME || currentScreen == AppScreen.SETTINGS) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Estado del video seleccionado y posición actual en ms (para conservar al entrar a configuración)
    var currentVideo by remember { mutableStateOf<VideoItem?>(null) }
    var currentVideoEntity by remember { mutableStateOf<com.example.data.VideoEntity?>(null) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }

    // Control de visibilidad del diálogo de selección de fuente
    var showSourceDialog by remember { mutableStateOf(false) }

    // Lanzador 1: Galería de videos (Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importVideo(context, uri) { updatedItem, lastPos, entity ->
                currentVideo = updatedItem
                currentPlaybackPositionMs = lastPos
                currentVideoEntity = entity
                currentScreen = AppScreen.PLAYER
            }
        }
    }

    // Lanzador 2: Gestor de Archivos nativo de Android (Storage Access Framework)
    val fileManagerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importVideo(context, uri) { updatedItem, lastPos, entity ->
                currentVideo = updatedItem
                currentPlaybackPositionMs = lastPos
                currentVideoEntity = entity
                currentScreen = AppScreen.PLAYER
            }
        }
    }

    // Funciones para activar los selectores
    val openGallery = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    val openFileManager = {
        fileManagerLauncher.launch(arrayOf("video/*"))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.SETTINGS -> {
                // Pantalla independiente y dedicada de Configuración y Telemetría
                SettingsScreen(
                    currentEngine = selectedAudioEngine,
                    onEngineChanged = { newEngine ->
                        viewModel.setAudioEngine(newEngine)
                    },
                    onNavigateBack = {
                        currentScreen = previousScreen
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
            AppScreen.PLAYER -> {
                val activeVideo = currentVideo
                if (activeVideo != null) {
                    // Pantalla de Reproducción activa (Edge-to-Edge nativa completa)
                    VideoPlayerScreen(
                        videoItem = activeVideo,
                        initialVideoEntity = currentVideoEntity,
                        currentAudioEngine = selectedAudioEngine,
                        initialPositionMs = currentPlaybackPositionMs,
                        onPositionChanged = { newPos ->
                            currentPlaybackPositionMs = newPos
                        },
                        onPlaybackProgress = { posMs, durMs ->
                            viewModel.updatePlaybackProgress(activeVideo.uri.toString(), posMs, durMs)
                        },
                        onSaveVideoSettings = { updatedEntity ->
                            currentVideoEntity = updatedEntity
                            viewModel.saveVideoSettings(updatedEntity)
                        },
                        onBackToHome = {
                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            currentVideo = null
                            currentVideoEntity = null
                            currentPlaybackPositionMs = 0L
                            currentScreen = AppScreen.HOME
                        },
                        onChangeVideoSource = {
                            showSourceDialog = true
                        },
                        onOpenSettings = {
                            previousScreen = AppScreen.PLAYER
                            currentScreen = AppScreen.SETTINGS
                        },
                        onAudioEngineChange = { newEngine ->
                            viewModel.setAudioEngine(newEngine)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    currentScreen = AppScreen.HOME
                }
            }
            AppScreen.HOME -> {
                // Pantalla de Inicio / Importación de Video enriquecida con biblioteca Room
                VideoImportScreen(
                    importedVideos = importedVideos,
                    selectedAudioEngine = selectedAudioEngine,
                    onOpenAudioSettings = {
                        previousScreen = AppScreen.HOME
                        currentScreen = AppScreen.SETTINGS
                    },
                    onOpenGallery = openGallery,
                    onOpenFileManager = openFileManager,
                    onPlayVideo = { entity ->
                        viewModel.playFromHistory(
                            entity = entity,
                            onError = { errorMsg ->
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                                showSourceDialog = true
                            },
                            onPlay = { item, lastPos, updatedEntity ->
                                currentVideo = item
                                currentPlaybackPositionMs = lastPos
                                currentVideoEntity = updatedEntity
                                currentScreen = AppScreen.PLAYER
                            }
                        )
                    },
                    onDeleteVideo = { videoId ->
                        val targetEntity = importedVideos.find { it.id == videoId }
                        viewModel.deleteVideo(videoId, targetEntity?.uriString)
                    },
                    onRenameVideo = { videoId, newName ->
                        viewModel.renameVideo(videoId, newName)
                    },
                    onClearHistory = {
                        viewModel.clearAllVideos()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }

        // Diálogo para cambiar de video o seleccionar fuente
        if (showSourceDialog) {
            VideoSourceDialog(
                onDismissRequest = { showSourceDialog = false },
                onSelectGallery = {
                    openGallery()
                },
                onSelectFileManager = {
                    openFileManager()
                }
            )
        }
    }
}


/**
 * Función Greeting conservada para asegurar compatibilidad con pruebas unitarias existentes.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}

