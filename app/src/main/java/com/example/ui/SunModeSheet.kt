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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opengl.VideoEqualizerState
import kotlin.math.roundToInt

/**
 * SunModeSheet - Pantalla independiente para el Modo Sol Extremo y Accesibilidad
 *
 * Ejecuta en GPU un algoritmo GLSL de realce adaptativo que:
 * 1. Eleva exponencialmente las zonas oscuras y sombras sin sobreexponer las altas luces.
 * 2. Compensa el desvanecimiento de color provocado por el reflejo solar en el cristal del móvil.
 * 3. Proporciona contraste extremo para personas con dificultades visuales o visión al aire libre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunModeSheet(
    state: VideoEqualizerState,
    onStateChange: (VideoEqualizerState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isSunModeActive = state.sunMode > 0.01f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("sun_mode_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera de la herramienta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
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
                            text = "Modo Sol Extremo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "GLSL GPU",
                                color = Color(0xFFFCD34D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Máxima visibilidad bajo luz solar directa y alto contraste",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switch principal de activación
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activar Modo Sol / Exteriores",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isSunModeActive) "Shader activo: sombras elevadas y microcontraste expandido"
                                   else "Desactivado: curvas de color estándar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isSunModeActive,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onStateChange(
                                    state.copy(
                                        sunMode = if (state.sunMode > 0.05f) state.sunMode else 0.85f,
                                        brightness = if (state.brightness == 0.0f) 0.06f else state.brightness,
                                        contrast = if (state.contrast == 1.0f) 1.30f else state.contrast,
                                        gamma = if (state.gamma == 1.0f) 1.20f else state.gamma
                                    )
                                )
                            } else {
                                onStateChange(state.copy(sunMode = 0.0f))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF16161A),
                            checkedTrackColor = Color(0xFFF59E0B),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0xFF33333C)
                        ),
                        modifier = Modifier.testTag("sun_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control deslizante de intensidad de compensación solar
            if (isSunModeActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensidad de Realce Solar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${(state.sunMode * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Slider(
                        value = state.sunMode,
                        onValueChange = { newValue ->
                            onStateChange(state.copy(sunMode = newValue))
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("sun_mode_intensity_slider"),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color.White, CircleShape)
                                    .border(2.dp, Color(0xFFF59E0B), CircleShape)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(4.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFF59E0B),
                                    inactiveTrackColor = Color(0xFF33333C)
                                ),
                                drawStopIndicator = null,
                                thumbTrackGapSize = 0.dp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Perfiles de Accesibilidad y Luz Exterior
            Text(
                text = "Perfiles de Visibilidad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val profiles = listOf(
                Triple("Sol Directo (Pleno Día)", "Máxima elevación de sombras y saturación para sol abrasador", 1.0f),
                Triple("Alto Contraste Accesibilidad", "Contraste nítido y separación de bordes para baja visibilidad", 0.80f),
                Triple("Equilibrado al Aire Libre", "Ajuste moderado para terrazas o paseos en días nublados", 0.50f)
            )

            profiles.forEach { (title, subtitle, intensity) ->
                val isCurrent = isSunModeActive && kotlin.math.abs(state.sunMode - intensity) < 0.12f
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) Color(0xFFF59E0B).copy(alpha = 0.18f) else Color(0xFF222228),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onStateChange(
                                state.copy(
                                    sunMode = intensity,
                                    brightness = 0.06f,
                                    contrast = 1.35f,
                                    gamma = 1.22f,
                                    saturation = 1.25f,
                                    sharpness = 0.40f
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) Color(0xFFFCD34D) else Color.White
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta explicativa de tecnología en GPU
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1C22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "¿Cómo funciona en la GPU?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "El shader OpenGL ES procesa cada fotograma a nivel de píxel aplicando una curva de transferencia luminosa que expande las sombras empastadas sin sobreexponer las altas luces. Ideal para ver videos en exteriores sin necesidad de elevar manualmente el brillo del teléfono a niveles que recalienten el dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
