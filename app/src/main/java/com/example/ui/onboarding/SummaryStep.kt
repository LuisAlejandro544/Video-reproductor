package com.example.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioEngineType
import com.example.model.GraphicsEngineType
import com.example.ui.theme.AppThemeMode

/**
 * SummaryStep.kt - Pantalla 6 del Asistente de Bienvenida (Resumen Final).
 *
 * Propósito:
 * Muestra una tarjeta recapitulativa con todas las opciones elegidas por el usuario
 * antes de finalizar y acceder a la pantalla principal de la biblioteca.
 */
@Composable
fun SummaryStep(
    hasPermission: Boolean,
    selectedAudioEngine: AudioEngineType,
    selectedGraphicsEngine: GraphicsEngineType,
    selectedThemeMode: AppThemeMode,
    useDynamicColor: Boolean,
    scanMessagingApps: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(26.dp)
                )
            }

            Column {
                Text(
                    text = "¡Todo Configurado!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tu reproductor está listo con tus preferencias elegidas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Audio Engine Summary
                SummaryItemRow(
                    label = "Motor de Audio",
                    value = if (selectedAudioEngine == AudioEngineType.OBOE) "Google Oboe (Nativo C++)" else "Android Media3 (AudioTrack)",
                    icon = if (selectedAudioEngine == AudioEngineType.OBOE) Icons.Default.Equalizer else Icons.Default.Headphones,
                    accentColor = if (selectedAudioEngine == AudioEngineType.OBOE) MaterialTheme.colorScheme.primary else Color(0xFF38BDF8)
                )

                // Graphics Engine Summary
                SummaryItemRow(
                    label = "Motor Gráfico",
                    value = if (selectedGraphicsEngine == GraphicsEngineType.VULKAN) "Vulkan 1.1+ (Bajo Nivel / En Desarrollo)" else "OpenGL ES 3.0+ (Estable)",
                    icon = if (selectedGraphicsEngine == GraphicsEngineType.VULKAN) Icons.Default.Speed else Icons.Default.VideogameAsset,
                    accentColor = if (selectedGraphicsEngine == GraphicsEngineType.VULKAN) Color(0xFFF59E0B) else Color(0xFF10B981)
                )

                // Theme Summary
                val themeLabel = when (selectedThemeMode) {
                    AppThemeMode.DARK -> "Modo Oscuro"
                    AppThemeMode.LIGHT -> "Modo Claro"
                    AppThemeMode.SYSTEM -> "Sincronizado con el Sistema"
                } + if (useDynamicColor) " + Material You" else ""
                SummaryItemRow(
                    label = "Tema Visual",
                    value = themeLabel,
                    icon = Icons.Default.Palette,
                    accentColor = MaterialTheme.colorScheme.primary
                )

                // Messaging Scan Summary
                SummaryItemRow(
                    label = "Descubrimiento de Videos",
                    value = if (scanMessagingApps) "Escanear WhatsApp y Telegram" else "Modo Privado (Solo Importar)",
                    icon = if (scanMessagingApps) Icons.Default.QuestionAnswer else Icons.Default.Lock,
                    accentColor = if (scanMessagingApps) Color(0xFF10B981) else Color(0xFF6366F1)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            Text(
                text = "Recuerda que puedes modificar cualquiera de estos parámetros en cualquier momento desde la pantalla de Ajustes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun SummaryItemRow(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
