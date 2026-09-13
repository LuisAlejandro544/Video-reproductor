package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioEngineType
import com.example.data.VideoEntity
import com.example.ui.library.ArchitectureInfoCard
import com.example.ui.library.AudioEngineBanner
import com.example.ui.library.BrandHeroSection
import com.example.ui.library.ClearHistoryConfirmDialog
import com.example.ui.library.DeleteVideoConfirmDialog
import com.example.ui.library.EmptyImportedVideosCard
import com.example.ui.library.ImportQuickCard
import com.example.ui.library.RenameVideoDialog
import com.example.ui.library.SupportedFormatsSection
import com.example.ui.library.TopHeaderSection
import com.example.ui.library.VideoHistoryCard

/**
 * VideoImportScreen.kt - Pantalla Principal y Biblioteca de Videos
 *
 * Arquitectura modular que integra:
 * - ImportQuickCard: Selección rápida de videos desde galería o gestor de archivos.
 * - VideoHistoryCard / EmptyImportedVideosCard: Gestión visual de videos importados y su progreso con miniatura real.
 * - LibraryDialogs: Diálogos modales para borrado individual, vaciado de historial o edición de nombre.
 * - LibraryHeaders y LibraryFooterCards: Identidad visual, estado del motor y soporte de formatos.
 */
@Composable
fun VideoImportScreen(
    importedVideos: List<VideoEntity>,
    selectedAudioEngine: AudioEngineType,
    onOpenAudioSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFileManager: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onRenameVideo: (Long, String) -> Unit = { _, _ -> },
    onDeleteVideo: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var videoPendingDelete by remember { mutableStateOf<VideoEntity?>(null) }
    var videoPendingRename by remember { mutableStateOf<VideoEntity?>(null) }

    // Diálogo modal para renombrar video usando motor Rust
    videoPendingRename?.let { video ->
        RenameVideoDialog(
            video = video,
            onConfirm = { newName ->
                onRenameVideo(video.id, newName)
                videoPendingRename = null
            },
            onDismiss = { videoPendingRename = null }
        )
    }

    // Diálogo modal de confirmación antes de eliminar un video individual de la biblioteca
    videoPendingDelete?.let { video ->
        DeleteVideoConfirmDialog(
            video = video,
            onConfirm = {
                onDeleteVideo(video.id)
                videoPendingDelete = null
            },
            onDismiss = { videoPendingDelete = null }
        )
    }

    // Diálogo de confirmación para vaciar todo el historial
    if (showClearConfirmDialog) {
        ClearHistoryConfirmDialog(
            onConfirm = {
                showClearConfirmDialog = false
                onClearHistory()
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Espacio inicial y barra superior
        item {
            Spacer(modifier = Modifier.height(12.dp))
            TopHeaderSection(
                selectedAudioEngine = selectedAudioEngine,
                onOpenAudioSettings = onOpenAudioSettings
            )
        }

        // Título y presentación
        item {
            BrandHeroSection()
        }

        // Banner interactivo del motor de audio
        item {
            AudioEngineBanner(
                selectedAudioEngine = selectedAudioEngine,
                onOpenAudioSettings = onOpenAudioSettings
            )
        }

        // Opciones de importación rápida
        item {
            Text(
                text = "Importar nuevo video",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImportQuickCard(
                    title = "Galería",
                    subtitle = "Fotos y videos",
                    icon = Icons.Default.PhotoLibrary,
                    accentColor = MaterialTheme.colorScheme.primary,
                    testTag = "import_button_gallery",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGallery
                )
                ImportQuickCard(
                    title = "Archivos",
                    subtitle = "Carpetas y SD",
                    icon = Icons.Default.FolderOpen,
                    accentColor = Color(0xFF38BDF8),
                    testTag = "import_button_file_manager",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFileManager
                )
            }
        }

        // Encabezado de la sección de Videos Importados y Vistos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Archivos Importados",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${importedVideos.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (importedVideos.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text(
                            text = "Limpiar todo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Lista de videos importados o Estado Vacío
        if (importedVideos.isEmpty()) {
            item {
                EmptyImportedVideosCard(
                    onImportClick = onOpenGallery
                )
            }
        } else {
            items(
                items = importedVideos,
                key = { it.id }
            ) { video ->
                VideoHistoryCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onRename = { videoPendingRename = video },
                    onDelete = { videoPendingDelete = video }
                )
            }
        }

        // Formatos compatibles
        item {
            SupportedFormatsSection()
        }

        // Información de arquitectura y rendimiento
        item {
            ArchitectureInfoCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
