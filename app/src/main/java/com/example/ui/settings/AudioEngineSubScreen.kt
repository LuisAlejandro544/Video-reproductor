package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType

/**
 * AudioEngineSubScreen.kt - Selección Técnica del Motor de Audio
 *
 * Propósito:
 * Permite al usuario alternar entre el motor estándar Android Media3 (AudioTrack)
 * y el motor de alto rendimiento de baja latencia Google Oboe C++ (AAudio / OpenSL ES).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEngineSubScreen(
    selectedEngine: AudioEngineType,
    onEngineSelected: (AudioEngineType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Motor de Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Configuración del backend de sonido", fontSize = 12.sp, color = Color(0xFF38BDF8))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_engine_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B14)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EngineSelectionCard(
                engineType = AudioEngineType.MEDIA3,
                isSelected = selectedEngine == AudioEngineType.MEDIA3,
                badgeText = "Predeterminado (Universal)",
                badgeColor = Color(0xFF0284C7),
                details = "Procesamiento a través del pipeline estándar de Android Media3 y AudioTrack del sistema. Garantiza sincronización A/V automática, compensación de latencia Bluetooth y máxima compatibilidad.",
                onSelect = { onEngineSelected(AudioEngineType.MEDIA3) },
                testTag = "select_media3_engine_card"
            )

            EngineSelectionCard(
                engineType = AudioEngineType.OBOE,
                isSelected = selectedEngine == AudioEngineType.OBOE,
                badgeText = "Baja Latencia (C++)",
                badgeColor = Color(0xFF10B981),
                details = "Procesamiento nativo en C++ mediante Google Oboe con canal directo AAudio (Android 8.0+) o OpenSL ES. Minimiza la latencia de hardware para procesamiento acústico de bajo nivel.",
                onSelect = { onEngineSelected(AudioEngineType.OBOE) },
                testTag = "select_oboe_engine_card"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "El motor seleccionado se aplicará de inmediato a todos los videos en reproducción.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de selección interactiva para cada motor de audio.
 */
@Composable
fun EngineSelectionCard(
    engineType: AudioEngineType,
    isSelected: Boolean,
    badgeText: String,
    badgeColor: Color,
    details: String,
    onSelect: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
    val bgColor = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.10f) else Color(0xFF0F172A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF38BDF8),
                        unselectedColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = engineType.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = engineType.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = details,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.60f),
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
    }
}
