package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtitles.SubtitleSize
import com.example.subtitles.SubtitleTrackItem

/**
 * SubtitlesBottomSheet - Panel modal para la selección y configuración de subtítulos.
 *
 * Características:
 * - Selección e importación directa de archivos externos `.srt` (SubRip) y `.vtt` (WebVTT).
 * - Detección y activación de pistas de texto embebidas en el contenedor de video (MKV/MP4).
 * - Opción rápida para desactivar o apagar los subtítulos en pantalla.
 * - Ajuste de tamaño de fuente (Pequeño, Normal, Grande) adaptado a pantallas móviles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlesBottomSheet(
    subtitlesEnabled: Boolean,
    availableTracks: List<SubtitleTrackItem>,
    selectedTrackId: String?,
    externalSubtitle: SubtitleTrackItem?,
    selectedSize: SubtitleSize,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectTrack: (SubtitleTrackItem) -> Unit,
    onPickExternalSubtitle: (Uri) -> Unit,
    onRemoveExternalSubtitle: () -> Unit,
    onSizeChanged: (SubtitleSize) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Lanzador SAF para seleccionar archivos de subtítulos SRT o VTT desde el almacenamiento
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickExternalSubtitle(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("subtitles_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Cabecera con título y botón de cierre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Subtítulos",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Soporte nativo SRT, WebVTT y SSA/ASS (Rust Core)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("subtitles_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar menú de subtítulos",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de tamaño de subtítulo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tamaño:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtitleSize.entries.forEach { sizeOption ->
                        FilterChip(
                            selected = selectedSize == sizeOption,
                            onClick = { onSizeChanged(sizeOption) },
                            label = {
                                Text(
                                    text = sizeOption.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedSize == sizeOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF222228),
                                labelColor = Color.White.copy(alpha = 0.7f),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedSize == sizeOption) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("subtitle_size_${sizeOption.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Opción para Desactivar Subtítulos
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (!subtitlesEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF222228)
                ),
                border = BorderStroke(
                    1.dp,
                    if (!subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSubtitles(false) }
                    .testTag("subtitles_disable_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SubtitlesOff,
                            contentDescription = null,
                            tint = if (!subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Desactivar subtítulos",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (!subtitlesEnabled) FontWeight.Bold else FontWeight.Normal,
                                color = if (!subtitlesEnabled) Color.White else Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                    if (!subtitlesEnabled) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Desactivados actualmente",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sección: Cargar archivo externo (.srt / .vtt)
            Text(
                text = "SUBTÍTULO EXTERNO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (externalSubtitle != null) {
                // Subtítulo externo actualmente cargado
                val isExternalSelected = subtitlesEnabled && selectedTrackId == externalSubtitle.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExternalSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF222228)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExternalSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onToggleSubtitles(true)
                            onSelectTrack(externalSubtitle)
                        }
                        .testTag("external_subtitle_active_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = if (isExternalSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = externalSubtitle.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                                val formatText = when {
                                    externalSubtitle.mimeType.contains("ssa") || externalSubtitle.label.endsWith(".ass", true) || externalSubtitle.label.endsWith(".ssa", true) -> {
                                        "Formato SSA/ASS (Rust Core)"
                                    }
                                    externalSubtitle.mimeType.contains("vtt") -> "Formato WebVTT (.vtt)"
                                    else -> "Formato SubRip (.srt)"
                                }
                                Text(
                                    text = formatText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp
                                    )
                                )
                                if (externalSubtitle.assInfo != null) {
                                    val info = externalSubtitle.assInfo
                                    Text(
                                        text = "Rust: ${info.dialogueCount} diálogos • ${info.styleCount} estilos${if (info.playResX > 0) " • ${info.playResX}x${info.playResY}" else ""}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExternalSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Pista activa",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 6.dp)
                                )
                            }
                            IconButton(
                                onClick = onRemoveExternalSubtitle,
                                modifier = Modifier.testTag("remove_external_subtitle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar subtítulo externo",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botón para seleccionar o cambiar archivo .srt / .vtt / .ass / .ssa
            Button(
                onClick = {
                    // Permitir selección de tipos de subtítulos y archivos de texto
                    subtitlePickerLauncher.launch(
                        arrayOf(
                            "application/x-subrip",
                            "text/vtt",
                            "text/x-ssa",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282832),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_subtitle_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (externalSubtitle != null) "Cargar otro archivo (.srt / .vtt / .ass)" else "Cargar archivo de subtítulos (.srt / .vtt / .ass)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            // Sección: Pistas internas del video (si las hay)
            val internalTracks = availableTracks.filter { !it.isExternal }
            if (internalTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PISTAS EMBEBIDAS EN EL VIDEO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((internalTracks.size * 54).coerceAtMost(160).dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(internalTracks) { track ->
                        val isSelected = subtitlesEnabled && selectedTrackId == track.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF222228)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleSubtitles(true)
                                    onSelectTrack(track)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = track.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionada",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
