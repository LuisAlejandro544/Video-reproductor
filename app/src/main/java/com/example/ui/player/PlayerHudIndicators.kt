package com.example.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * PlayerHudIndicators.kt - Elementos visuales flotantes y HUD del reproductor multimedia.
 */

/**
 * Lado del doble toque para salto rápido de 5 segundos.
 */
enum class DoubleTapSeekSide {
    LEFT,
    RIGHT
}

/**
 * Tipo de ajuste para el indicador minimalista en pantalla.
 */
enum class GestureIndicatorType {
    BRIGHTNESS,
    VOLUME
}

/**
 * Indicador visual flotante dinámico y minimalista para retroalimentación en tiempo real
 * de los gestos táctiles de brillo y volumen sin invadir la reproducción.
 *
 * Mejoras dinámicas:
 * - Interpolación fluida de la fracción con amortiguación suave (Spring).
 * - Expansión adaptativa de la cápsula y efecto de rebote en extremos (0% y 100%).
 * - Resplandor dinámico según el nivel actual de brillo o volumen.
 */
@Composable
fun MinimalistGestureIndicator(
    type: GestureIndicatorType,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = clampedFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gesture_fraction_animation"
    )

    val percentage = (animatedFraction * 100).roundToInt()
    val isBrightness = type == GestureIndicatorType.BRIGHTNESS

    val isExtreme = animatedFraction >= 0.98f || animatedFraction <= 0.02f
    val iconScale by animateFloatAsState(
        targetValue = if (isExtreme) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "gesture_icon_scale"
    )

    val capsuleWidth by animateDpAsState(
        targetValue = if (isExtreme) 52.dp else 48.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "gesture_capsule_width"
    )

    val iconVector = if (isBrightness) {
        when {
            animatedFraction > 0.66f -> Icons.Default.BrightnessHigh
            animatedFraction > 0.33f -> Icons.Default.BrightnessMedium
            else -> Icons.Default.BrightnessLow
        }
    } else {
        when {
            percentage == 0 -> Icons.Default.VolumeMute
            animatedFraction < 0.5f -> Icons.Default.VolumeDown
            else -> Icons.Default.VolumeUp
        }
    }

    val accentColor = if (isBrightness) Color(0xFFFACC15) else Color(0xFF38BDF8)
    val testTag = if (isBrightness) "gesture_indicator_brightness" else "gesture_indicator_volume"

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF0F121C).copy(alpha = 0.90f),
        border = BorderStroke(
            1.5.dp,
            accentColor.copy(alpha = (0.25f + (animatedFraction * 0.45f)).coerceIn(0.25f, 0.70f))
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .testTag(testTag)
            .width(capsuleWidth)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = if (isBrightness) "Brillo de pantalla" else "Volumen multimedia",
                tint = accentColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedFraction.coerceIn(0.02f, 1f))
                        .background(
                            Brush.verticalGradient(
                                colors = if (isBrightness) {
                                    listOf(Color(0xFFFEF08A), Color(0xFFEAB308))
                                } else {
                                    listOf(Color(0xFF7DD3FC), Color(0xFF0284C7))
                                }
                            ),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
        }
    }
}

/**
 * Insignia flotante dinámica para el avance rápido fluido a 2X al mantener presionado.
 * Incorpora animación viva de pulso y resplandor continuo para transmitir aceleración.
 */
@Composable
fun FastForward2xBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fast_forward_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_forward_glow"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_forward_scale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = glowAlpha)),
        shadowElevation = 10.dp,
        modifier = modifier
            .scale(scalePulse)
            .testTag("player_fast_forward_2x_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Avance rápido 2X",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "2X",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "Avance Rápido",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Indicador HUD animado dinámico para retroalimentación de Doble Tap (+5s / -5s).
 * Incluye efecto dinámico de rebote elástico y aura luminosa al activarse.
 */
@Composable
fun DoubleTapSeekIndicator(
    side: DoubleTapSeekSide,
    modifier: Modifier = Modifier
) {
    var isTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(side) {
        isTriggered = true
    }

    val dynamicScale by animateFloatAsState(
        targetValue = if (isTriggered) 1.0f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "double_tap_scale"
    )

    Surface(
        shape = CircleShape,
        color = Color(0xFF0B1120).copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.85f)),
        shadowElevation = 14.dp,
        modifier = modifier
            .size(78.dp)
            .scale(dynamicScale)
            .testTag(if (side == DoubleTapSeekSide.LEFT) "double_tap_rewind_indicator" else "double_tap_forward_indicator")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (side == DoubleTapSeekSide.LEFT) {
                    Icons.Default.FastRewind
                } else {
                    Icons.Default.FastForward
                },
                contentDescription = if (side == DoubleTapSeekSide.LEFT) "Retroceder 5 segundos" else "Adelantar 5 segundos",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (side == DoubleTapSeekSide.LEFT) "-5 seg" else "+5 seg",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Botón flotante para desbloquear la pantalla cuando los controles están bloqueados.
 */
@Composable
fun ControlsUnlockButton(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onUnlock)
            .testTag("player_unlock_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Pantalla bloqueada. Toca para desbloquear.",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Desbloquear",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Mensaje de error amigable cuando un archivo no puede reproducirse.
 */
@Composable
fun PlayerErrorOverlay(
    errorMessage: String,
    onBackToHome: () -> Unit,
    onChangeVideoSource: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onBackToHome) {
                        Text("Volver")
                    }
                    Button(onClick = onChangeVideoSource) {
                        Text("Cambiar video")
                    }
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

/**
 * Banner flotante para notificar de forma clara e intuitiva cambios críticos en el estado del audio:
 * - "Sin sonido" cuando el volumen baja a 0 o se silencia.
 * - "Sonido restablecido" cuando el volumen vuelve a subir.
 */
@Composable
fun SoundStatusHudBanner(
    isMuted: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B1120).copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            if (isMuted) Color(0xFFEF4444).copy(alpha = 0.65f) else Color(0xFF38BDF8).copy(alpha = 0.65f)
        ),
        shadowElevation = 10.dp,
        modifier = modifier.testTag("sound_status_hud_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = title,
                tint = if (isMuted) Color(0xFFF87171) else Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

