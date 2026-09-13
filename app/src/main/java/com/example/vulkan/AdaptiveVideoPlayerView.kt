package com.example.vulkan

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.GraphicsEngineType
import com.example.opengl.OpenGLVideoPlayerView
import com.example.opengl.VideoEqualizerState
import com.example.ui.AspectRatioMode
import kotlinx.coroutines.delay

/**
 * AdaptiveVideoPlayerView.kt - Selector de Motor Gráfico con Degradación Elegante (Fallback Automático)
 *
 * Propósito:
 * Garantiza una reproducción robusta y sin interrupciones independientemente de las
 * peculiaridades del hardware o la estabilidad de los controladores Vulkan del dispositivo.
 *
 * Mecanismo de Fallback Automático:
 * 1. Inspección Previa de Capacidades: Evalúa mediante VulkanCapabilities si el hardware
 *    soporta Vulkan 1.1+ tanto en Android OS (PackageManager) como a nivel de driver nativo.
 *    Si no se cumplen los requisitos mínimos, redirige inmediatamente a OpenGL ES 3.0+.
 * 2. Fallback Dinámico en Tiempo de Ejecución: Si el usuario fuerza el uso de Vulkan pero el
 *    dispositivo falla durante la creación del dispositivo lógico, la asignación de swapchain
 *    o la importación de AHardwareBuffer, el componente conmuta en caliente a OpenGL ES 3.0+
 *    sin cerrar la aplicación ni pausar el audio.
 * 3. Indicador de Aviso No Invasivo: Muestra una notificación temporal transparente para informar
 *    al usuario sobre la conmutación a OpenGL ES garantizando transparencia total.
 *
 * Licencia: Apache 2.0. Compatible con 32 y 64 bits (minSdk 26).
 */
@Composable
fun AdaptiveVideoPlayerView(
    player: ExoPlayer,
    preferredEngine: GraphicsEngineType,
    equalizerState: VideoEqualizerState,
    aspectRatioMode: AspectRatioMode,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier,
    onActiveEngineChanged: ((GraphicsEngineType) -> Unit)? = null
) {
    val TAG = "AdaptiveVideoPlayer"
    val context = LocalContext.current

    // Estado reactivo del motor gráfico en uso activo
    var activeEngine by remember(preferredEngine) {
        mutableStateOf(preferredEngine)
    }

    // Mensaje descriptivo en caso de activación del fallback automático
    var fallbackReason by remember { mutableStateOf<String?>(null) }
    var showFallbackBanner by remember { mutableStateOf(false) }

    // Validación inicial de capacidades antes de montar la superficie
    LaunchedEffect(preferredEngine) {
        if (preferredEngine == GraphicsEngineType.VULKAN) {
            val status = VulkanCapabilities.checkCapabilities(context)
            if (!status.isVulkan11OrHigher) {
                val reason = "Hardware incompatible con Vulkan 1.1+ (Detectado: ${status.apiVersionString})"
                Log.w(TAG, "$reason. Conmutando a OpenGL ES 3.0+")
                activeEngine = GraphicsEngineType.OPENGL_ES
                fallbackReason = reason
                showFallbackBanner = true
                onActiveEngineChanged?.invoke(GraphicsEngineType.OPENGL_ES)
            } else {
                activeEngine = GraphicsEngineType.VULKAN
                onActiveEngineChanged?.invoke(GraphicsEngineType.VULKAN)
            }
        } else {
            activeEngine = GraphicsEngineType.OPENGL_ES
            onActiveEngineChanged?.invoke(GraphicsEngineType.OPENGL_ES)
        }
    }

    // Ocultar banner de degradación elegante tras 4 segundos
    LaunchedEffect(showFallbackBanner) {
        if (showFallbackBanner) {
            delay(4000)
            showFallbackBanner = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (activeEngine) {
            GraphicsEngineType.VULKAN -> {
                VulkanVideoPlayerView(
                    player = player,
                    equalizerState = equalizerState,
                    aspectRatioMode = aspectRatioMode,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    onFallbackToOpenGL = { reason ->
                        Log.e(TAG, "Fallback dinámico activado desde Vulkan: $reason")
                        activeEngine = GraphicsEngineType.OPENGL_ES
                        fallbackReason = "Vulkan no pudo inicializar: $reason"
                        showFallbackBanner = true
                        onActiveEngineChanged?.invoke(GraphicsEngineType.OPENGL_ES)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            GraphicsEngineType.OPENGL_ES -> {
                OpenGLVideoPlayerView(
                    player = player,
                    equalizerState = equalizerState,
                    aspectRatioMode = aspectRatioMode,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Banner informativo sutil si ocurrió degradación automática
        AnimatedVisibility(
            visible = showFallbackBanner && fallbackReason != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDD202020),
                contentColor = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Aviso de degradación gráfica",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "Degradación elegante: Conmutado a OpenGL ES (${fallbackReason ?: ""})",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
