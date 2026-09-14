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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PlayerGestureDetector.kt - Detección y gestión de gestos sobre el lienzo de video.
 *
 * Soporta de manera fluida y concurrente:
 * 1. Pellizcar para Zoom (Pinch-to-Zoom): Ampliación táctil fluida con dos dedos desde 1.0x hasta 10.0x.
 * 2. Desplazamiento panorámico (Pan): Con el zoom activo (>1.0x), deslizar con uno o dos dedos para explorar la imagen.
 * 3. Restablecimiento rápido de Zoom: Doble toque sobre video ampliado para volver a escala original (1.0x).
 * 4. Toque simple (Single Tap): Muestra / oculta la interfaz de controles superpuesta.
 * 5. Doble toque (Double Tap): Adelanta (+5s) o retrocede (-5s) en escala 1.0x según el lado de la pantalla pulsado.
 * 6. Pulsación prolongada (Long Press): Activa avance rápido a 2X en el lateral derecho mientras se mantenga presionado.
 * 7. Deslizamiento vertical (Vertical Drag):
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
    zoomScale: Float = 1.0f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    onZoomChange: (scale: Float, panX: Float, panY: Float) -> Unit = { _, _, _ -> },
    onResetZoom: () -> Unit = {},
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
    val zoomScaleUpdated by rememberUpdatedState(zoomScale)
    val panOffsetXUpdated by rememberUpdatedState(panOffsetX)
    val panOffsetYUpdated by rememberUpdatedState(panOffsetY)
    val onZoomChangeUpdated by rememberUpdatedState(onZoomChange)
    val onResetZoomUpdated by rememberUpdatedState(onResetZoom)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_gesture_surface")
            .pointerInput(isControlsLocked, playbackSpeed, zoomScale) {
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
                        var lastPointerX = startX
                        var lastPointerY = startY
                        var currentY = startY
                        var hasDragged = false
                        var isPanningZoom = false
                        var isPinching = false
                        var wasPinching = false
                        var prevPinchDistance = 0f
                        var prevCentroid = Offset(startX, startY)
                        val isLeft = startX < (size.width / 2f)
                        val isRightSide = startX >= (size.width / 2f)
                        val touchSlop = viewConfiguration.touchSlop

                        // Brillo base exacto al iniciar la interacción táctil
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
                        val isCurrentlyZoomed = zoomScaleUpdated > 1.05f

                        val fastForwardJob = if (isRightSide && !isControlsLocked && !isCurrentlyZoomed) {
                            coroutineScope.launch {
                                delay(400)
                                if (!hasDragged && !isPinching && !wasPinching) {
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
                                val pressedPointers = event.changes.filter { it.pressed }
                                if (pressedPointers.isEmpty()) {
                                    break
                                }

                                if (pressedPointers.size >= 2) {
                                    // Interacción multitáctil: Pellizcar para Zoom continuo hasta x10
                                    isPinching = true
                                    wasPinching = true
                                    fastForwardJob?.cancel()
                                    if (isFastForwardActive) {
                                        isFastForwardActive = false
                                        onStopFastForward2xUpdated()
                                    }
                                    singleTapJob?.cancel()
                                    singleTapJob = null
                                    if (hasDragged && !isPanningZoom) {
                                        hasDragged = false
                                        gestureHideJob?.cancel()
                                        onHideGestureIndicatorUpdated()
                                    }

                                    val p1 = pressedPointers[0].position
                                    val p2 = pressedPointers[1].position
                                    val currentDist = hypot(p1.x - p2.x, p1.y - p2.y)
                                    val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                    if (prevPinchDistance > 0f) {
                                        val zoomRatio = currentDist / prevPinchDistance
                                        val newScale = (zoomScaleUpdated * zoomRatio).coerceIn(1.0f, 10.0f)
                                        val centroidDelta = currentCentroid - prevCentroid

                                        val maxPanX = (size.width * (newScale - 1f)) / 2f
                                        val maxPanY = (size.height * (newScale - 1f)) / 2f

                                        val newPanX = if (newScale <= 1.0f) 0f else (panOffsetXUpdated + centroidDelta.x).coerceIn(-maxPanX, maxPanX)
                                        val newPanY = if (newScale <= 1.0f) 0f else (panOffsetYUpdated + centroidDelta.y).coerceIn(-maxPanY, maxPanY)

                                        onZoomChangeUpdated(newScale, newPanX, newPanY)
                                    }

                                    prevPinchDistance = currentDist
                                    prevCentroid = currentCentroid
                                    event.changes.forEach { it.consume() }
                                } else {
                                    // Un solo dedo apoyado
                                    val change = pressedPointers[0]
                                    prevPinchDistance = 0f

                                    if (change.changedToUp()) {
                                        fastForwardJob?.cancel()
                                        if (isFastForwardActive) {
                                            isFastForwardActive = false
                                            onStopFastForward2xUpdated()
                                            try {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            } catch (_: Throwable) {}
                                        } else if (wasPinching) {
                                            // Si veníamos de un pellizco con dos dedos, no ejecutar tap ni ajustar volumen
                                            change.consume()
                                            break
                                        } else if (!hasDragged) {
                                            val totalDx = abs(change.position.x - startX)
                                            val totalDy = abs(change.position.y - startY)
                                            if (totalDx < touchSlop && totalDy < touchSlop) {
                                                val now = System.currentTimeMillis()

                                                if (zoomScaleUpdated > 1.05f) {
                                                    // Con zoom activo, el doble toque restablece la vista a 1.0x
                                                    if (now - lastTapTimeMs < 320) {
                                                        singleTapJob?.cancel()
                                                        singleTapJob = null
                                                        lastTapTimeMs = 0L
                                                        onResetZoomUpdated()
                                                        try {
                                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                        } catch (_: Throwable) {}
                                                    } else {
                                                        lastTapTimeMs = now
                                                        singleTapJob?.cancel()
                                                        singleTapJob = coroutineScope.launch {
                                                            delay(180)
                                                            onSingleTapUpdated()
                                                            lastTapTimeMs = 0L
                                                        }
                                                    }
                                                } else {
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
                                            }
                                        } else {
                                            if (!isPanningZoom) {
                                                gestureHideJob?.cancel()
                                                gestureHideJob = coroutineScope.launch {
                                                    delay(1000)
                                                    onHideGestureIndicatorUpdated()
                                                }
                                            }
                                        }
                                        change.consume()
                                        break
                                    }

                                    val totalDx = abs(change.position.x - startX)
                                    val totalDy = abs(change.position.y - startY)

                                    if (wasPinching) {
                                        change.consume()
                                        lastPointerX = change.position.x
                                        lastPointerY = change.position.y
                                        continue
                                    }

                                    if (zoomScaleUpdated > 1.05f) {
                                        // Con zoom activo, el arrastre de un dedo desplaza el marco del video
                                        if (!hasDragged) {
                                            if (totalDx > touchSlop || totalDy > touchSlop) {
                                                hasDragged = true
                                                isPanningZoom = true
                                                fastForwardJob?.cancel()
                                                lastPointerX = change.position.x
                                                lastPointerY = change.position.y
                                                change.consume()
                                            }
                                        } else if (isPanningZoom) {
                                            val dx = change.position.x - lastPointerX
                                            val dy = change.position.y - lastPointerY
                                            val maxPanX = (size.width * (zoomScaleUpdated - 1f)) / 2f
                                            val maxPanY = (size.height * (zoomScaleUpdated - 1f)) / 2f

                                            val newPanX = (panOffsetXUpdated + dx).coerceIn(-maxPanX, maxPanX)
                                            val newPanY = (panOffsetYUpdated + dy).coerceIn(-maxPanY, maxPanY)

                                            onZoomChangeUpdated(zoomScaleUpdated, newPanX, newPanY)
                                            lastPointerX = change.position.x
                                            lastPointerY = change.position.y
                                            change.consume()
                                        }
                                    } else {
                                        // Modo estándar (escala 1.0x): Brillo en lado izquierdo, volumen en lado derecho
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
                                            val delta = -dragAmountY / (size.height.toFloat().coerceAtLeast(1f) * 0.45f)
                                            if (isLeft) {
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

