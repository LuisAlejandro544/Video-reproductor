package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PlayerHudOverlay.kt - Capa modular de indicadores visuales y notificaciones HUD en tiempo real
 *
 * Responsabilidad:
 * Renderiza de forma animada:
 * - Botón de desbloqueo de pantalla si los controles están bloqueados.
 * - Spinner de buffering/carga de ExoPlayer.
 * - Indicador minimalista de gesto de brillo y volumen en los extremos izquierdo/derecho.
 * - Badge flotante de avance rápido a 2X al mantener presionado.
 * - Indicador interactivo de nivel de Zoom (hasta x10) con botón para reiniciar encuadre.
 * - Notificación HUD animada de volumen silenciado ("Sin sonido") o reactivado ("Sonido activado").
 * - Animación de doble toque en laterales para saltos de ±5 segundos.
 */
@Composable
fun BoxScope.PlayerHudOverlay(
    isControlsLocked: Boolean,
    onUnlockControls: () -> Unit,
    isBuffering: Boolean,
    gestureIndicatorVisible: Boolean,
    gestureIndicatorType: GestureIndicatorType?,
    currentBrightness: Float,
    currentVolume: Int,
    maxVolume: Int,
    isFastForwarding2x: Boolean,
    showZoomHud: Boolean,
    showControls: Boolean,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    soundBannerText: String?,
    soundBannerSubtext: String?,
    isSoundBannerMuted: Boolean,
    doubleTapSeekSide: DoubleTapSeekSide?,
    modifier: Modifier = Modifier
) {
    // Botón flotante para desbloquear la pantalla
    if (isControlsLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            ControlsUnlockButton(onUnlock = onUnlockControls)
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

    // Indicador HUD interactivo para Zoom táctil (Pinch-to-zoom hasta x10)
    AnimatedVisibility(
        visible = showZoomHud || (showControls && zoomScale > 1.05f),
        enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.88f),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.88f),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = if (isFastForwarding2x) 86.dp else 24.dp)
    ) {
        ZoomHudIndicator(
            zoomScale = zoomScale,
            onResetZoom = onResetZoom
        )
    }

    // Banner flotante para aviso de volumen a 0 ("Sin sonido") o reactivado ("Sonido activado")
    AnimatedVisibility(
        visible = soundBannerText != null,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.88f),
        exit = fadeOut(tween(250)),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (isFastForwarding2x) 86.dp else 40.dp)
    ) {
        soundBannerText?.let { title ->
            SoundStatusHudBanner(
                isMuted = isSoundBannerMuted,
                title = title,
                subtitle = soundBannerSubtext ?: ""
            )
        }
    }

    // Indicador HUD para Doble Tap (+5s / -5s)
    AnimatedVisibility(
        visible = doubleTapSeekSide != null,
        enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.8f),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.8f),
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
}
