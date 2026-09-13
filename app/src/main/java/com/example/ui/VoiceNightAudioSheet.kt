package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import kotlin.math.roundToInt

/**
 * VoiceNightAudioSheet - Pantalla independiente para el Compresor Dinámico y Modo Voces Claras
 *
 * Procesa en tiempo real las tramas de audio PCM de manera universal:
 * - Con Google Oboe C++: Mediante búfer de callback de hardware de ultra baja latencia con SIMD NEON.
 * - Con Android Media3: Mediante pipeline de AudioProcessor PCM en tiempo real integrado en ExoPlayer.
 *
 * Funcionalidades clave:
 * 1. Filtro Peaking Vocal: Eleva las frecuencias de inteligibilidad humana (1.5 kHz - 3.5 kHz)
 *    para que los diálogos de películas y series se entiendan a la perfección.
 * 2. Compresor Dinámico (DRC): Atenúa automáticamente explosiones y disparos ensordecedores
 *    mientras eleva susurros, ideal para ver cine de noche sin despertar a nadie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNightAudioSheet(
    currentAudioEngine: AudioEngineType = AudioEngineType.MEDIA3,
    onSwitchToOboe: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados locales reactivos conectados con el motor de audio Oboe / Media3
    var voiceClarityEnabled by remember { mutableStateOf(OboeAudioEngine.isVoiceClarityEnabled()) }
    var voiceGain by remember { mutableFloatStateOf(OboeAudioEngine.voiceClarityGain) }

    var compressorEnabled by remember { mutableStateOf(OboeAudioEngine.isDynamicCompressorEnabled()) }
    var compressorIntensity by remember { mutableFloatStateOf(OboeAudioEngine.compressorIntensity) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("voice_night_audio_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val isOboeActive = (currentAudioEngine == AudioEngineType.OBOE)

            // Cabecera de la herramienta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOboeActive) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isOboeActive) Color(0xFFA78BFA) else Color(0xFF38BDF8),
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
                            text = "Audio Inteligente DSP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isOboeActive) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF0284C7).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = if (isOboeActive) "OBOE C++" else "MEDIA3 DSP",
                                color = if (isOboeActive) Color(0xFFC4B5FD) else Color(0xFF7DD3FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Voces claras y compresión nocturna de rango dinámico",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BANNER INFORMATIVO DEL MOTOR ACTIVO (Universal, disponible en Media3 y Oboe)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOboeActive) Color(0xFF1E1A29) else Color(0xFF13202E)
                ),
                border = BorderStroke(1.dp, if (isOboeActive) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color(0xFF0284C7).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOboeActive) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF0284C7).copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isOboeActive) Icons.Default.Audiotrack else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isOboeActive) Color(0xFFA78BFA) else Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOboeActive) "Motor Activo: Google Oboe C++ (Nativo)" else "Motor Activo: Android Media3 (PCM Pipeline)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isOboeActive) Color(0xFFC4B5FD) else Color(0xFF7DD3FC)
                        )
                        Text(
                            text = if (isOboeActive)
                                "Procesamiento de ultra baja latencia en búfer de hardware con aceleración matemática SIMD NEON."
                            else
                                "Procesamiento en tiempo real sobre el flujo PCM de Media3 integrado sin alterar la sincronía audio/video.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // SECCIÓN 1: MODO VOCES CLARAS (CLEAR DIALOGUE)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (voiceClarityEnabled) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Modo Voces Claras",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Realza diálogos y consonantes (1.5 - 3.5 kHz)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = voiceClarityEnabled,
                            enabled = true,
                            onCheckedChange = { checked ->
                                voiceClarityEnabled = checked
                                OboeAudioEngine.setVoiceClarity(checked, voiceGain)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF16161A),
                                checkedTrackColor = Color(0xFF8B5CF6),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color(0xFF33333C)
                            ),
                            modifier = Modifier.testTag("voice_clarity_switch")
                        )
                    }

                    if (voiceClarityEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ganancia Vocal",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "+${(voiceGain * 6.0f).roundToInt()} dB",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                        }
                        Slider(
                            value = voiceGain,
                            onValueChange = { newGain ->
                                voiceGain = newGain
                                OboeAudioEngine.setVoiceClarity(true, newGain)
                            },
                            valueRange = 0.2f..1.2f,
                            steps = 9,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color.White, CircleShape)
                                        .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(4.dp),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF8B5CF6),
                                        inactiveTrackColor = Color(0xFF33333C)
                                    ),
                                    drawStopIndicator = null,
                                    thumbTrackGapSize = 0.dp
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECCIÓN 2: COMPRESOR DINÁMICO (MODO NOCHE / DRC)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = if (compressorEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Compresor Dinámico (Modo Noche)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Atenúa explosiones y eleva susurros automáticamente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = compressorEnabled,
                            enabled = true,
                            onCheckedChange = { checked ->
                                compressorEnabled = checked
                                OboeAudioEngine.setDynamicCompressor(checked, compressorIntensity)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF16161A),
                                checkedTrackColor = Color(0xFF38BDF8),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color(0xFF33333C)
                            ),
                            modifier = Modifier.testTag("dynamic_compressor_switch")
                        )
                    }

                    if (compressorEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Intensidad de Compresión de Picos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${(compressorIntensity * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Slider(
                            value = compressorIntensity,
                            onValueChange = { newIntensity ->
                                compressorIntensity = newIntensity
                                OboeAudioEngine.setDynamicCompressor(true, newIntensity)
                            },
                            valueRange = 0.2f..1.0f,
                            steps = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color.White, CircleShape)
                                        .border(2.dp, Color(0xFF38BDF8), CircleShape)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(4.dp),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF38BDF8),
                                        inactiveTrackColor = Color(0xFF33333C)
                                    ),
                                    drawStopIndicator = null,
                                    thumbTrackGapSize = 0.dp
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 3: PRESETS ACÚSTICOS RÁPIDOS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Perfiles Acústicos Rápidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            val audioPresets = listOf(
                Triple("Cine Nocturno (Sin Sobresaltos)", "Máxima protección de picos sonoros con diálogos claros", Pair(true, true)),
                Triple("Voces en Primer Plano", "Prioridad a conversaciones, entrevistas y podcasts", Pair(true, false)),
                Triple("Protección de Explosiones", "Compresor de impacto alto para películas bélicas y de acción", Pair(false, true)),
                Triple("Audio Plano / Estándar", "Sonido original de la pista sin ecualización adicional", Pair(false, false))
            )

            audioPresets.forEach { (name, desc, statePair) ->
                val (voiceOn, compOn) = statePair
                val isSelected = (voiceClarityEnabled == voiceOn) && (compressorEnabled == compOn)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.18f) else Color(0xFF222228),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            voiceClarityEnabled = voiceOn
                            compressorEnabled = compOn
                            OboeAudioEngine.setVoiceClarity(voiceOn, voiceGain)
                            OboeAudioEngine.setDynamicCompressor(compOn, compressorIntensity)
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
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFFC4B5FD) else Color.White
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta informativa de rendimiento
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
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isOboeActive) Color(0xFF8B5CF6) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Procesamiento nativo con sincronía de 0 ms",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOboeActive) Color(0xFFC4B5FD) else Color(0xFF7DD3FC)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isOboeActive)
                            "El algoritmo de compresión y filtrado vocal se ejecuta directamente dentro del bucle de callback C++ de Google Oboe con enteros y coma flotante acelerada por hardware (NEON). No introduce latencia ni desfase labial."
                        else
                            "El algoritmo de compresión y realce vocal se aplica en el pipeline PCM de Media3 en tiempo real, manteniendo perfecta sincronización labial y bajo impacto térmico en el dispositivo.",
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
