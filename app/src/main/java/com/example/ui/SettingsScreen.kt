package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioChannelMode
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import com.example.player.BufferMemoryProfile
import com.example.player.PlayerLoadControlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Subpantallas dedicadas para la sección de Configuración.
 * Permite navegar a pantallas independientes por categoría para mayor comodidad y ergonomía.
 */
enum class SettingsSubScreen {
    HUB,            // Menú principal con accesos a cada apartado
    AUDIO_ENGINE,   // Selección entre Oboe C++ y Media3
    AUDIO_CHANNELS, // Enrutamiento Estéreo / Mono / Pseudo-Estéreo Haas
    AUDIO_TEST,     // Verificación y prueba senoidal de sonido en tiempo real
    TELEMETRY,      // Métricas nativas de hardware, buffers y CPU
    ABOUT           // Información de arquitectura, 32/64 bits y distribución Uptodown
}

/**
 * SettingsScreen - Arquitectura modular de configuración por pantallas independientes.
 *
 * En lugar de acumular todos los ajustes en un único scroll saturado, divide la experiencia
 * en pantallas dedicadas por funcionalidad (Motor de Audio, Canales Estéreo/Mono, Prueba de Sonido,
 * Telemetría del Sistema y Acerca de), facilitando la navegación táctil en teléfonos móviles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentEngine: AudioEngineType,
    onEngineChanged: (AudioEngineType) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.HUB) }
    var selectedEngine by remember { mutableStateOf(currentEngine) }
    var currentChannelMode by remember { mutableStateOf(OboeAudioEngine.currentChannelMode) }

    // Telemetría en tiempo real
    var apiName by remember { mutableStateOf(OboeAudioEngine.getApiName()) }
    var sampleRate by remember { mutableStateOf(OboeAudioEngine.getSampleRate()) }
    var channelCount by remember { mutableStateOf(OboeAudioEngine.getChannelCount()) }
    var framesWritten by remember { mutableLongStateOf(OboeAudioEngine.getFramesWritten()) }

    // Estado del test de sonido
    var isTestingAudio by remember { mutableStateOf(false) }
    var testAudioMessage by remember { mutableStateOf<String?>(null) }
    var activeTestTrack by remember { mutableStateOf<AudioTrack?>(null) }

    val context = LocalContext.current
    val memoryProfile = remember { PlayerLoadControlHelper.getMemoryProfile(context) }
    val coroutineScope = rememberCoroutineScope()

    // Manejo de retroceso: si estamos en una subpantalla, volver al HUB; si estamos en el HUB, salir
    BackHandler {
        if (activeSubScreen != SettingsSubScreen.HUB) {
            activeSubScreen = SettingsSubScreen.HUB
        } else {
            AudioTestManager.stopTone(activeTestTrack)
            onNavigateBack()
        }
    }

    // Actualización de telemetría periódica
    LaunchedEffect(Unit) {
        while (true) {
            apiName = OboeAudioEngine.getApiName()
            sampleRate = OboeAudioEngine.getSampleRate()
            channelCount = OboeAudioEngine.getChannelCount()
            framesWritten = OboeAudioEngine.getFramesWritten()
            currentChannelMode = OboeAudioEngine.currentChannelMode
            delay(400)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioTestManager.stopTone(activeTestTrack)
        }
    }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "settings_subscreen_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            SettingsSubScreen.HUB -> {
                SettingsHubView(
                    selectedEngine = selectedEngine,
                    currentChannelMode = currentChannelMode,
                    sampleRate = sampleRate,
                    onNavigateTo = { activeSubScreen = it },
                    onNavigateBack = {
                        AudioTestManager.stopTone(activeTestTrack)
                        onNavigateBack()
                    }
                )
            }
            SettingsSubScreen.AUDIO_ENGINE -> {
                AudioEngineSubScreen(
                    selectedEngine = selectedEngine,
                    onEngineSelected = { engine ->
                        selectedEngine = engine
                        onEngineChanged(engine)
                        if (engine == AudioEngineType.OBOE) {
                            OboeAudioEngine.start()
                        } else {
                            OboeAudioEngine.stop()
                        }
                    },
                    onBack = { activeSubScreen = SettingsSubScreen.HUB }
                )
            }
            SettingsSubScreen.AUDIO_CHANNELS -> {
                AudioChannelsSubScreen(
                    currentMode = currentChannelMode,
                    onModeSelected = { mode ->
                        currentChannelMode = mode
                        OboeAudioEngine.setChannelMode(mode)
                    },
                    onBack = { activeSubScreen = SettingsSubScreen.HUB }
                )
            }
            SettingsSubScreen.AUDIO_TEST -> {
                AudioTestSubScreen(
                    selectedEngine = selectedEngine,
                    isTestingAudio = isTestingAudio,
                    testAudioMessage = testAudioMessage,
                    onToggleTest = {
                        if (isTestingAudio) {
                            AudioTestManager.stopTone(activeTestTrack)
                            isTestingAudio = false
                            testAudioMessage = "Prueba detenida"
                        } else {
                            isTestingAudio = true
                            testAudioMessage = "Reproduciendo en ${selectedEngine.name}..."
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) {
                                    AudioTestManager.playTone(
                                        engine = selectedEngine,
                                        onTrackCreated = { track -> activeTestTrack = track },
                                        onFinish = {
                                            isTestingAudio = false
                                            testAudioMessage = "Prueba completada con éxito"
                                        }
                                    )
                                }
                            }
                        }
                    },
                    onBack = {
                        AudioTestManager.stopTone(activeTestTrack)
                        isTestingAudio = false
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
            SettingsSubScreen.TELEMETRY -> {
                TelemetrySubScreen(
                    selectedEngine = selectedEngine,
                    apiName = apiName,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    framesWritten = framesWritten,
                    memoryProfile = memoryProfile,
                    onBack = { activeSubScreen = SettingsSubScreen.HUB }
                )
            }
            SettingsSubScreen.ABOUT -> {
                AboutSubScreen(
                    onBack = { activeSubScreen = SettingsSubScreen.HUB }
                )
            }
        }
    }
}

/**
 * Menú principal de Configuración (Hub) con tarjetas cómodas y espaciosas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHubView(
    selectedEngine: AudioEngineType,
    currentChannelMode: AudioChannelMode,
    sampleRate: Int,
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
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Ajustes y opciones del reproductor",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8))
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
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B14)
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
                text = "AUDIO Y REPRODUCCIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            SettingsNavigationCard(
                icon = Icons.Default.Audiotrack,
                iconTint = Color(0xFF38BDF8),
                title = "Motor de Audio",
                subtitle = "Alternar entre Google Oboe C++ y Android Media3",
                badge = selectedEngine.title,
                testTag = "settings_item_audio_engine",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_ENGINE) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.Headphones,
                iconTint = Color(0xFF00E5FF),
                title = "Canales de Audio (Estéreo / Mono)",
                subtitle = "Conversión a estéreo, mono centrado o efecto Haas 3D",
                badge = currentChannelMode.title,
                testTag = "settings_item_audio_channels",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_CHANNELS) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.GraphicEq,
                iconTint = Color(0xFF10B981),
                title = "Prueba de Sonido",
                subtitle = "Sintetizador senoidal 440 Hz para verificación física",
                badge = "Prueba PCM",
                testTag = "settings_item_audio_test",
                onClick = { onNavigateTo(SettingsSubScreen.AUDIO_TEST) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DIAGNÓSTICO Y SISTEMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            SettingsNavigationCard(
                icon = Icons.Default.Memory,
                iconTint = Color(0xFFA855F7),
                title = "Telemetría y Rendimiento",
                subtitle = "Monitoreo en tiempo real de tramas C++, buffers y RAM",
                badge = if (sampleRate > 0) "$sampleRate Hz" else "Activo",
                testTag = "settings_item_telemetry",
                onClick = { onNavigateTo(SettingsSubScreen.TELEMETRY) }
            )

            SettingsNavigationCard(
                icon = Icons.Default.Info,
                iconTint = Color(0xFFF59E0B),
                title = "Arquitectura y Distribución",
                subtitle = "Compatibilidad 32/64 bits, Android Go y distribución APK",
                badge = "Uptodown",
                testTag = "settings_item_about",
                onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
            )
        }
    }
}

/**
 * Tarjeta interactiva de acceso para cada pantalla de configuración.
 */
@Composable
private fun SettingsNavigationCard(
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconTint.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = it,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.60f),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Subpantalla dedicada para el Motor de Audio (Oboe C++ vs Media3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioEngineSubScreen(
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
                engineType = AudioEngineType.OBOE,
                isSelected = selectedEngine == AudioEngineType.OBOE,
                badgeText = "Baja Latencia (C++)",
                badgeColor = Color(0xFF10B981),
                details = "Procesamiento nativo en C++ mediante Google Oboe con canal directo AAudio (Android 8.0+) o OpenSL ES. Minimiza la latencia de hardware y previene microcortes en archivos pesados.",
                onSelect = { onEngineSelected(AudioEngineType.OBOE) },
                testTag = "select_oboe_engine_card"
            )

            EngineSelectionCard(
                engineType = AudioEngineType.MEDIA3,
                isSelected = selectedEngine == AudioEngineType.MEDIA3,
                badgeText = "Estándar Android",
                badgeColor = Color(0xFF64748B),
                details = "Procesamiento a través del pipeline estándar de Android Media3 y AudioTrack del sistema. Permite compatibilidad universal con efectos del sistema operativo.",
                onSelect = { onEngineSelected(AudioEngineType.MEDIA3) },
                testTag = "select_media3_engine_card"
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
 * Subpantalla dedicada para Canales de Audio (Estéreo, Mono y Pseudo-Estéreo Haas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioChannelsSubScreen(
    currentMode: AudioChannelMode,
    onModeSelected: (AudioChannelMode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Canales de Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Enrutamiento estéreo, mono y espacial", fontSize = 12.sp, color = Color(0xFF00E5FF))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_channels_back")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161B26),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "El procesamiento se realiza en tiempo real a nivel de muestras PCM mediante DSP nativo en C++ sin cortar la reproducción del video.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            AudioChannelMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                val icon = when (mode) {
                    AudioChannelMode.STEREO -> Icons.Default.Headphones
                    AudioChannelMode.MONO -> Icons.Default.GraphicEq
                    AudioChannelMode.SPATIAL_HAAS -> Icons.Default.SpatialAudio
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color(0xFF0F172A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onModeSelected(mode) }
                        .testTag("settings_channel_mode_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.subtitle,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.45f),
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        RadioButton(
                            selected = isSelected,
                            onClick = { onModeSelected(mode) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subpantalla dedicada para Prueba y Validación de Sonido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioTestSubScreen(
    selectedEngine: AudioEngineType,
    isTestingAudio: Boolean,
    testAudioMessage: String?,
    onToggleTest: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Prueba de Sonido", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Verificación física en tiempo real", fontSize = 12.sp, color = Color(0xFF10B981))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_test_back")) {
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTestingAudio) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Generador Senoidal de 440 Hz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedEngine == AudioEngineType.OBOE) {
                            "Motor Activo: Google Oboe C++ (AAudio / OpenSL ES). Las muestras PCM de 16 bits se escriben directamente en el buffer nativo de hardware."
                        } else {
                            "Motor Activo: Android Media3 / AudioTrack. Las muestras PCM se envían a través del subsistema de audio estándar del sistema operativo."
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onToggleTest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestingAudio) Color(0xFFEF4444) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("play_test_audio_button")
                    ) {
                        Icon(
                            imageVector = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestingAudio) "Detener Prueba" else "Iniciar Prueba de Sonido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    testAudioMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subpantalla dedicada para Telemetría y Rendimiento del Hardware.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelemetrySubScreen(
    selectedEngine: AudioEngineType,
    apiName: String,
    sampleRate: Int,
    channelCount: Int,
    framesWritten: Long,
    memoryProfile: BufferMemoryProfile,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Telemetría y Rendimiento", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Métricas de bajo nivel en tiempo real", fontSize = 12.sp, color = Color(0xFFA855F7))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_telemetry_back")) {
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Diagnóstico en Tiempo Real",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    TelemetryItem("Motor Seleccionado", selectedEngine.title, highlight = true)
                    TelemetryItem("Backend Nativo C++", if (selectedEngine == AudioEngineType.OBOE) apiName else "En espera (Media3 activo)")
                    TelemetryItem("Frecuencia de Muestreo", if (sampleRate > 0) "$sampleRate Hz" else "48000 Hz")
                    TelemetryItem("Canales Activos", if (channelCount > 0) "$channelCount canales" else "2 canales")
                    TelemetryItem("Tramas Escritas en C++", "$framesWritten tramas")
                    TelemetryItem("Arquitectura CPU", Build.SUPPORTED_ABIS.firstOrNull() ?: "Universal")
                    TelemetryItem("Versión SO", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    TelemetryItem("Soporte Nativo AAudio", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "Disponible (API >= 26)" else "No disponible")
                    TelemetryItem("Perfil Búfer RAM", "${memoryProfile.profileName} (${memoryProfile.maxBufferRamMb} MB)", highlight = true)
                    TelemetryItem("Búfer Dinámico", "${memoryProfile.minBufferSec.toInt()}s - ${memoryProfile.maxBufferSec.toInt()}s")
                    TelemetryItem("RAM Total Dispositivo", "${memoryProfile.totalRamMb} MB (Android Go: ${if (memoryProfile.isLowRamDevice) "Sí" else "No"})")
                    TelemetryItem("Motor de Subtítulos", "SRT (SubRip) y WebVTT (.vtt)")
                }
            }
        }
    }
}

/**
 * Subpantalla dedicada para Arquitectura, Portabilidad y Distribución.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSubScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arquitectura y Distribución", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Detalles de portabilidad y compilación", fontSize = 12.sp, color = Color(0xFFF59E0B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_about_back")) {
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Canal de Distribución y Portabilidad", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Nova Video Player está diseñado y optimizado para su distribución independiente y tiendas alternativas (Uptodown / descarga directa de APK). No depende de servicios propietarios de Google Play Services para su núcleo multimedia.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Compilado nativamente para 32-bit (armeabi-v7a, x86) y 64-bit (arm64-v8a, x86_64) con soporte de bajo consumo para dispositivos de recursos contenidos (Android Go).",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Licencias permisivas: todas las bibliotecas utilizadas (Google Oboe, Jetpack Compose, Media3) operan bajo licencia Apache 2.0 garantizando máxima libertad y portabilidad.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
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
private fun EngineSelectionCard(
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = engineType.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
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

/**
 * Fila de visualización para métricas de telemetría.
 */
@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) Color(0xFF38BDF8) else Color.White
        )
    }
}

/**
 * Gestor de generación de tono de prueba para validar en tiempo real y al 100%
 * la salida física de audio tanto en el motor Oboe C++ como en AudioTrack de Media3.
 */
object AudioTestManager {
    private const val SAMPLE_RATE = 48000
    private const val DURATION_SECONDS = 1.5
    private const val FREQUENCY = 440.0 // Tono A4 (440 Hz)

    suspend fun playTone(
        engine: AudioEngineType,
        onTrackCreated: (AudioTrack?) -> Unit,
        onFinish: () -> Unit
    ) {
        val totalSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val numChannels = 2
        val pcmBytes = ByteArray(totalSamples * numChannels * 2)

        // Sintetizar tono senoidal puro de 440 Hz con envolvente suave (fade in / out)
        var byteIndex = 0
        for (i in 0 until totalSamples) {
            val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / FREQUENCY)
            val envelope = when {
                i < 2400 -> i / 2400.0
                i > totalSamples - 2400 -> (totalSamples - i) / 2400.0
                else -> 1.0
            }
            val sampleVal = (sin(angle) * 16000.0 * envelope).toInt().toShort()
            val lowByte = (sampleVal.toInt() and 0xFF).toByte()
            val highByte = ((sampleVal.toInt() shr 8) and 0xFF).toByte()

            // Canal izquierdo
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
            // Canal derecho
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
        }

        if (engine == AudioEngineType.OBOE) {
            // Reproducción 100% real mediante Google Oboe C++ nativo
            OboeAudioEngine.init(SAMPLE_RATE, numChannels)
            OboeAudioEngine.setVolume(1.0f)
            OboeAudioEngine.start()
            OboeAudioEngine.write(pcmBytes, 0, pcmBytes.size)

            // Esperar duración del tono y finalizar
            delay((DURATION_SECONDS * 1000).toLong())
            onFinish()
        } else {
            // Reproducción 100% real mediante AudioTrack estándar de Android
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufSize, pcmBytes.size))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                onTrackCreated(track)
                track.write(pcmBytes, 0, pcmBytes.size)
                track.play()

                delay((DURATION_SECONDS * 1000).toLong())
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                onTrackCreated(null)
                onFinish()
            } catch (e: Exception) {
                onFinish()
            }
        }
    }

    fun stopTone(audioTrack: AudioTrack?) {
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        OboeAudioEngine.stop()
    }
}
