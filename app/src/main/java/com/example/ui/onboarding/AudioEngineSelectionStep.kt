package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioEngineType

/**
 * AudioEngineSelectionStep.kt - Pantalla 2 del Asistente de Bienvenida.
 *
 * Propósito:
 * Permite al usuario seleccionar la tecnología de reproducción de audio preferida:
 * - Media3 (AudioTrack estándar de Android)
 * - Google Oboe C++ (Motor nativo de ultra-baja latencia)
 *
 * Presenta con total transparencia las ventajas y desventajas técnicas de cada opción
 * para que el usuario tome una decisión informada según sus hábitos y periféricos de audio.
 */
@Composable
fun AudioEngineSelectionStep(
    selectedEngine: AudioEngineType,
    onEngineSelected: (AudioEngineType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "¿Qué motor de audio prefieres?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Nova Player cuenta con dos motores de salida de sonido. Puedes cambiar de motor en cualquier momento desde la configuración o durante la reproducción.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Opción 1: Android Media3 (AudioTrack estándar)
        SelectionCardWithProsCons(
            title = "Android Media3",
            badgeText = "Recomendado para Bluetooth",
            badgeColor = Color(0xFF38BDF8),
            icon = Icons.Default.Headphones,
            isSelected = selectedEngine == AudioEngineType.MEDIA3,
            onSelect = { onEngineSelected(AudioEngineType.MEDIA3) },
            advantages = listOf(
                "Compatibilidad universal y óptima sincronización A/V en Bluetooth.",
                "Consumo de batería mínimo con el búfer estándar del sistema."
            ),
            disadvantages = listOf(
                "Mayor latencia de respuesta en el búfer acústico del sistema."
            ),
            testTag = "onboarding_select_media3"
        )

        // Opción 2: Google Oboe C++ (Nativo)
        SelectionCardWithProsCons(
            title = "Google Oboe C++",
            badgeText = "Ultra Baja Latencia",
            badgeColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Equalizer,
            isSelected = selectedEngine == AudioEngineType.OBOE,
            onSelect = { onEngineSelected(AudioEngineType.OBOE) },
            advantages = listOf(
                "Latencia prácticamente nula mediante acceso directo por hardware C++.",
                "Procesamiento PCM flotante de 32 bits y aceleración SIMD NEON."
            ),
            disadvantages = listOf(
                "En algunos audífonos Bluetooth propietarios puede requerir calibración."
            ),
            testTag = "onboarding_select_oboe"
        )
    }
}
