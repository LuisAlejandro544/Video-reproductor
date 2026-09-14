package com.example.ui

import android.media.AudioTrack
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import com.example.player.PlayerLoadControlHelper
import com.example.ui.settings.AboutSubScreen
import com.example.ui.settings.AppearanceSubScreen
import com.example.ui.settings.AudioChannelsSubScreen
import com.example.ui.settings.AudioEngineSubScreen
import com.example.ui.settings.AudioTestManager
import com.example.ui.settings.AudioTestSubScreen
import com.example.ui.settings.FormatsSubScreen
import com.example.ui.settings.SettingsHubView
import com.example.ui.settings.SettingsSubScreen
import com.example.ui.settings.TelemetrySubScreen
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsScreen.kt - Coordinador Modular de la Pantalla de Configuración
 *
 * Arquitectura:
 * Implementa un patrón Hub-and-Spoke donde el menú principal (SettingsHubView)
 * dirige la navegación hacia subpantallas modulares desacopladas:
 * - AudioEngineSubScreen: Alternancia Oboe C++ vs Media3
 * - AudioChannelsSubScreen: Enrutamiento estéreo, mono y espacial Haas
 * - AudioTestSubScreen: Validación acústica mediante AudioTestManager
 * - TelemetrySubScreen: Métricas en tiempo real de buffer y hardware
 * - AboutSubScreen: Licencias y detalles de arquitectura 32/64 bits
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentEngine: AudioEngineType,
    onEngineChanged: (AudioEngineType) -> Unit,
    currentThemeMode: AppThemeMode,
    useDynamicColor: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    isSoundEffectsEnabled: Boolean = true,
    onSoundEffectsToggled: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit,
    onPlayClickSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.HUB) }
    var selectedEngine by remember { mutableStateOf(currentEngine) }
    var currentChannelMode by remember { mutableStateOf(OboeAudioEngine.currentChannelMode) }

    // Telemetría en tiempo real obtenida desde el motor nativo
    var apiName by remember { mutableStateOf(OboeAudioEngine.getApiName()) }
    var sampleRate by remember { mutableStateOf(OboeAudioEngine.getSampleRate()) }
    var channelCount by remember { mutableStateOf(OboeAudioEngine.getChannelCount()) }
    var framesWritten by remember { mutableLongStateOf(OboeAudioEngine.getFramesWritten()) }

    // Estado del sintetizador de prueba de sonido
    var isTestingAudio by remember { mutableStateOf(false) }
    var testAudioMessage by remember { mutableStateOf<String?>(null) }
    var activeTestTrack by remember { mutableStateOf<AudioTrack?>(null) }

    val context = LocalContext.current
    val memoryProfile = remember { PlayerLoadControlHelper.getMemoryProfile(context) }
    val coroutineScope = rememberCoroutineScope()

    // Manejo de retroceso ergonómico para móviles
    BackHandler {
        if (activeSubScreen != SettingsSubScreen.HUB) {
            activeSubScreen = SettingsSubScreen.HUB
        } else {
            AudioTestManager.stopTone(activeTestTrack)
            onNavigateBack()
        }
    }

    // Actualización de telemetría nativa periódica
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
                    currentThemeMode = currentThemeMode,
                    useDynamicColor = useDynamicColor,
                    onNavigateTo = {
                        onPlayClickSound()
                        activeSubScreen = it
                    },
                    onNavigateBack = {
                        AudioTestManager.stopTone(activeTestTrack)
                        onNavigateBack()
                    }
                )
            }
            SettingsSubScreen.APPEARANCE -> {
                AppearanceSubScreen(
                    currentThemeMode = currentThemeMode,
                    useDynamicColor = useDynamicColor,
                    onThemeModeSelected = onThemeModeChanged,
                    onDynamicColorToggled = onDynamicColorChanged,
                    isSoundEffectsEnabled = isSoundEffectsEnabled,
                    onSoundEffectsToggled = onSoundEffectsToggled,
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
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
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
            SettingsSubScreen.AUDIO_CHANNELS -> {
                AudioChannelsSubScreen(
                    currentMode = currentChannelMode,
                    onModeSelected = { mode ->
                        onPlayClickSound()
                        currentChannelMode = mode
                        OboeAudioEngine.setChannelMode(mode)
                    },
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
            SettingsSubScreen.AUDIO_TEST -> {
                AudioTestSubScreen(
                    selectedEngine = selectedEngine,
                    isTestingAudio = isTestingAudio,
                    testAudioMessage = testAudioMessage,
                    onToggleTest = {
                        onPlayClickSound()
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
                        onPlayClickSound()
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
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
            SettingsSubScreen.ABOUT -> {
                AboutSubScreen(
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
            SettingsSubScreen.FORMATS -> {
                FormatsSubScreen(
                    onBack = {
                        onPlayClickSound()
                        activeSubScreen = SettingsSubScreen.HUB
                    }
                )
            }
        }
    }
}
