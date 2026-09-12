package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.example.opengl.VideoEqualizerState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * VideoEqualizerSheet - Panel de Ecualizador de Video en Tiempo Real
 *
 * Permite ajustar brillo, contraste, saturación, gamma y nitidez directamente
 * sobre el fragment shader de GPU en C++ sin pausar ni congelar la reproducción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEqualizerSheet(
    state: VideoEqualizerState,
    onStateChange: (VideoEqualizerState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("video_equalizer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera con título, descripción y botón de restablecer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ecualizador de Video",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Acelerado por GPU (OpenGL ES C++)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (!state.isDefault) {
                    IconButton(
                        onClick = { onStateChange(VideoEqualizerState.DEFAULT) },
                        modifier = Modifier.testTag("equalizer_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Restablecer a valores estándar",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets preconfigurados
            Text(
                text = "PRESETS DE IMAGEN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VideoEqualizerState.PRESETS.keys.toList()) { presetName ->
                    val presetState = VideoEqualizerState.PRESETS[presetName]!!
                    val isSelected = state == presetState
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStateChange(presetState) },
                        label = {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF22222A),
                            labelColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("preset_$presetName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Controles de ajuste fino en tiempo real
            // 1. Brillo [-0.5f a 0.5f]
            EqualizerSliderRow(
                icon = Icons.Default.BrightnessMedium,
                label = "Brillo",
                valueText = "${(state.brightness * 100).roundToInt()}%",
                value = state.brightness,
                valueRange = -0.5f..0.5f,
                onValueChange = { onStateChange(state.copy(brightness = it)) },
                testTag = "slider_brightness"
            )

            // 2. Contraste [0.5f a 2.0f]
            EqualizerSliderRow(
                icon = Icons.Default.Contrast,
                label = "Contraste",
                valueText = "${(state.contrast * 100).roundToInt()}%",
                value = state.contrast,
                valueRange = 0.5f..2.0f,
                onValueChange = { onStateChange(state.copy(contrast = it)) },
                testTag = "slider_contrast"
            )

            // 3. Saturación [0.0f a 2.0f]
            EqualizerSliderRow(
                icon = Icons.Default.ColorLens,
                label = "Saturación",
                valueText = "${(state.saturation * 100).roundToInt()}%",
                value = state.saturation,
                valueRange = 0.0f..2.0f,
                onValueChange = { onStateChange(state.copy(saturation = it)) },
                testTag = "slider_saturation"
            )

            // 4. Corrección Gamma [0.5f a 2.0f]
            EqualizerSliderRow(
                icon = Icons.Default.Visibility,
                label = "Gamma",
                valueText = String.format(Locale.US, "%.2f", state.gamma),
                value = state.gamma,
                valueRange = 0.5f..2.0f,
                onValueChange = { onStateChange(state.copy(gamma = it)) },
                testTag = "slider_gamma"
            )

            // 5. Nitidez / Realce de Bordes [0.0f a 1.0f]
            EqualizerSliderRow(
                icon = Icons.Default.Details,
                label = "Nitidez (Sharpening)",
                valueText = if (state.sharpness <= 0.01f) "Desactivado" else "${(state.sharpness * 100).roundToInt()}%",
                value = state.sharpness,
                valueRange = 0.0f..1.0f,
                onValueChange = { onStateChange(state.copy(sharpness = it)) },
                testTag = "slider_sharpness"
            )

            // 6. Filtro de Luz Azul / Modo Descanso Visual [0.0f a 1.0f]
            EqualizerSliderRow(
                icon = Icons.Default.Nightlight,
                label = "Filtro Luz Azul (Descanso Visual)",
                valueText = if (state.blueLightFilter <= 0.01f) "Desactivado" else "${(state.blueLightFilter * 100).roundToInt()}% (Ámbar cálido)",
                value = state.blueLightFilter,
                valueRange = 0.0f..1.0f,
                onValueChange = { onStateChange(state.copy(blueLightFilter = it)) },
                testTag = "slider_blue_light"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Fila reutilizable para un deslizador del ecualizador de video.
 */
@Composable
private fun EqualizerSliderRow(
    icon: ImageVector,
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
