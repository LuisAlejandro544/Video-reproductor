package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType
import com.example.data.VideoEntity
import com.example.utils.VideoUtils

/**
 * Pantalla principal interactiva de Nova Video Player.
 *
 * Ofrece:
 * 1. Acceso directo a la importación de videos (Galería y Gestor de Archivos de Android).
 * 2. Visualización enriquecida de los archivos importados y reproducidos previamente con:
 *    - Título completo del archivo de video.
 *    - Duración formateada (ej. 04:32 o 01:20:15).
 *    - Tamaño del archivo.
 *    - Barra de progreso de visualización y estado (Visto completo, En pausa o Sin empezar).
 *    - Reproducción inmediata con un solo toque desde la posición guardada.
 * 3. Acceso al selector y telemetría del motor de audio (Oboe C++ vs Media3 AudioTrack).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoImportScreen(
    importedVideos: List<VideoEntity>,
    selectedAudioEngine: AudioEngineType,
    onOpenAudioSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFileManager: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación para vaciar el historial
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Vaciar biblioteca de videos",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar todos los videos del registro de importados y vistos? Tus archivos originales no se borrarán del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearHistory()
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
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
                    onDelete = { onDeleteVideo(video.id) }
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

/**
 * Barra superior con el nombre de la app y acceso directo a Configuración.
 */
@Composable
private fun TopHeaderSection(
    selectedAudioEngine: AudioEngineType,
    onOpenAudioSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Nova Player",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(
            onClick = onOpenAudioSettings,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF0F172A), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .testTag("home_settings_top_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Hero de presentación con estética moderna.
 */
@Composable
private fun BrandHeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Tu centro multimedia de alta fidelidad",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Audio de latencia ultrabaja en C++ con Google Oboe y reproducción fluida en cualquier formato.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/**
 * Banner de motor de audio activo con acceso directo a la configuración.
 */
@Composable
private fun AudioEngineBanner(
    selectedAudioEngine: AudioEngineType,
    onOpenAudioSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onOpenAudioSettings() }
            .testTag("home_audio_settings_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF0284C7).copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Motor Activo:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.65f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF10B981).copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (selectedAudioEngine == AudioEngineType.OBOE) "Oboe C++ (Baja Latencia)" else "Media3 (AudioTrack)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF34D399) else Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (selectedAudioEngine == AudioEngineType.OBOE)
                        "AAudio nativo sin microcortes"
                    else
                        "Pipeline del sistema Android",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Tarjeta compacta para seleccionar Galería o Gestor de Archivos.
 */
@Composable
private fun ImportQuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

/**
 * Tarjeta que muestra un video del historial / importados con su título, duración y progreso.
 */
@Composable
private fun VideoHistoryCard(
    video: VideoEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("video_history_item_${video.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Miniatura simulada con insignia de duración
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8).copy(alpha = 0.7f),
                        modifier = Modifier.size(30.dp)
                    )

                    // Insignia de duración en la esquina inferior derecha
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = video.formattedDuration.ifEmpty { "00:00" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Información textual del archivo
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Título del archivo
                    Text(
                        text = video.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Fila con duración y tamaño
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = video.formattedDuration.ifEmpty { "00:00" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }

                        if (video.formattedSize.isNotEmpty()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.4f))
                            )
                            Text(
                                text = video.formattedSize,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Estado de visualización
                    if (video.isCompleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Visto completo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else if (video.lastPositionMs > 0L) {
                        val progressText = VideoUtils.formatDuration(video.lastPositionMs)
                        Text(
                            text = "En pausa en $progressText",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    } else {
                        Text(
                            text = "Listo para reproducir",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Botón de eliminar del historial
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_video_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar de la lista",
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Barra de progreso de visualización si ya se inició la reproducción
            if (video.durationMs > 0L && video.lastPositionMs > 0L) {
                val progress = (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }

            // Botón de acción principal para reproducir / reanudar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPlay,
                    modifier = Modifier.testTag("play_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (video.lastPositionMs > 0L && !video.isCompleted) "Reanudar" else "Reproducir",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Estado vacío cuando aún no se ha importado ni visto ningún video.
 */
@Composable
private fun EmptyImportedVideosCard(
    onImportClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8).copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Tu biblioteca está lista",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Importa un archivo arriba desde tu Galería o Gestor de Archivos para comenzar a disfrutar de audio de baja latencia.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = onImportClick,
                modifier = Modifier.testTag("empty_state_import_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Seleccionar mi primer video",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                )
            }
        }
    }
}

/**
 * Sección de formatos compatibles nativamente.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportedFormatsSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Formatos soportados nativamente",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val formats = listOf("MP4", "MKV", "WebM", "AVI", "MOV", "TS", "3GP", "FLV")
                formats.forEach { format ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(format, style = MaterialTheme.typography.labelSmall.copy(color = Color.White)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta que detalla el soporte de arquitecturas y aceleración.
 */
@Composable
private fun ArchitectureInfoCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "Arquitectura optimizada en 32 y 64 bits",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Compilado para armeabi-v7a, arm64-v8a, x86 y x86_64 con aceleración nativa por hardware.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
        }
    }
}
