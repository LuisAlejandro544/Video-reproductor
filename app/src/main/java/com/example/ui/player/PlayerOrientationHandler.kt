package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * PlayerOrientationHandler.kt - Gestión de rotación por sensor de hardware.
 *
 * Utiliza el acelerómetro/giroscopio del dispositivo mediante OrientationEventListener
 * para alternar automáticamente entre modo horizontal, horizontal invertido y vertical
 * según la postura física del terminal móvil, incluso si el bloqueo de rotación del sistema
 * está activo. Restablece de forma determinista la orientación a vertical al salir.
 */
@Composable
fun PlayerOrientationHandler(
    context: Context,
    activity: Activity?
) {
    DisposableEffect(context, activity) {
        val orientationEventListener = if (activity != null) {
            object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                private var lastOrientation = -1

                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return

                    // Rotación física del dispositivo:
                    // 60° a 120°  -> Horizontal inverso (Reverse Landscape)
                    // 240° a 300° -> Horizontal estándar (Landscape)
                    // 340° a 360° o 0° a 20° -> Vertical (Portrait)
                    val targetOrientation = when (orientation) {
                        in 60..120 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        in 240..300 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        in 340..360, in 0..20 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> return
                    }

                    if (targetOrientation != lastOrientation) {
                        lastOrientation = targetOrientation
                        activity.requestedOrientation = targetOrientation
                    }
                }
            }
        } else null

        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        onDispose {
            orientationEventListener?.disable()
            // Al salir de la reproducción, restablecer siempre a vertical
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
