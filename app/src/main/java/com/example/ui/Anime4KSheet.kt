package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opengl.Anime4kMode
import com.example.opengl.VideoEqualizerState
import kotlin.math.roundToInt

/**
 * Anime4KSheet - Hoja de configuración dedicada e independiente para Anime4K.
 *
 * Proporciona control preciso sobre los algoritmos de reconstrucción de trazos,
 * perfilado de líneas oscuras y limpieza de artefactos para animación cel en tiempo real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Anime4KSheet(
    state: VideoEqualizerState,
    onStateChange: (VideoEqualizerState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEnabled = state.anime4kMode != Anime4kMode.OFF

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131722),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("anime4k_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera Anime4K
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(26.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Anime4K (Animación)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "GPU GLSL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA78BFA),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Reconstrucción de trazos y realce para anime cel-shading",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switch principal de activación rápida
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color(0xFF1E2333)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Anime4K Activado" else "Anime4K Desactivado",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color(0xFFA78BFA) else Color.White
                            )
                        )
                        Text(
                            text = if (isEnabled) {
                                "Procesamiento de líneas activo en ${state.anime4kMode.title}"
                            } else {
                                "Toca el interruptor para optimizar la animación"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            onStateChange(
                                state.copy(
                                    anime4kMode = if (checked) Anime4kMode.PRO else Anime4kMode.OFF
                                )
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8B5CF6),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("anime4k_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modos de potencia Anime4K
            Text(
                text = "Modo de procesamiento:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Anime4kModeOption(
                mode = Anime4kMode.LITE,
                icon = Icons.Default.Bolt,
                title = "Anime4K Lite (Recomendado móviles)",
                subtitle = "Reconstrucción bilateral rápida. Máxima fluidez y bajo consumo de batería.",
                isSelected = state.anime4kMode == Anime4kMode.LITE,
                onSelect = {
                    onStateChange(state.copy(anime4kMode = Anime4kMode.LITE))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Anime4kModeOption(
                mode = Anime4kMode.PRO,
                icon = Icons.Default.Brush,
                title = "Anime4K Pro (Máximo detalle)",
                subtitle = "Perfilado direccional y adelgazamiento de trazos borrosos cel-shading.",
                isSelected = state.anime4kMode == Anime4kMode.PRO,
                onSelect = {
                    onStateChange(state.copy(anime4kMode = Anime4kMode.PRO))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Anime4kModeOption(
                mode = Anime4kMode.RESTORE,
                icon = Icons.Default.CleaningServices,
                title = "Anime4K Restauración / Denoise",
                subtitle = "Suaviza el grano y compresión en colores planos preservando contornos.",
                isSelected = state.anime4kMode == Anime4kMode.RESTORE,
                onSelect = {
                    onStateChange(state.copy(anime4kMode = Anime4kMode.RESTORE))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Control deslizante de Intensidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Intensidad del realce",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${(state.anime4kStrength * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    ),
                    modifier = Modifier.testTag("anime4k_strength_label")
                )
            }

            Slider(
                value = state.anime4kStrength,
                onValueChange = { newStrength ->
                    onStateChange(state.copy(anime4kStrength = newStrength))
                },
                valueRange = 0.1f..1.0f,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .testTag("anime4k_strength_slider"),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, if (isEnabled) Color(0xFFA78BFA) else Color(0xFF64748B), CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF8B5CF6),
                            inactiveTrackColor = Color(0xFF272F45),
                            disabledActiveTrackColor = Color(0xFF334155),
                            disabledInactiveTrackColor = Color(0xFF1E293B)
                        ),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta informativa técnica
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Optimización Zero-Copy en GPU",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "El procesado Anime4K corre directamente en el fragment shader C++/OpenGL sobre la textura externa OES. No produce copias en memoria RAM ni latencia de sincronización.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Anime4kModeOption(
    mode: Anime4kMode,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.18f) else Color(0xFF181D2B)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("anime4k_mode_${mode.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color(0xFF242B3D),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFFA78BFA) else Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.60f),
                        lineHeight = 15.sp
                    )
                )
            }
        }
    }
}
