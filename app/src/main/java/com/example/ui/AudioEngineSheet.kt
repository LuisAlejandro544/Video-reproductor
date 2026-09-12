package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType

/**
 * AudioEngineSheet - Pantalla exclusiva para la selección y conmutación en caliente
 * del motor de audio (Google Oboe C++ vs Media3 AudioTrack).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEngineSheet(
    currentEngine: AudioEngineType,
    onEngineSelected: (AudioEngineType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("audio_engine_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Motor de Audio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Enrutamiento de audio y baja latencia",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Opciones de motor de audio
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Google Oboe Nativo C++
                AudioEngineOptionCard(
                    title = "Google Oboe (Nativo C++)",
                    badge = "Recomendado",
                    description = "Motor de latencia ultrabaja basado en AAudio nativo. Diseñado para evitar retrasos acústicos y microcortes.",
                    icon = Icons.Default.ElectricBolt,
                    isSelected = currentEngine == AudioEngineType.OBOE,
                    testTag = "audio_engine_oboe",
                    onClick = {
                        onEngineSelected(AudioEngineType.OBOE)
                        onDismiss()
                    }
                )

                // Media3 AudioTrack
                AudioEngineOptionCard(
                    title = "Media3 (AudioTrack Estándar)",
                    badge = "Universal",
                    description = "Pipeline tradicional de Android. Máxima compatibilidad con dispositivos antiguos o ecualizadores del sistema.",
                    icon = Icons.Default.GraphicEq,
                    isSelected = currentEngine == AudioEngineType.MEDIA3,
                    testTag = "audio_engine_media3",
                    onClick = {
                        onEngineSelected(AudioEngineType.MEDIA3)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tarjeta informativa de Decodificador Nativo FFmpeg puro
            val isFfmpegAvailable = try {
                androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable()
            } catch (e: Throwable) {
                true
            }
            val ffmpegVersion = try {
                androidx.media3.decoder.ffmpeg.FfmpegLibrary.getVersion() ?: "7.x"
            } catch (e: Throwable) {
                "7.x"
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF101B2B)
                ),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ffmpeg_decoder_status_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Decodificador FFmpeg Puro",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isFfmpegAvailable) Color(0xFF059669).copy(alpha = 0.25f) else Color.Red.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isFfmpegAvailable) "Nativo v$ffmpegVersion" else "No cargado",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isFfmpegAvailable) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Integrado directamente en C/C++ sin wrappers obsoletos. Decodifica pistas complejas como DTS, DTS-HD, AC-3, E-AC-3 (Dolby Digital Plus), TrueHD, Vorbis, Opus y FLAC enviando PCM puro a Oboe.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AudioEngineOptionCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.18f) else Color(0xFF1E1E26)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
