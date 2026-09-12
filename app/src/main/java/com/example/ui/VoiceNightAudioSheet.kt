package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.audio.OboeAudioEngine
import kotlin.math.roundToInt

/**
 * VoiceNightAudioSheet - Pantalla independiente para el Compresor Dinámico y Modo Voces Claras
 *
 * Procesa en tiempo real las tramas de audio PCM dentro del motor C++ (Google Oboe):
 * 1. Filtro Peaking Vocal: Eleva las frecuencias de inteligibilidad humana (1.5 kHz - 3.5 kHz)
 *    para que los diálogos de películas y series se entiendan a la perfección.
 * 2. Compresor Dinámico (DRC): Atenúa automáticamente explosiones y disparos ensordecedores
 *    mientras eleva susurros, ideal para ver cine de noche sin despertar a nadie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNightAudioSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados locales reactivos conectados con el motor Oboe C++
    var voiceClarityEnabled by remember { mutableStateOf(OboeAudioEngine.isVoiceClarityEnabled()) }
    var voiceGain by remember { mutableFloatStateOf(0.75f) }

    var compressorEnabled by remember { mutableStateOf(OboeAudioEngine.isDynamicCompressorEnabled()) }
    var compressorIntensity by remember { mutableFloatStateOf(0.80f) }

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
            // Cabecera de la herramienta
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
                        imageVector = Icons.Default.GraphicEq,
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
                            text = "Audio Inteligente DSP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "OBOE C++",
                                color = Color(0xFFC4B5FD),
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

            Spacer(modifier = Modifier.height(18.dp))

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
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Switch(
                            checked = voiceClarityEnabled,
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
                                text = "Ganancia de Frecuencias Vocales",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "+${(voiceGain * 8.0f).roundToInt()} dB",
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
                            valueRange = 0.1f..1.0f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF8B5CF6),
                                activeTrackColor = Color(0xFF8B5CF6),
                                inactiveTrackColor = Color(0xFF33333C)
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Switch(
                            checked = compressorEnabled,
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
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF33333C)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 3: PRESETS ACÚSTICOS RÁPIDOS
            Text(
                text = "Perfiles Acústicos Rápidos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Procesamiento nativo de 0 ms de retraso",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC4B5FD)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "El algoritmo de compresión y filtrado vocal se ejecuta directamente dentro del bucle de callback de audio C++ de Google Oboe con enteros y coma flotante acelerada por hardware (NEON). No introduce latencia ni desfase labial con la imagen.",
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
