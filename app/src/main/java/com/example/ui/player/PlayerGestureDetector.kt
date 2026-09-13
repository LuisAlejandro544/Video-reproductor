package com.example.ui.player

import android.app.Activity
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PlayerGestureDetector.kt - Detección y gestión de gestos sobre el lienzo de video.
 *
 * Soporta de manera fluida y concurrente:
 * 1. Toque simple (Single Tap): Muestra / oculta la interfaz de controles superpuesta.
 * 2. Doble toque (Double Tap): Adelanta (+5s) o retrocede (-5s) según el lado de la pantalla pulsado.
 * 3. Pulsación prolongada (Long Press): Activa avance rápido a 2X en el lateral derecho mientras se mantenga presionado.
 * 4. Deslizamiento vertical (Vertical Drag):
 *    - Mitad izquierda: Ajuste fino de brillo de pantalla en la ventana activa.
 *    - Mitad derecha: Ajuste de volumen multimedia del dispositivo con retroalimentación HUD.
 */
@Composable
fun PlayerGestureSurface(
    isControlsLocked: Boolean,
    playbackSpeed: Float,
    view: View,
    activity: Activity?,
    audioManager: AudioManager?,
    maxVolume: Int,
    currentVolume: Int,
    onVolumeChange: (Int) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (isLeft: Boolean) -> Unit,
    onStartFastForward2x: () -> Unit,
    onStopFastForward2x: () -> Unit,
    onShowGestureIndicator: (type: GestureIndicatorType) -> Unit,
    onHideGestureIndicator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapIsLeft by remember { mutableStateOf(false) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    var gestureHideJob by remember { mutableStateOf<Job?>(null) }

    // Estados actualizados reactivamente para el bucle de puntero asíncrono
    val currentBrightnessUpdated by rememberUpdatedState(currentBrightness)
    val onBrightnessChangeUpdated by rememberUpdatedState(onBrightnessChange)
    val currentVolumeUpdated by rememberUpdatedState(currentVolume)
    val maxVolumeUpdated by rememberUpdatedState(maxVolume)
    val onVolumeChangeUpdated by rememberUpdatedState(onVolumeChange)
    val onSingleTapUpdated by rememberUpdatedState(onSingleTap)
    val onDoubleTapSeekUpdated by rememberUpdatedState(onDoubleTapSeek)
    val onStartFastForward2xUpdated by rememberUpdatedState(onStartFastForward2x)
    val onStopFastForward2xUpdated by rememberUpdatedState(onStopFastForward2x)
    val onShowGestureIndicatorUpdated by rememberUpdatedState(onShowGestureIndicator)
    val onHideGestureIndicatorUpdated by rememberUpdatedState(onHideGestureIndicator)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_gesture_surface")
            .pointerInput(isControlsLocked, playbackSpeed) {
                if (isControlsLocked) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }
                        }
                    }
                } else {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        var currentY = startY
                        var hasDragged = false
                        val isLeft = startX < (size.width / 2f)
                        val isRightSide = startX >= (size.width / 2f)
                        val touchSlop = viewConfiguration.touchSlop

                        // Brillo base exacto al iniciar la interacción táctil (evita regresiones o saltos bruscos)
                        val winBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                        var gestureBrightness = if (winBrightness in 0.01f..1.0f) {
                            winBrightness
                        } else {
                            currentBrightnessUpdated
                        }

                        // Fracción base de volumen al iniciar la interacción táctil
                        var gestureVolumeFraction = if (maxVolumeUpdated > 0) {
                            (currentVolumeUpdated.toFloat() / maxVolumeUpdated.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0.5f
                        }

                        var isFastForwardActive = false

                        val fastForwardJob = if (isRightSide && !isControlsLocked) {
                            coroutineScope.launch {
                                delay(400)
                                if (!hasDragged) {
                                    isFastForwardActive = true
                                    onStartFastForward2xUpdated()
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    } catch (_: Throwable) {}
                                }
                            }
                        } else null

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (change.changedToUp()) {
                                    fastForwardJob?.cancel()
                                    if (isFastForwardActive) {
                                        isFastForwardActive = false
                                        onStopFastForward2xUpdated()
                                        try {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        } catch (_: Throwable) {}
                                    } else if (!hasDragged) {
                                        val totalDx = abs(change.position.x - startX)
                                        val totalDy = abs(change.position.y - startY)
                                        if (totalDx < touchSlop && totalDy < touchSlop) {
                                            val now = System.currentTimeMillis()
                                            val isCenterTap = startX >= (size.width * 0.28f) && startX <= (size.width * 0.72f)

                                            if (isCenterTap) {
                                                singleTapJob?.cancel()
                                                singleTapJob = null
                                                lastTapTimeMs = 0L
                                                onSingleTapUpdated()
                                            } else {
                                                if (now - lastTapTimeMs < 320 && lastTapIsLeft == isLeft) {
                                                    singleTapJob?.cancel()
                                                    singleTapJob = null
                                                    lastTapTimeMs = 0L

                                                    onDoubleTapSeekUpdated(isLeft)

                                                    try {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    } catch (_: Throwable) {}
                                                } else {
                                                    lastTapTimeMs = now
                                                    lastTapIsLeft = isLeft

                                                    singleTapJob?.cancel()
                                                    singleTapJob = coroutineScope.launch {
                                                        delay(160)
                                                        onSingleTapUpdated()
                                                        lastTapTimeMs = 0L
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        gestureHideJob?.cancel()
                                        gestureHideJob = coroutineScope.launch {
                                            delay(1000)
                                            onHideGestureIndicatorUpdated()
                                        }
                                    }
                                    change.consume()
                                    break
                                }

                                val totalDx = abs(change.position.x - startX)
                                val totalDy = abs(change.position.y - startY)
                                val dragAmountY = change.position.y - currentY

                                if (!hasDragged) {
                                    if (totalDy > touchSlop && totalDy > totalDx && !isFastForwardActive) {
                                        fastForwardJob?.cancel()
                                        hasDragged = true
                                        change.consume()
                                        gestureHideJob?.cancel()
                                        onShowGestureIndicatorUpdated(if (isLeft) GestureIndicatorType.BRIGHTNESS else GestureIndicatorType.VOLUME)
                                        currentY = change.position.y
                                    }
                                } else {
                                    change.consume()
                                    // Sensibilidad de arrastre: deslizamiento hacia arriba suma, hacia abajo resta
                                    val delta = -dragAmountY / (size.height.toFloat().coerceAtLeast(1f) * 0.45f)
                                    if (isLeft) {
                                        // Acumulación progresiva sobre la variable local: elimina el rebote o caída de brillo
                                        gestureBrightness = (gestureBrightness + delta).coerceIn(0.01f, 1f)
                                        onBrightnessChangeUpdated(gestureBrightness)
                                        val window = activity?.window
                                        if (window != null) {
                                            val lp = window.attributes
                                            lp.screenBrightness = gestureBrightness
                                            window.attributes = lp
                                        }
                                    } else {
                                        gestureVolumeFraction = (gestureVolumeFraction + delta).coerceIn(0f, 1f)
                                        val targetVol = (gestureVolumeFraction * maxVolumeUpdated).roundToInt().coerceIn(0, maxVolumeUpdated)
                                        if (targetVol != currentVolumeUpdated) {
                                            onVolumeChangeUpdated(targetVol)
                                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                        }
                                    }
                                    currentY = change.position.y
                                }
                            }
                        } finally {
                            fastForwardJob?.cancel()
                            if (isFastForwardActive) {
                                isFastForwardActive = false
                                onStopFastForward2xUpdated()
                            }
                        }
                    }
                }
            }
    )
}
