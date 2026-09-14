package com.example.ui.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.audio.AudioChannelMode
import com.example.audio.AudioEngineType
import com.example.model.GraphicsEngineType
import com.example.opengl.VideoEqualizerState
import com.example.subtitles.SubtitleSize
import com.example.subtitles.SubtitleTrackItem
import com.example.ui.Anime4KSheet
import com.example.ui.AspectRatioMode
import com.example.ui.AspectRatioSheet
import com.example.ui.AudioEngineSheet
import com.example.ui.FsrUpscaleSheet
import com.example.ui.GraphicsEngineSheet
import com.example.ui.PillarboxBlurSheet
import com.example.ui.PlaybackSpeedSheet
import com.example.ui.PlayerToolItem
import com.example.ui.PlayerToolsSideSheet
import com.example.ui.StereoMonoSheet
import com.example.ui.SubtitlesBottomSheet
import com.example.ui.SunModeSheet
import com.example.ui.VideoEqualizerSheet
import com.example.ui.VoiceNightAudioSheet
import com.example.ui.ZoomBottomSheet

/**
 * PlayerSheetHost.kt - Contenedor modular de Hojas Modales y Menús Laterales
 *
 * Responsabilidad:
 * Centraliza la invocación y renderizado de todas las interfaces contextuales y hojas modales
 * del reproductor (herramientas, ecualizador, modo sol, pillarbox, FSR, Anime4K, velocidad,
 * relación de aspecto, motores de audio/gráficos, subtítulos y zoom táctil).
 *
 * Desacopla más de 300 líneas de la pantalla principal VideoPlayerScreen.kt.
 */
@Composable
fun PlayerSheetHost(
    // Panel de herramientas principal
    showToolsSideSheet: Boolean,
    onDismissToolsSideSheet: () -> Unit,
    onSelectTool: (PlayerToolItem) -> Unit,

    // Motor de audio
    activeAudioEngine: AudioEngineType,
    showAudioEngineSheet: Boolean,
    onAudioEngineSelected: (AudioEngineType) -> Unit,
    onDismissAudioEngineSheet: () -> Unit,

    // Estéreo / Mono
    showStereoMonoSheet: Boolean,
    audioChannelMode: AudioChannelMode,
    onAudioChannelModeSelected: (AudioChannelMode) -> Unit,
    onDismissStereoMonoSheet: () -> Unit,

    // Ecualizador de video
    showEqualizerSheet: Boolean,
    equalizerState: VideoEqualizerState,
    onEqualizerStateChange: (VideoEqualizerState) -> Unit,
    onDismissEqualizerSheet: () -> Unit,

    // Modo Sol Extremo
    showSunModeSheet: Boolean,
    onDismissSunModeSheet: () -> Unit,

    // Pillarbox Blur
    showPillarboxSheet: Boolean,
    onDismissPillarboxSheet: () -> Unit,

    // FSR Super Resolution
    showFsrSheet: Boolean,
    onDismissFsrSheet: () -> Unit,

    // Anime4K Upscaling
    showAnime4kSheet: Boolean,
    onDismissAnime4kSheet: () -> Unit,

    // Voz Clara y Modo Noche
    showVoiceNightSheet: Boolean,
    onSwitchToOboeForVoiceNight: () -> Unit,
    onDismissVoiceNightSheet: () -> Unit,

    // Velocidad de reproducción
    showSpeedSheet: Boolean,
    playbackSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismissSpeedSheet: () -> Unit,

    // Relación de Aspecto
    showAspectRatioSheet: Boolean,
    currentAspectMode: AspectRatioMode,
    onAspectRatioSelected: (AspectRatioMode) -> Unit,
    onDismissAspectRatioSheet: () -> Unit,

    // Motor Gráfico (OpenGL ES vs Vulkan)
    showGraphicsEngineSheet: Boolean,
    currentGraphicsEngine: GraphicsEngineType,
    onGraphicsEngineSelected: (GraphicsEngineType) -> Unit,
    onDismissGraphicsEngineSheet: () -> Unit,

    // Subtítulos
    showSubtitlesSheet: Boolean,
    subtitlesEnabled: Boolean,
    availableTracks: List<SubtitleTrackItem>,
    selectedTrackId: String?,
    externalSubtitle: SubtitleTrackItem?,
    subtitleSize: SubtitleSize,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectSubtitleTrack: (SubtitleTrackItem) -> Unit,
    onPickExternalSubtitle: (Uri) -> Unit,
    onRemoveExternalSubtitle: () -> Unit,
    onSubtitleSizeChanged: (SubtitleSize) -> Unit,
    onDismissSubtitlesSheet: () -> Unit,

    // Zoom táctil
    showZoomSheet: Boolean,
    zoomScale: Float,
    onZoomSelected: (Float) -> Unit,
    onResetZoom: () -> Unit,
    onDismissZoomSheet: () -> Unit,

    modifier: Modifier = Modifier
) {
    // Panel Lateral de Herramientas
    PlayerToolsSideSheet(
        visible = showToolsSideSheet,
        currentAudioEngine = activeAudioEngine,
        onDismiss = onDismissToolsSideSheet,
        onSelectTool = onSelectTool
    )

    // Selector Stereo / Mono
    if (showStereoMonoSheet) {
        StereoMonoSheet(
            currentMode = audioChannelMode,
            onModeSelected = onAudioChannelModeSelected,
            onDismiss = onDismissStereoMonoSheet
        )
    }

    // Ecualizador de video
    if (showEqualizerSheet) {
        VideoEqualizerSheet(
            state = equalizerState,
            onStateChange = onEqualizerStateChange,
            onDismiss = onDismissEqualizerSheet
        )
    }

    // Modo Sol
    if (showSunModeSheet) {
        SunModeSheet(
            state = equalizerState,
            onStateChange = onEqualizerStateChange,
            onDismiss = onDismissSunModeSheet
        )
    }

    // Pillarbox Blur
    if (showPillarboxSheet) {
        PillarboxBlurSheet(
            state = equalizerState,
            onStateChange = onEqualizerStateChange,
            onDismiss = onDismissPillarboxSheet
        )
    }

    // FSR Super Resolution
    if (showFsrSheet) {
        FsrUpscaleSheet(
            state = equalizerState,
            onStateChange = onEqualizerStateChange,
            onDismiss = onDismissFsrSheet
        )
    }

    // Anime4K Upscaling
    if (showAnime4kSheet) {
        Anime4KSheet(
            state = equalizerState,
            onStateChange = onEqualizerStateChange,
            onDismiss = onDismissAnime4kSheet
        )
    }

    // Voz y Modo Noche
    if (showVoiceNightSheet) {
        VoiceNightAudioSheet(
            currentAudioEngine = activeAudioEngine,
            onSwitchToOboe = onSwitchToOboeForVoiceNight,
            onDismiss = onDismissVoiceNightSheet
        )
    }

    // Velocidad de reproducción
    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            currentSpeed = playbackSpeed,
            onSpeedSelected = onSpeedSelected,
            onDismiss = onDismissSpeedSheet
        )
    }

    // Relación de aspecto
    if (showAspectRatioSheet) {
        AspectRatioSheet(
            currentMode = currentAspectMode,
            onModeSelected = onAspectRatioSelected,
            onDismiss = onDismissAspectRatioSheet
        )
    }

    // Motor de Audio
    if (showAudioEngineSheet) {
        AudioEngineSheet(
            currentEngine = activeAudioEngine,
            onEngineSelected = onAudioEngineSelected,
            onDismiss = onDismissAudioEngineSheet
        )
    }

    // Motor Gráfico
    if (showGraphicsEngineSheet) {
        GraphicsEngineSheet(
            currentEngine = currentGraphicsEngine,
            onEngineSelected = onGraphicsEngineSelected,
            onDismiss = onDismissGraphicsEngineSheet
        )
    }

    // Subtítulos
    if (showSubtitlesSheet) {
        SubtitlesBottomSheet(
            subtitlesEnabled = subtitlesEnabled,
            availableTracks = availableTracks,
            selectedTrackId = selectedTrackId,
            externalSubtitle = externalSubtitle,
            selectedSize = subtitleSize,
            onToggleSubtitles = onToggleSubtitles,
            onSelectTrack = onSelectSubtitleTrack,
            onPickExternalSubtitle = onPickExternalSubtitle,
            onRemoveExternalSubtitle = onRemoveExternalSubtitle,
            onSizeChanged = onSubtitleSizeChanged,
            onDismiss = onDismissSubtitlesSheet
        )
    }

    // Zoom
    if (showZoomSheet) {
        ZoomBottomSheet(
            currentZoom = zoomScale,
            onZoomSelected = onZoomSelected,
            onResetZoom = onResetZoom,
            onDismiss = onDismissZoomSheet
        )
    }
}
