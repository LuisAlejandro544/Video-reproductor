package com.example.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioChannelMode
import com.example.audio.AudioEngineType
import com.example.ui.theme.AppThemeMode

/**
 * SettingsHubView.kt - Menú Principal de Navegación por Categorías (Hub)
 *
 * Propósito:
 * Presenta las opciones del sistema organizadas en tarjetas visuales amplias,
 * facilitando la exploración con una sola mano en pantallas de teléfonos móviles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubView(
    selectedEngine: AudioEngineType,
    currentChannelMode: AudioChannelMode,
    sampleRate: Int,
    currentThemeMode: AppThemeMode,
    useDynamicColor: Boolean,
    onNavigateTo: (SettingsSubScreen) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configuración",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Ajustes y opciones del reproductor",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "PERSONALIZACIÓN Y APARIENCIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            SettingsNavigationCard(
                icon = Icons.Default.Palette,
                iconTint = Color(0xFFF59E0B),
                title = "Apariencia y Material You",
                subtitle = "Tema claro, oscuro o del sistema y color dinámico",
                badge = "${currentThemeMode.displayName}${if (useDynamicColor) " (M.You)" else ""}",
                testTag = "settings_item_appearance",
                onClick = { onNavigateTo(SettingsSubScreen.APPEARANCE) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AUDIO Y REPRODUCCIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            SettingsNavigationCard(
                icon = Icons.Default.Audiotrack,
                iconTint = Color(0xFF38BDF8),
                title = "Motor de Audio",
                subtitle = "Alternar entre Google Oboe C++ y Android Media3",
                badge = if (selectedEngine == AudioEngineType.OBOE) "Oboe C++" else "Media3",
                testTag = "settings_item_audio_engine",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_ENGINE) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.Headphones,
                iconTint = Color(0xFF00E5FF),
                title = "Canales de Audio (Estéreo / Mono)",
                subtitle = "Conversión a estéreo, mono centrado o efecto Haas 3D",
                badge = when (currentChannelMode) {
                    AudioChannelMode.STEREO -> "Estéreo"
                    AudioChannelMode.MONO -> "Mono"
                    AudioChannelMode.SPATIAL_HAAS -> "Haas 3D"
                },
                testTag = "settings_item_audio_channels",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_CHANNELS) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.GraphicEq,
                iconTint = Color(0xFF10B981),
                title = "Prueba de Sonido",
                subtitle = "Sintetizador senoidal 440 Hz para verificación física",
                badge = "440 Hz PCM",
                testTag = "settings_item_audio_test",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_TEST) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "DIAGNÓSTICO Y SISTEMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            SettingsNavigationCard(
                icon = Icons.Default.Memory,
                iconTint = Color(0xFFA855F7),
                title = "Telemetría y Rendimiento",
                subtitle = "Monitoreo en tiempo real de tramas C++, buffers y RAM",
                badge = if (sampleRate > 0) "${sampleRate / 1000} kHz" else "Activo",
                testTag = "settings_item_telemetry",
                onClick = { onNavigateTo(SettingsSubScreen.TELEMETRY) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.Info,
                iconTint = Color(0xFF3B82F6),
                title = "Arquitectura y Distribución",
                subtitle = "Compatibilidad 32/64 bits, Android Go y distribución APK",
                badge = "APK / Uptodown",
                testTag = "settings_item_about",
                onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.Movie,
                iconTint = Color(0xFF06B6D4),
                title = "Formatos y Compatibilidad",
                subtitle = "Contenedores de video, códecs de audio y subtítulos",
                badge = "Universal",
                testTag = "settings_item_formats",
                onClick = { onNavigateTo(SettingsSubScreen.FORMATS) }
            )
        }
    }
}

/**
 * Tarjeta interactiva de acceso para cada pantalla de configuración.
 * Diseño robusto con protección contra wrapping indebido en pantallas estrechas.
 */
@Composable
fun SettingsNavigationCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    badge?.let { badgeText ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconTint.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
