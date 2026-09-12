package com.example.ui

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import com.example.audio.OboeAudioProcessor
import com.example.model.VideoItem
import com.example.opengl.OpenGLVideoPlayerView
import com.example.opengl.VideoEqualizerState
import com.example.utils.VideoUtils
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Pantalla principal de reproducción de video estilo reproductor multimedia de PC.
 *
 * Características principales:
 * - Integración con ExoPlayer (Media3) para máxima compatibilidad de codecs y aceleración por hardware.
 * - Enrutamiento dinámico y en tiempo real de audio hacia Google Oboe C++ o Media3 AudioTrack.
 * - Navegación hacia pantalla independiente de Configuración conservando la posición de reproducción.
 * - Controles táctiles superpuestos con ocultamiento automático inteligente tras inactividad.
 * - Barra superior con título del archivo, tamaño y botón para cambiar de fuente (Galería / Gestor de archivos).
 * - Modos de relación de aspecto (Ajustar, Llenar, Zoom).
 * - Salto de 10 segundos hacia adelante y atrás.
 * - Barra de progreso interactiva (Seekbar) con tiempo transcurrido y duración total.
 * - Mantenimiento de pantalla encendida (FLAG_KEEP_SCREEN_ON) durante la sesión de visualización.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoItem: VideoItem,
    currentAudioEngine: AudioEngineType = AudioEngineType.OBOE,
    initialPositionMs: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    onPlaybackProgress: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    onBackToHome: () -> Unit,
    onChangeVideoSource: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Procesador de audio que desvía tramas PCM hacia Google Oboe C++ o hacia AudioTrack
    val oboeAudioProcessor = remember {
        OboeAudioProcessor().apply {
            currentEngine = currentAudioEngine
        }
    }

    // Actualización 100% real e inmediata del motor ante cualquier cambio en la configuración
    LaunchedEffect(currentAudioEngine) {
        oboeAudioProcessor.currentEngine = currentAudioEngine
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

    // Modo de aspecto: FIT (ajustar), ZOOM (rellenar pantalla completa), FILL (estirar)
    var currentAspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }

    // Dimensiones nativas de video para cálculo geométrico y kernel de nitidez
    var videoWidth by remember { mutableIntStateOf(1920) }
    var videoHeight by remember { mutableIntStateOf(1080) }

    // Ecualizador de Video en Tiempo Real con Shaders OpenGL ES en C++
    var equalizerState by remember { mutableStateOf(VideoEqualizerState.DEFAULT) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // Control de Velocidad de Reproducción con Sonic Pitch Preservation (hasta 2.0x)
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    // Interacción del usuario arrastrando la barra de progreso
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderScrubbingPosition by remember { mutableFloatStateOf(0f) }

    val activity = context as? Activity
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
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember {
        val initialVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        mutableIntStateOf(initialVol)
    }
    var volumeFraction by remember {
        val initialFrac = if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0.5f
        mutableFloatStateOf(initialFrac.coerceIn(0f, 1f))
    }

    // Dimensiones de pantalla para calibrar la sensibilidad del gesto
    var containerWidth by remember { mutableIntStateOf(1) }
    var containerHeight by remember { mutableIntStateOf(1) }

    // Estado del indicador flotante minimalista
    var gestureIndicatorType by remember { mutableStateOf<GestureIndicatorType?>(null) }
    var gestureIndicatorVisible by remember { mutableStateOf(false) }
    var gestureHideJob by remember { mutableStateOf<Job?>(null) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }

    // Instancia de ExoPlayer personalizada con el Sink de Oboe C++
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
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return audioSink ?: super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            }
        }

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            try {
                val mediaItem = MediaItem.fromUri(videoItem.uri)
                setMediaItem(mediaItem)
                if (initialPositionMs > 0L) {
                    seekTo(initialPositionMs)
                }
                prepare()
                playWhenReady = true
            } catch (e: Throwable) {
                Log.e("VideoPlayerScreen", "Error preparando ExoPlayer: ${e.message}", e)
                playbackErrorMessage = e.localizedMessage ?: "Error al preparar el video"
            }
        }
    }

    // Mantener la pantalla encendida mientras el reproductor está activo y restaurar brillo al salir
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val lp = window?.attributes
            if (lp != null) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
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

    // Sincronizar ciclo de vida de la actividad (pausar en segundo plano)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        exoPlayer.pause()
                        OboeAudioEngine.pause()
                    } catch (e: Throwable) {
                        Log.w("VideoPlayerScreen", "Error pausando en ciclo de vida: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
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
                    // Sincronizar el estado determinista de reproducción cuando el reproductor esté listo
                    isPlaying = exoPlayer.playWhenReady
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Actualiza el icono de inmediato según la intención real de reproducción
                isPlaying = playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                // Solo actualizar si no estamos en buffering activo, para evitar que una pausa transitoria de red desactive el icono
                if (!isBuffering && exoPlayer.playbackState != Player.STATE_ENDED) {
                    isPlaying = playing
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    // Si el video fue grabado en vertical con metadatos de rotación (90° o 270°),
                    // intercambiar ancho y alto para calcular la relación de aspecto correcta
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
                playbackErrorMessage = error.localizedMessage ?: "No se pudo reproducir el video."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Bucle para actualizar la barra de progreso periódicamente
    LaunchedEffect(isPlaying, isDraggingSlider) {
        while (true) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                onPositionChanged(currentPositionMs)
                onPlaybackProgress?.invoke(currentPositionMs, totalDurationMs)
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

    // Manejar botón Atrás del sistema
    BackHandler {
        onBackToHome()
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

        // Capa interactiva de gestos: toque simple (controles) y deslizamiento vertical (brillo a la izq, volumen a la der)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_gesture_surface")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        var currentY = startY
                        var hasDragged = false
                        val isLeft = startX < (size.width / 2f)
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.isConsumed) {
                                break
                            }

                            if (change.changedToUp()) {
                                if (!hasDragged) {
                                    // Toque simple: alternar visibilidad de los controles
                                    showControls = !showControls
                                    lastInteractionTime = System.currentTimeMillis()
                                } else {
                                    // Fin del deslizamiento: desvanecer suavemente el HUD minimalista
                                    gestureHideJob?.cancel()
                                    gestureHideJob = coroutineScope.launch {
                                        delay(1000)
                                        gestureIndicatorVisible = false
                                    }
                                }
                                change.consume()
                                break
                            }

                            val totalDx = abs(change.position.x - startX)
                            val totalDy = abs(change.position.y - startY)
                            val dragAmountY = change.position.y - currentY

                            if (!hasDragged) {
                                if (totalDy > touchSlop && totalDy > totalDx) {
                                    hasDragged = true
                                    change.consume()
                                    gestureHideJob?.cancel()
                                    gestureIndicatorType = if (isLeft) GestureIndicatorType.BRIGHTNESS else GestureIndicatorType.VOLUME
                                    gestureIndicatorVisible = true
                                    currentY = change.position.y
                                }
                            } else {
                                change.consume()
                                val delta = -dragAmountY / (size.height.toFloat().coerceAtLeast(1f) * 0.45f)
                                if (isLeft) {
                                    // Mitad izquierda: ajustar brillo de pantalla de la ventana
                                    currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                                    val window = activity?.window
                                    if (window != null) {
                                        val lp = window.attributes
                                        lp.screenBrightness = currentBrightness
                                        window.attributes = lp
                                    }
                                } else {
                                    // Mitad derecha: ajustar volumen multimedia físico del dispositivo
                                    volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                                    val targetVol = (volumeFraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                    if (targetVol != currentVolume) {
                                        currentVolume = targetVol
                                        audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)
                                    }
                                    if (isMuted && targetVol > 0) {
                                        isMuted = false
                                        exoPlayer.volume = 1f
                                        OboeAudioEngine.setVolume(1f)
                                    }
                                }
                                currentY = change.position.y
                            }
                        }
                    }
                }
        )

        // Indicador de carga / búfer cuando está procesando
        if (isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }

        // Indicador flotante minimalista para retroalimentación visual en tiempo real
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
                    volumeFraction
                }
                MinimalistGestureIndicator(
                    type = type,
                    fraction = fraction
                )
            }
        }

        // Interfaz superpuesta (Controles, Título, Tiempo)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
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

                // Barra superior: Título, tamaño, selector de fuente, aspecto y motor de audio
                TopControlsBar(
                    title = videoItem.name,
                    size = videoItem.formattedSize,
                    aspectModeLabel = currentAspectMode.label,
                    selectedAudioEngine = currentAudioEngine,
                    isPortrait = isPortrait,
                    onBack = onBackToHome,
                    onChangeSource = onChangeVideoSource,
                    onToggleAspectMode = {
                        currentAspectMode = when (currentAspectMode) {
                            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
                            AspectRatioMode.ZOOM -> AspectRatioMode.FILL
                            AspectRatioMode.FILL -> AspectRatioMode.FIT
                        }
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenSettings = {
                        onPositionChanged(exoPlayer.currentPosition)
                        onOpenSettings()
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Controles centrales: Retroceder 10s, Play/Pause, Avanzar 10s
                CenterPlaybackControls(
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        // Conmutación inmediata determinista basada en playWhenReady para respuesta al primer toque
                        val willPlay = !exoPlayer.playWhenReady || exoPlayer.playbackState == Player.STATE_ENDED
                        if (willPlay) {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                            if (currentAudioEngine == AudioEngineType.OBOE) {
                                OboeAudioEngine.start()
                            }
                            isPlaying = true
                        } else {
                            exoPlayer.pause()
                            OboeAudioEngine.pause()
                            isPlaying = false
                        }
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onRewind10 = {
                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                        exoPlayer.seekTo(newPos)
                        currentPositionMs = newPos
                        onPositionChanged(newPos)
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onForward10 = {
                        val maxPos = if (totalDurationMs > 0) totalDurationMs else exoPlayer.duration
                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(maxPos)
                        exoPlayer.seekTo(newPos)
                        currentPositionMs = newPos
                        onPositionChanged(newPos)
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Barra inferior: Slider de tiempo, duración, velocidad, ecualizador y silencio
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
                    onOpenSpeedSelector = {
                        showSpeedSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOpenEqualizer = {
                        showEqualizerSheet = true
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

        // Panel inferior del Ecualizador de Video en Tiempo Real
        if (showEqualizerSheet) {
            VideoEqualizerSheet(
                state = equalizerState,
                onStateChange = { equalizerState = it },
                onDismiss = { showEqualizerSheet = false }
            )
        }

        // Panel inferior de Velocidad de Reproducción (hasta 2.0x)
        if (showSpeedSheet) {
            PlaybackSpeedSheet(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { newSpeed ->
                    playbackSpeed = newSpeed
                    exoPlayer.playbackParameters = PlaybackParameters(newSpeed, 1.0f)
                },
                onDismiss = { showSpeedSheet = false }
            )
        }

        // Mensaje de Error Amigable si el archivo no puede decodificarse o no es accesible
        if (playbackErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se pudo reproducir el video",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = playbackErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackToHome
                            ) {
                                Text("Volver")
                            }
                            Button(
                                onClick = {
                                    playbackErrorMessage = null
                                    try {
                                        exoPlayer.seekTo(0)
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    } catch (e: Throwable) {
                                        playbackErrorMessage = e.localizedMessage ?: "Reintento fallido"
                                    }
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra superior de controles del reproductor adaptada tanto para modo vertical como horizontal.
 */
@Composable
private fun TopControlsBar(
    title: String,
    size: String,
    aspectModeLabel: String,
    selectedAudioEngine: AudioEngineType,
    isPortrait: Boolean,
    onBack: () -> Unit,
    onChangeSource: () -> Unit,
    onToggleAspectMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (isPortrait) {
            // Diseño optimizado para MODO VERTICAL (Portrait)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Fila 1: Botón Volver + Título y Tamaño (con todo el ancho disponible) + Botón Ajustes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a inicio",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (size.isNotEmpty()) {
                            Text(
                                text = "Tamaño: $size",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    // Acceso directo a Ajustes
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("player_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes del reproductor",
                            tint = Color.White
                        )
                    }
                }

                // Fila 2: Chips de acciones rápidas (Motor de audio, Cambiar fuente, Modo de aspecto)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip Motor de Audio
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedAudioEngine == AudioEngineType.OBOE) {
                            Color(0xFF0284C7).copy(alpha = 0.25f)
                        } else {
                            Color.White.copy(alpha = 0.15f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSettings)
                            .testTag("player_audio_engine_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Motor de audio",
                                tint = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (selectedAudioEngine == AudioEngineType.OBOE) "Oboe C++" else "Media3",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White
                                )
                            )
                        }
                    }

                    // Botón Cambiar Video
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onChangeSource)
                            .testTag("player_change_source_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Cambiar video",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Cambiar",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                            )
                        }
                    }

                    // Botón Modo de Pantalla (Aspect Ratio)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleAspectMode)
                            .testTag("player_aspect_mode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Modo de pantalla",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = aspectModeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                            )
                        }
                    }
                }
            }
        } else {
            // Diseño en MODO HORIZONTAL (Landscape)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a inicio",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (size.isNotEmpty()) {
                        Text(
                            text = "Tamaño: $size",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Chip indicador del motor de audio activo (Oboe C++ / Media3)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedAudioEngine == AudioEngineType.OBOE) {
                        Color(0xFF0284C7).copy(alpha = 0.25f)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenSettings)
                        .testTag("player_audio_engine_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Motor de audio",
                            tint = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (selectedAudioEngine == AudioEngineType.OBOE) "Oboe C++" else "Media3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAudioEngine == AudioEngineType.OBOE) Color(0xFF38BDF8) else Color.White
                            )
                        )
                    }
                }

                // Botón para cambiar de video (Galería o Gestor)
                FilledTonalButton(
                    onClick = onChangeSource,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("player_change_source_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cambiar", style = MaterialTheme.typography.labelMedium)
                }

                // Alternador de relación de aspecto
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleAspectMode)
                        .testTag("player_aspect_mode_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Modo de pantalla",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = aspectModeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }

                // Botón de acceso directo a Ajustes
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("player_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes del reproductor",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Controles de reproducción centrales: Retroceso, Play/Pausa y Avance.
 */
@Composable
private fun CenterPlaybackControls(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Retroceder 10 segundos
        IconButton(
            onClick = onRewind10,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .testTag("player_rewind_button")
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Retroceder 10 segundos",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Play / Pause principal
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("player_play_pause_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // Avanzar 10 segundos
        IconButton(
            onClick = onForward10,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .testTag("player_forward_button")
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Avanzar 10 segundos",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Barra inferior con barra de progreso, tiempo transcurrido, duración total y control de volumen.
 */
@Composable
private fun BottomControlsBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    isMuted: Boolean,
    playbackSpeed: Float,
    hasActiveEqualizer: Boolean,
    onOpenSpeedSelector: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Slider interactivo de progreso
            val progress = if (totalDurationMs > 0) {
                (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    onSeekStarted()
                    onSeekChanged(newProgress)
                },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("player_timeline_slider")
            )

            // Fila inferior con tiempo transcurrido, velocidad, ecualizador de video y silencio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(currentPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                    )
                    Text(
                        text = VideoUtils.formatDuration(totalDurationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón selector de velocidad (hasta 2x)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSpeedSelector)
                            .testTag("player_speed_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidad de reproducción",
                                tint = if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White
                                )
                            )
                        }
                    }

                    // Botón de Ecualizador de Video en Tiempo Real (OpenGL ES C++)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasActiveEqualizer) Color(0xFF0284C7).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (hasActiveEqualizer) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenEqualizer)
                            .testTag("player_equalizer_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Ecualizador de video",
                                tint = if (hasActiveEqualizer) Color(0xFF38BDF8) else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (hasActiveEqualizer) "EQ Activo" else "EQ Video",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasActiveEqualizer) Color(0xFF38BDF8) else Color.White
                                )
                            )
                        }
                    }

                    // Botón Silenciar
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.testTag("player_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tipo de ajuste para el indicador minimalista en pantalla.
 */
private enum class GestureIndicatorType {
    BRIGHTNESS,
    VOLUME
}

/**
 * Indicador visual flotante minimalista para retroalimentación en tiempo real
 * de los gestos táctiles de brillo y volumen sin invadir la reproducción.
 */
@Composable
private fun MinimalistGestureIndicator(
    type: GestureIndicatorType,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val percentage = (fraction.coerceIn(0f, 1f) * 100).roundToInt()
    val isBrightness = type == GestureIndicatorType.BRIGHTNESS

    val iconVector = if (isBrightness) {
        when {
            fraction > 0.66f -> Icons.Default.BrightnessHigh
            fraction > 0.33f -> Icons.Default.BrightnessMedium
            else -> Icons.Default.BrightnessLow
        }
    } else {
        when {
            percentage == 0 -> Icons.Default.VolumeMute
            fraction < 0.5f -> Icons.Default.VolumeDown
            else -> Icons.Default.VolumeUp
        }
    }

    val accentColor = if (isBrightness) Color(0xFFFACC15) else Color(0xFF38BDF8)
    val testTag = if (isBrightness) "gesture_indicator_brightness" else "gesture_indicator_volume"

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF141418).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
            .testTag(testTag)
            .width(46.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icono dinámico según el nivel actual
            Icon(
                imageVector = iconVector,
                contentDescription = if (isBrightness) "Brillo de pantalla" else "Volumen multimedia",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )

            // Barra vertical de nivel estilizada con esquinas redondeadas
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                        .background(
                            Brush.verticalGradient(
                                colors = if (isBrightness) {
                                    listOf(Color(0xFFFDE047), Color(0xFFEAB308))
                                } else {
                                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                }
                            ),
                            shape = RoundedCornerShape(2.5.dp)
                        )
                )
            }

            // Porcentaje numérico legible
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}
