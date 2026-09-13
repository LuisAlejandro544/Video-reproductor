package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.example.audio.AudioChannelMode
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import com.example.audio.OboeAudioProcessor
import com.example.data.VideoEntity
import com.example.model.VideoItem
import com.example.opengl.OpenGLVideoPlayerView
import com.example.opengl.VideoEqualizerState
import com.example.player.PlayerLoadControlHelper
import com.example.subtitles.AssSubtitleOverlay
import com.example.subtitles.SubtitleSize
import com.example.subtitles.SubtitleTrackItem
import com.example.subtitles.SubtitleUtils
import com.example.ui.player.BottomControlsBar
import com.example.ui.player.CenterPlaybackControls
import com.example.ui.player.ControlsUnlockButton
import com.example.ui.player.DoubleTapSeekIndicator
import com.example.ui.player.DoubleTapSeekSide
import com.example.ui.player.FastForward2xBadge
import com.example.ui.player.GestureIndicatorType
import com.example.ui.player.MinimalistGestureIndicator
import com.example.ui.player.PlayerErrorOverlay
import com.example.ui.player.PlayerGestureSurface
import com.example.ui.player.PlayerOrientationHandler
import com.example.ui.player.TopControlsBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla principal de reproducción de video estilo reproductor multimedia de PC.
 *
 * Características principales (Modularizada):
 * - Integración con ExoPlayer (Media3) para máxima compatibilidad de codecs y aceleración por hardware.
 * - Enrutamiento dinámico y en tiempo real de audio hacia Google Oboe C++ o Media3 AudioTrack.
 * - Navegación hacia pantalla independiente de Configuración conservando la posición de reproducción.
 * - Controles táctiles superpuestos con ocultamiento inteligente (PlayerOverlayControls).
 * - Detección de gestos ágil y modular para brillo, volumen, avance 2X y doble toque (PlayerGestureDetector).
 * - Indicadores HUD en tiempo real para saltos de tiempo y niveles (PlayerHudIndicators).
 * - Gestión por sensor físico de rotación de hardware (PlayerOrientationHandler).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoItem: VideoItem,
    initialVideoEntity: VideoEntity? = null,
    currentAudioEngine: AudioEngineType = AudioEngineType.MEDIA3,
    initialPositionMs: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    onPlaybackProgress: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    onSaveVideoSettings: ((VideoEntity) -> Unit)? = null,
    onBackToHome: () -> Unit,
    onChangeVideoSource: () -> Unit,
    onOpenSettings: () -> Unit,
    onAudioEngineChange: ((AudioEngineType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado local reactivo del motor de audio seleccionado (restaura del video o general)
    var activeAudioEngine by remember(initialVideoEntity, currentAudioEngine) {
        val restored = initialVideoEntity?.audioEngine?.let {
            try { AudioEngineType.valueOf(it) } catch (_: Exception) { currentAudioEngine }
        } ?: currentAudioEngine
        mutableStateOf(restored)
    }

    // Procesador de audio que desvía tramas PCM hacia Google Oboe C++ o hacia AudioTrack
    val oboeAudioProcessor = remember {
        OboeAudioProcessor().apply {
            currentEngine = activeAudioEngine
        }
    }

    // Actualización 100% real e inmediata del motor ante cualquier cambio en la configuración
    LaunchedEffect(activeAudioEngine) {
        oboeAudioProcessor.currentEngine = activeAudioEngine
    }

    // Estado de la reproducción
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }

    // Control de visibilidad de la interfaz superpuesta
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val activity = context as? Activity
    val view = LocalView.current

    // Sensor de rotación de pantalla mediante hardware
    PlayerOrientationHandler(context = context, activity = activity)

    // Modo de aspecto: FIT (ajustar), ZOOM (rellenar pantalla completa), FILL (estirar)
    var currentAspectMode by remember(initialVideoEntity) {
        val restored = initialVideoEntity?.aspectRatioMode?.let {
            try { AspectRatioMode.valueOf(it) } catch (_: Exception) { AspectRatioMode.FIT }
        } ?: AspectRatioMode.FIT
        mutableStateOf(restored)
    }

    // Dimensiones nativas de video para cálculo geométrico y kernel de nitidez
    var videoWidth by remember { mutableIntStateOf(1920) }
    var videoHeight by remember { mutableIntStateOf(1080) }

    // Ecualizador de Video en Tiempo Real con Shaders OpenGL ES en C++
    var equalizerState by remember(initialVideoEntity) {
        mutableStateOf(initialVideoEntity?.toEqualizerState() ?: VideoEqualizerState.DEFAULT)
    }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // Control de Velocidad de Reproducción con Sonic Pitch Preservation (hasta 2.0x)
    var playbackSpeed by remember(initialVideoEntity) {
        mutableFloatStateOf(initialVideoEntity?.playbackSpeed ?: 1.0f)
    }
    var showSpeedSheet by remember { mutableStateOf(false) }

    // Panel lateral de herramientas y estado de bloqueo de controles
    var showToolsSideSheet by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }

    // Pantallas exclusivas e independientes para cada herramienta
    var showPillarboxSheet by remember { mutableStateOf(false) }
    var showFsrSheet by remember { mutableStateOf(false) }
    var showAnime4kSheet by remember { mutableStateOf(false) }
    var showSunModeSheet by remember { mutableStateOf(false) }
    var showVoiceNightSheet by remember { mutableStateOf(false) }
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var showAudioEngineSheet by remember { mutableStateOf(false) }
    var showStereoMonoSheet by remember { mutableStateOf(false) }

    // Modo de canal de audio (Estéreo / Mono / Pseudo-Estéreo Haas)
    var audioChannelMode by remember(initialVideoEntity) {
        val restored = initialVideoEntity?.audioChannelMode?.let {
            try { AudioChannelMode.valueOf(it) } catch (_: Exception) { OboeAudioEngine.currentChannelMode }
        } ?: OboeAudioEngine.currentChannelMode
        mutableStateOf(restored)
    }

    // Interacción del usuario arrastrando la barra de progreso
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderScrubbingPosition by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    // Control de Brillo de Pantalla (0.01f a 1.0f)
    var currentBrightness by remember {
        val windowAttr = activity?.window?.attributes
        val currentWinBrightness = windowAttr?.screenBrightness ?: -1f
        val initialVal = if (currentWinBrightness in 0.01f..1.0f) {
            currentWinBrightness
        } else {
            try {
                android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    128
                ) / 255f
            } catch (_: Exception) {
                0.5f
            }
        }
        mutableFloatStateOf(initialVal.coerceIn(0.01f, 1f))
    }

    // Control de Volumen Multimedia del Dispositivo
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember {
        val initialVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        mutableIntStateOf(initialVol)
    }

    // Dimensiones de pantalla para calibrar la sensibilidad del gesto
    var containerWidth by remember { mutableIntStateOf(1) }
    var containerHeight by remember { mutableIntStateOf(1) }

    // Estado del indicador flotante minimalista
    var gestureIndicatorType by remember { mutableStateOf<GestureIndicatorType?>(null) }
    var gestureIndicatorVisible by remember { mutableStateOf(false) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }

    // Estado del modo de avance rápido a 2X (activado al mantener presionado el lateral derecho)
    var isFastForwarding2x by remember { mutableStateOf(false) }
    var wasPlayingBefore2x by remember { mutableStateOf(false) }

    // Control de doble toque (doble click) para adelantar o retroceder 5 segundos (+5s / -5s)
    var doubleTapSeekSide by remember { mutableStateOf<DoubleTapSeekSide?>(null) }
    var doubleTapHideJob by remember { mutableStateOf<Job?>(null) }

    // Estado del Sistema de Subtítulos (SRT / WebVTT / SSA / ASS y pistas embebidas)
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember(initialVideoEntity) {
        mutableStateOf(initialVideoEntity?.subtitlesEnabled ?: true)
    }
    var subtitleSize by remember(initialVideoEntity) {
        val restored = initialVideoEntity?.subtitleSize?.let {
            try { SubtitleSize.valueOf(it) } catch (_: Exception) { SubtitleSize.MEDIUM }
        } ?: SubtitleSize.MEDIUM
        mutableStateOf(restored)
    }
    var externalSubtitle by remember { mutableStateOf<SubtitleTrackItem?>(null) }
    var availableTracks by remember { mutableStateOf<List<SubtitleTrackItem>>(emptyList()) }
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var currentCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    // Función central para persistir las configuraciones específicas de este video
    val saveSettings: () -> Unit = {
        val baseEntity = initialVideoEntity
        if (baseEntity != null) {
            val updated = baseEntity.copy(
                playbackSpeed = playbackSpeed,
                aspectRatioMode = currentAspectMode.name,
                audioEngine = activeAudioEngine.name,
                audioChannelMode = audioChannelMode.name,
                subtitlesEnabled = subtitlesEnabled,
                subtitleSize = subtitleSize.name,
                externalSubtitleUri = externalSubtitle?.uri?.toString(),
                externalSubtitleName = externalSubtitle?.label,
                eqBrightness = equalizerState.brightness,
                eqContrast = equalizerState.contrast,
                eqSaturation = equalizerState.saturation,
                eqGamma = equalizerState.gamma,
                eqSharpness = equalizerState.sharpness,
                eqBlueLightFilter = equalizerState.blueLightFilter,
                eqPillarboxBlur = equalizerState.pillarboxBlur,
                eqFsrEnabled = equalizerState.fsrEnabled,
                eqFsrSharpness = equalizerState.fsrSharpness,
                eqSunMode = equalizerState.sunMode,
                eqAnime4kMode = equalizerState.anime4kMode.id,
                eqAnime4kStrength = equalizerState.anime4kStrength
            )
            onSaveVideoSettings?.invoke(updated)
        }
    }

    // Control de Carga de RAM Adaptativo para Android Go y terminales modestos
    val adaptiveLoadControl = remember {
        try {
            PlayerLoadControlHelper.createAdaptiveLoadControl(context)
        } catch (e: Throwable) {
            Log.e("VideoPlayerScreen", "Error inicializando LoadControl adaptativo: ${e.message}")
            null
        }
    }

    // Instancia de ExoPlayer personalizada con el Sink de Oboe C++ y LoadControl adaptativo
    val exoPlayer = remember(videoItem.uri) {
        val audioSink = try {
            DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(oboeAudioProcessor))
                .build()
        } catch (e: Throwable) {
            Log.e("VideoPlayerScreen", "Error configurando DefaultAudioSink con Oboe: ${e.message}")
            null
        }

        val renderersFactory = object : DefaultRenderersFactory(context) {
            init {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            }

            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return audioSink ?: super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            }
        }

        val playerBuilder = ExoPlayer.Builder(context, renderersFactory)
        if (adaptiveLoadControl != null) {
            playerBuilder.setLoadControl(adaptiveLoadControl)
        }

        playerBuilder.build().apply {
            try {
                val mediaItem = MediaItem.fromUri(videoItem.uri)
                setMediaItem(mediaItem)
                if (initialPositionMs > 0L) {
                    seekTo(initialPositionMs)
                }
                if (playbackSpeed != 1.0f) {
                    playbackParameters = PlaybackParameters(playbackSpeed, 1.0f)
                }
                prepare()
                playWhenReady = true
            } catch (e: Throwable) {
                Log.e("VideoPlayerScreen", "Error preparando ExoPlayer: ${e.message}", e)
                playbackErrorMessage = e.localizedMessage ?: "Error al preparar el video"
            }
        }
    }

    // Restaurar subtítulo externo persistido si existía guardado para este video
    LaunchedEffect(initialVideoEntity?.externalSubtitleUri) {
        val subUriStr = initialVideoEntity?.externalSubtitleUri
        if (!subUriStr.isNullOrBlank() && externalSubtitle == null) {
            try {
                val subUri = Uri.parse(subUriStr)
                val fileName = initialVideoEntity.externalSubtitleName ?: SubtitleUtils.resolveSubtitleFileName(context, subUri)
                val mimeType = SubtitleUtils.detectSubtitleMimeType(fileName)
                val fullAss = SubtitleUtils.loadFullAssTrackIfApplicable(context, subUri, fileName)
                val track = SubtitleTrackItem(
                    id = "ext_persisted_${System.currentTimeMillis()}",
                    label = fileName,
                    mimeType = mimeType,
                    isExternal = true,
                    uri = subUri,
                    assInfo = fullAss?.summary ?: SubtitleUtils.inspectAssMetadataIfApplicable(context, subUri, fileName),
                    assStyles = fullAss?.styles ?: emptyList(),
                    assDialogues = fullAss?.dialogues ?: emptyList()
                )
                externalSubtitle = track
                selectedTrackId = track.id

                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mimeType)
                    .setLanguage("und")
                    .setLabel(fileName)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()

                val currentMediaItem = exoPlayer.currentMediaItem
                if (currentMediaItem != null) {
                    val currentPos = exoPlayer.currentPosition
                    val wasPlaying = exoPlayer.playWhenReady
                    val updatedItem = currentMediaItem.buildUpon()
                        .setSubtitleConfigurations(listOf(subtitleConfig))
                        .build()
                    exoPlayer.setMediaItem(updatedItem, currentPos)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = wasPlaying
                }
            } catch (e: Throwable) {
                Log.w("VideoPlayerScreen", "No se pudo restaurar subtítulo externo persistido: ${e.message}")
            }
        }
    }

    // Mantener la pantalla encendida y ocultar de forma inmersiva barras del sistema
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            saveSettings()
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val lp = window?.attributes
            if (lp != null) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            try {
                val currentPos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                onPositionChanged(currentPos)
                onPlaybackProgress?.invoke(currentPos, dur)
            } catch (_: Throwable) {}
            try {
                exoPlayer.stop()
                exoPlayer.release()
            } catch (e: Throwable) {
                Log.e("VideoPlayerScreen", "Error liberando ExoPlayer: ${e.message}")
            }
            try {
                OboeAudioEngine.stop()
            } catch (e: Throwable) {
                Log.e("VideoPlayerScreen", "Error deteniendo OboeAudioEngine: ${e.message}")
            }
        }
    }

    // Sincronizar ciclo de vida de la actividad
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        val currentPos = exoPlayer.currentPosition
                        val dur = exoPlayer.duration
                        onPositionChanged(currentPos)
                        onPlaybackProgress?.invoke(currentPos, dur)
                        exoPlayer.pause()
                        OboeAudioEngine.pause()
                    } catch (e: Throwable) {
                        Log.w("VideoPlayerScreen", "Error pausando en ciclo de vida: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        val insetsCtrl = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
                        insetsCtrl?.hide(WindowInsetsCompat.Type.systemBars())

                        if (isPlaying) {
                            exoPlayer.play()
                            if (currentAudioEngine == AudioEngineType.OBOE) {
                                OboeAudioEngine.start()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w("VideoPlayerScreen", "Error reanudando en ciclo de vida: ${e.message}")
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Escuchador de eventos de ExoPlayer
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    playbackErrorMessage = null
                    isPlaying = exoPlayer.playWhenReady
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    try {
                        val currentPos = exoPlayer.currentPosition
                        val dur = exoPlayer.duration
                        onPositionChanged(currentPos)
                        onPlaybackProgress?.invoke(currentPos, dur)
                    } catch (_: Throwable) {}
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (!isBuffering && exoPlayer.playbackState != Player.STATE_ENDED) {
                    isPlaying = playing
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    if (videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270) {
                        videoWidth = videoSize.height
                        videoHeight = videoSize.width
                    } else {
                        videoWidth = videoSize.width
                        videoHeight = videoSize.height
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("VideoPlayerScreen", "Error de reproducción ExoPlayer: ${error.message}", error)
                isBuffering = false
                isPlaying = false
                val rawMsg = error.localizedMessage ?: ""
                playbackErrorMessage = if (rawMsg.contains("Source error", ignoreCase = true) ||
                    error.errorCodeName.contains("SOURCE", ignoreCase = true) ||
                    rawMsg.contains("Permission", ignoreCase = true)
                ) {
                    "El permiso temporal del sistema sobre este video ha expirado. Selecciona el archivo nuevamente desde la Galería para guardarlo permanentemente."
                } else {
                    rawMsg.ifBlank { "No se pudo reproducir el video." }
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                currentCues = cueGroup.cues
            }

            override fun onTracksChanged(tracks: Tracks) {
                val detected = mutableListOf<SubtitleTrackItem>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val label = when {
                                !format.label.isNullOrBlank() -> format.label!!
                                !format.language.isNullOrBlank() -> "Idioma: ${format.language}"
                                else -> "Pista de subtítulos #${detected.size + 1}"
                            }
                            val item = SubtitleTrackItem(
                                id = format.id ?: "text_track_${group.mediaTrackGroup.id}_$i",
                                label = label,
                                language = format.language,
                                mimeType = format.sampleMimeType ?: MimeTypes.APPLICATION_SUBRIP,
                                isExternal = false,
                                trackIndex = i
                            )
                            detected.add(item)
                            if (isSelected && selectedTrackId == null) {
                                selectedTrackId = item.id
                            }
                        }
                    }
                }
                availableTracks = detected
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Bucle para actualizar la barra de progreso periódicamente y persistir en Room con throttle
    LaunchedEffect(isPlaying, isDraggingSlider) {
        var lastPersistTimeMs = System.currentTimeMillis()
        while (true) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)

                val now = System.currentTimeMillis()
                if (isPlaying && now - lastPersistTimeMs >= 5000L) {
                    lastPersistTimeMs = now
                    onPlaybackProgress?.invoke(currentPositionMs, totalDurationMs)
                }
            }
            delay(500)
        }
    }

    // Ocultar controles automáticamente después de 4 segundos sin toques
    LaunchedEffect(showControls, lastInteractionTime, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Manejar botón Atrás del sistema de forma jerárquica
    BackHandler {
        when {
            showToolsSideSheet -> { showToolsSideSheet = false; saveSettings() }
            showEqualizerSheet -> { showEqualizerSheet = false; saveSettings() }
            showPillarboxSheet -> { showPillarboxSheet = false; saveSettings() }
            showFsrSheet -> { showFsrSheet = false; saveSettings() }
            showAnime4kSheet -> { showAnime4kSheet = false; saveSettings() }
            showSunModeSheet -> { showSunModeSheet = false; saveSettings() }
            showVoiceNightSheet -> { showVoiceNightSheet = false; saveSettings() }
            showSpeedSheet -> { showSpeedSheet = false; saveSettings() }
            showSubtitlesSheet -> { showSubtitlesSheet = false; saveSettings() }
            showAspectRatioSheet -> { showAspectRatioSheet = false; saveSettings() }
            showAudioEngineSheet -> { showAudioEngineSheet = false; saveSettings() }
            showStereoMonoSheet -> { showStereoMonoSheet = false; saveSettings() }
            isControlsLocked -> isControlsLocked = false
            else -> {
                saveSettings()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                onBackToHome()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                containerWidth = size.width
                containerHeight = size.height
            }
    ) {
        // Superficie de Video Acelerada por GPU con Shaders OpenGL ES en C++
        OpenGLVideoPlayerView(
            player = exoPlayer,
            equalizerState = equalizerState,
            aspectRatioMode = currentAspectMode,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            modifier = Modifier.fillMaxSize()
        )

        // Capa de Subtítulos Complejos SSA/ASS (Procesados por Rust Core nativo)
        if (subtitlesEnabled && externalSubtitle?.assDialogues?.isNotEmpty() == true) {
            AssSubtitleOverlay(
                dialogues = externalSubtitle!!.assDialogues,
                styles = externalSubtitle!!.assStyles,
                currentPositionMs = currentPositionMs,
                subtitleSize = subtitleSize,
                showControls = showControls,
                modifier = Modifier.fillMaxSize()
            )
        } else if (subtitlesEnabled && currentCues.isNotEmpty()) {
            // Capa de Subtítulos estándar (SRT / WebVTT y pistas embebidas)
            AndroidView(
                factory = { ctx ->
                    SubtitleView(ctx).apply {
                        setUserDefaultStyle()
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                        )
                        setFractionalTextSize(subtitleSize.fraction)
                    }
                },
                update = { view ->
                    view.setFractionalTextSize(subtitleSize.fraction)
                    view.setCues(currentCues)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = if (showControls) 96.dp else 28.dp
                    )
            )
        }

        // Capa interactiva de gestos táctiles
        PlayerGestureSurface(
            isControlsLocked = isControlsLocked,
            playbackSpeed = playbackSpeed,
            view = view,
            activity = activity,
            audioManager = audioManager,
            maxVolume = maxVolume,
            currentVolume = currentVolume,
            onVolumeChange = { currentVolume = it },
            currentBrightness = currentBrightness,
            onBrightnessChange = { currentBrightness = it },
            onSingleTap = {
                showControls = !showControls
                lastInteractionTime = System.currentTimeMillis()
            },
            onDoubleTapSeek = { isLeft ->
                val deltaMs = 5000L
                if (isLeft) {
                    val newPos = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPos)
                    currentPositionMs = newPos
                    doubleTapSeekSide = DoubleTapSeekSide.LEFT
                } else {
                    val maxPos = if (totalDurationMs > 0) totalDurationMs else exoPlayer.duration
                    val newPos = (exoPlayer.currentPosition + deltaMs).coerceAtMost(maxPos)
                    exoPlayer.seekTo(newPos)
                    currentPositionMs = newPos
                    doubleTapSeekSide = DoubleTapSeekSide.RIGHT
                }
                doubleTapHideJob?.cancel()
                doubleTapHideJob = coroutineScope.launch {
                    delay(600)
                    doubleTapSeekSide = null
                }
            },
            onStartFastForward2x = {
                isFastForwarding2x = true
                wasPlayingBefore2x = exoPlayer.playWhenReady
                exoPlayer.playbackParameters = PlaybackParameters(2.0f, 1.0f)
                if (!wasPlayingBefore2x) {
                    exoPlayer.play()
                }
            },
            onStopFastForward2x = {
                isFastForwarding2x = false
                exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed, 1.0f)
                if (!wasPlayingBefore2x) {
                    exoPlayer.pause()
                }
            },
            onShowGestureIndicator = { type ->
                gestureIndicatorType = type
                gestureIndicatorVisible = true
            },
            onHideGestureIndicator = {
                gestureIndicatorVisible = false
            }
        )

        // Botón flotante para desbloquear la pantalla
        if (isControlsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp),
                contentAlignment = Alignment.TopStart
            ) {
                ControlsUnlockButton(onUnlock = { isControlsLocked = false })
            }
        }

        // Indicador de carga / búfer
        if (isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }

        // Indicador flotante minimalista para brillo o volumen
        AnimatedVisibility(
            visible = gestureIndicatorVisible && gestureIndicatorType != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.88f),
            exit = fadeOut(tween(350)),
            modifier = Modifier
                .align(
                    if (gestureIndicatorType == GestureIndicatorType.BRIGHTNESS) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                )
                .padding(horizontal = 32.dp)
        ) {
            gestureIndicatorType?.let { type ->
                val fraction = if (type == GestureIndicatorType.BRIGHTNESS) {
                    currentBrightness
                } else {
                    if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0.5f
                }
                MinimalistGestureIndicator(type = type, fraction = fraction)
            }
        }

        // Indicador HUD para Avance Rápido a 2X
        AnimatedVisibility(
            visible = isFastForwarding2x,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.88f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp)
        ) {
            FastForward2xBadge()
        }

        // Indicador HUD para Doble Tap (+5s / -5s)
        AnimatedVisibility(
            visible = doubleTapSeekSide != null,
            enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.82f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(
                    if (doubleTapSeekSide == DoubleTapSeekSide.LEFT) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                )
                .padding(horizontal = 48.dp)
        ) {
            doubleTapSeekSide?.let { side ->
                DoubleTapSeekIndicator(side = side)
            }
        }

        // Interfaz superpuesta de controles
        AnimatedVisibility(
            visible = showControls && !isControlsLocked,
            enter = fadeIn(animationSpec = tween(140)),
            exit = fadeOut(animationSpec = tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showControls = false
                        lastInteractionTime = System.currentTimeMillis()
                    }
            ) {
                val isPortrait = containerHeight > containerWidth || (containerWidth in 1..600)

                TopControlsBar(
                    title = videoItem.name,
                    size = videoItem.formattedSize,
                    aspectModeLabel = currentAspectMode.label,
                    selectedAudioEngine = activeAudioEngine,
                    isPortrait = isPortrait,
                    onBack = {
                        saveSettings()
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onBackToHome()
                    },
                    onChangeSource = {
                        saveSettings()
                        onChangeVideoSource()
                    },
                    onToggleAspectMode = {
                        currentAspectMode = when (currentAspectMode) {
                            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
                            AspectRatioMode.ZOOM -> AspectRatioMode.FILL
                            AspectRatioMode.FILL -> AspectRatioMode.FIT
                        }
                        saveSettings()
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenSettings = {
                        saveSettings()
                        onPositionChanged(exoPlayer.currentPosition)
                        onOpenSettings()
                    },
                    onOpenTools = {
                        showToolsSideSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                CenterPlaybackControls(
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        val willPlay = !exoPlayer.playWhenReady || exoPlayer.playbackState == Player.STATE_ENDED
                        if (willPlay) {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                            if (activeAudioEngine == AudioEngineType.OBOE) {
                                OboeAudioEngine.start()
                            }
                            isPlaying = true
                        } else {
                            exoPlayer.pause()
                            OboeAudioEngine.pause()
                            isPlaying = false
                            try {
                                val currentPos = exoPlayer.currentPosition
                                val dur = exoPlayer.duration
                                onPositionChanged(currentPos)
                                onPlaybackProgress?.invoke(currentPos, dur)
                            } catch (_: Throwable) {}
                        }
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onRewind10 = {
                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                        exoPlayer.seekTo(newPos)
                        currentPositionMs = newPos
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onForward10 = {
                        val maxPos = if (totalDurationMs > 0) totalDurationMs else exoPlayer.duration
                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(maxPos)
                        exoPlayer.seekTo(newPos)
                        currentPositionMs = newPos
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                BottomControlsBar(
                    currentPositionMs = if (isDraggingSlider) {
                        (sliderScrubbingPosition * totalDurationMs).toLong()
                    } else {
                        currentPositionMs
                    },
                    totalDurationMs = totalDurationMs,
                    isMuted = isMuted,
                    playbackSpeed = playbackSpeed,
                    hasActiveEqualizer = !equalizerState.isDefault,
                    hasActiveSubtitles = subtitlesEnabled && (externalSubtitle != null || selectedTrackId != null || currentCues.isNotEmpty()),
                    onOpenSpeedSelector = {
                        showSpeedSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenEqualizer = {
                        showEqualizerSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenSubtitles = {
                        showSubtitlesSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenTools = {
                        showToolsSideSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onSeekStarted = {
                        isDraggingSlider = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onSeekChanged = { progressFraction ->
                        sliderScrubbingPosition = progressFraction
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onSeekFinished = {
                        val targetMs = (sliderScrubbingPosition * totalDurationMs).toLong()
                        exoPlayer.seekTo(targetMs)
                        currentPositionMs = targetMs
                        onPositionChanged(targetMs)
                        isDraggingSlider = false
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onToggleMute = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                        OboeAudioEngine.setVolume(if (isMuted) 0f else 1f)
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Panel Lateral de Herramientas
        PlayerToolsSideSheet(
            visible = showToolsSideSheet,
            currentAudioEngine = activeAudioEngine,
            onDismiss = { showToolsSideSheet = false },
            onSelectTool = { tool ->
                when (tool) {
                    PlayerToolItem.LOCK -> {
                        isControlsLocked = true
                        showControls = false
                    }
                    PlayerToolItem.PLAYBACK_SPEED -> showSpeedSheet = true
                    PlayerToolItem.VIDEO_EQUALIZER -> showEqualizerSheet = true
                    PlayerToolItem.SUN_MODE -> showSunModeSheet = true
                    PlayerToolItem.PILLARBOX_BLUR -> showPillarboxSheet = true
                    PlayerToolItem.FSR_SUPER_RESOLUTION -> showFsrSheet = true
                    PlayerToolItem.ANIME4K -> showAnime4kSheet = true
                    PlayerToolItem.VOICE_NIGHT_AUDIO -> showVoiceNightSheet = true
                    PlayerToolItem.STEREO_MONO -> showStereoMonoSheet = true
                    PlayerToolItem.SUBTITLES -> showSubtitlesSheet = true
                    PlayerToolItem.ASPECT_RATIO -> showAspectRatioSheet = true
                    PlayerToolItem.AUDIO_ENGINE -> showAudioEngineSheet = true
                }
            }
        )

        // Hojas modales para herramientas
        if (showStereoMonoSheet) {
            StereoMonoSheet(
                currentMode = audioChannelMode,
                onModeSelected = { mode ->
                    audioChannelMode = mode
                    oboeAudioProcessor.channelMode = mode
                    OboeAudioEngine.setChannelMode(mode)
                    saveSettings()
                },
                onDismiss = {
                    showStereoMonoSheet = false
                    saveSettings()
                }
            )
        }

        if (showEqualizerSheet) {
            VideoEqualizerSheet(
                state = equalizerState,
                onStateChange = {
                    equalizerState = it
                    saveSettings()
                },
                onDismiss = {
                    showEqualizerSheet = false
                    saveSettings()
                }
            )
        }

        if (showSunModeSheet) {
            SunModeSheet(
                state = equalizerState,
                onStateChange = {
                    equalizerState = it
                    saveSettings()
                },
                onDismiss = {
                    showSunModeSheet = false
                    saveSettings()
                }
            )
        }

        if (showPillarboxSheet) {
            PillarboxBlurSheet(
                state = equalizerState,
                onStateChange = {
                    equalizerState = it
                    saveSettings()
                },
                onDismiss = {
                    showPillarboxSheet = false
                    saveSettings()
                }
            )
        }

        if (showFsrSheet) {
            FsrUpscaleSheet(
                state = equalizerState,
                onStateChange = {
                    equalizerState = it
                    saveSettings()
                },
                onDismiss = {
                    showFsrSheet = false
                    saveSettings()
                }
            )
        }

        if (showAnime4kSheet) {
            Anime4KSheet(
                state = equalizerState,
                onStateChange = {
                    equalizerState = it
                    saveSettings()
                },
                onDismiss = {
                    showAnime4kSheet = false
                    saveSettings()
                }
            )
        }

        if (showVoiceNightSheet) {
            VoiceNightAudioSheet(
                currentAudioEngine = activeAudioEngine,
                onSwitchToOboe = {
                    activeAudioEngine = AudioEngineType.OBOE
                    oboeAudioProcessor.currentEngine = AudioEngineType.OBOE
                    onAudioEngineChange?.invoke(AudioEngineType.OBOE)
                    saveSettings()
                },
                onDismiss = {
                    showVoiceNightSheet = false
                    saveSettings()
                }
            )
        }

        if (showSpeedSheet) {
            PlaybackSpeedSheet(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { newSpeed ->
                    playbackSpeed = newSpeed
                    exoPlayer.playbackParameters = PlaybackParameters(newSpeed, 1.0f)
                    saveSettings()
                },
                onDismiss = {
                    showSpeedSheet = false
                    saveSettings()
                }
            )
        }

        if (showAspectRatioSheet) {
            AspectRatioSheet(
                currentMode = currentAspectMode,
                onModeSelected = {
                    currentAspectMode = it
                    saveSettings()
                },
                onDismiss = {
                    showAspectRatioSheet = false
                    saveSettings()
                }
            )
        }

        if (showAudioEngineSheet) {
            AudioEngineSheet(
                currentEngine = activeAudioEngine,
                onEngineSelected = { engine ->
                    activeAudioEngine = engine
                    oboeAudioProcessor.currentEngine = engine
                    onAudioEngineChange?.invoke(engine)
                    saveSettings()
                },
                onDismiss = {
                    showAudioEngineSheet = false
                    saveSettings()
                }
            )
        }

        if (showSubtitlesSheet) {
            SubtitlesBottomSheet(
                subtitlesEnabled = subtitlesEnabled,
                availableTracks = availableTracks,
                selectedTrackId = selectedTrackId,
                externalSubtitle = externalSubtitle,
                selectedSize = subtitleSize,
                onToggleSubtitles = { enabled ->
                    subtitlesEnabled = enabled
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                        .build()
                },
                onSelectTrack = { track ->
                    selectedTrackId = track.id
                    subtitlesEnabled = true
                    if (!track.isExternal) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setPreferredTextLanguage(track.language)
                            .build()
                    }
                },
                onPickExternalSubtitle = { uri ->
                    val fileName = SubtitleUtils.resolveSubtitleFileName(context, uri)
                    val mimeType = SubtitleUtils.detectSubtitleMimeType(fileName)
                    val fullAss = SubtitleUtils.loadFullAssTrackIfApplicable(context, uri, fileName)
                    val assInfo = fullAss?.summary ?: SubtitleUtils.inspectAssMetadataIfApplicable(context, uri, fileName)
                    val track = SubtitleTrackItem(
                        id = "ext_${System.currentTimeMillis()}",
                        label = fileName,
                        mimeType = mimeType,
                        isExternal = true,
                        uri = uri,
                        assInfo = assInfo,
                        assStyles = fullAss?.styles ?: emptyList(),
                        assDialogues = fullAss?.dialogues ?: emptyList()
                    )
                    externalSubtitle = track
                    selectedTrackId = track.id
                    subtitlesEnabled = true

                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                        .setMimeType(mimeType)
                        .setLanguage("und")
                        .setLabel(fileName)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()

                    val currentMediaItem = exoPlayer.currentMediaItem
                    if (currentMediaItem != null) {
                        val currentPos = exoPlayer.currentPosition
                        val wasPlaying = exoPlayer.playWhenReady
                        val updatedItem = currentMediaItem.buildUpon()
                            .setSubtitleConfigurations(listOf(subtitleConfig))
                            .build()
                        exoPlayer.setMediaItem(updatedItem, currentPos)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = wasPlaying
                    }

                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()

                    saveSettings()
                },
                onRemoveExternalSubtitle = {
                    externalSubtitle = null
                    val currentMediaItem = exoPlayer.currentMediaItem
                    if (currentMediaItem != null) {
                        val currentPos = exoPlayer.currentPosition
                        val wasPlaying = exoPlayer.playWhenReady
                        val updatedItem = currentMediaItem.buildUpon()
                            .setSubtitleConfigurations(emptyList())
                            .build()
                        exoPlayer.setMediaItem(updatedItem, currentPos)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = wasPlaying
                    }
                    saveSettings()
                },
                onSizeChanged = { newSize ->
                    subtitleSize = newSize
                    saveSettings()
                },
                onDismiss = {
                    showSubtitlesSheet = false
                    saveSettings()
                }
            )
        }

        // Mensaje de Error Amigable
        playbackErrorMessage?.let { errorMsg ->
            PlayerErrorOverlay(
                errorMessage = errorMsg,
                onBackToHome = onBackToHome,
                onChangeVideoSource = onChangeVideoSource,
                onRetry = {
                    playbackErrorMessage = null
                    try {
                        exoPlayer.seekTo(0)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (e: Throwable) {
                        playbackErrorMessage = e.localizedMessage ?: "Reintento fallido"
                    }
                }
            )
        }
    }
}
