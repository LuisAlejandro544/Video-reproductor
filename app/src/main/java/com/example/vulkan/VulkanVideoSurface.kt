package com.example.vulkan

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import com.example.opengl.VideoEqualizerState
import com.example.ui.AspectRatioMode

/**
 * VulkanVideoSurface.kt - Componente de Superficie en Jetpack Compose para Vulkan 1.1+
 *
 * Propósito:
 * Proporciona una superficie de renderizado directo mediante SurfaceView conectada
 * al motor gráfico de bajo nivel Vulkan (VulkanVideoEngine.cpp a través de ANativeWindow).
 *
 * Características principales:
 * 1. Acceso a Hardware sin sobrecarga de EGL: A diferencia de GLSurfaceView, SurfaceView
 *    expone un ANativeWindow puro directamente a Vulkan, reduciendo ciclos de CPU y consumo de batería.
 * 2. Conexión de Video en Memoria Compartida (Zero-Copy): Las tramas decodificadas se procesan
 *    en memoria gráfica compartida sin copias intermedias en memoria RAM de usuario.
 * 3. Detección y Degradación Elegante: Si el controlador Vulkan falla al crear la superficie o
 *    el Swapchain, invoca automáticamente el callback onFallbackToOpenGL para conmutar sin
 *    interrupción a OpenGL ES 3.0.
 *
 * Licencia: Apache 2.0 (Permisiva).
 * minSdk: Android 8.0 (API 26). Compatible con arquitecturas de 32 y 64 bits.
 */
@Composable
fun VulkanVideoPlayerView(
    player: ExoPlayer,
    equalizerState: VideoEqualizerState,
    aspectRatioMode: AspectRatioMode,
    videoWidth: Int,
    videoHeight: Int,
    onFallbackToOpenGL: (reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val TAG = "VulkanVideoSurface"
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }
    var activeSurface by remember { mutableStateOf<Surface?>(null) }
    var isEngineReady by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose {
            mainHandler.post {
                try {
                    player.clearVideoSurface()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error limpiando superficie en ExoPlayer: ${e.message}")
                }
            }
            NativeVulkanVideoEngine.release()
            activeSurface?.release()
            activeSurface = null
            isEngineReady = false
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                // Configurar para superposición transparente sobre la UI
                setZOrderMediaOverlay(true)

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        val surface = holder.surface
                        if (surface == null || !surface.isValid) {
                            Log.e(TAG, "Superficie SurfaceView inválida al crear.")
                            onFallbackToOpenGL("Superficie nativa no válida")
                            return
                        }

                        val width = width.coerceAtLeast(1)
                        val height = height.coerceAtLeast(1)

                        Log.i(TAG, "Inicializando motor Vulkan 1.1+ sobre SurfaceView (${width}x${height})...")

                        // Inicializar pipeline nativo en C++
                        val initialized = NativeVulkanVideoEngine.init(surface, width, height)
                        if (!initialized) {
                            val errorMsg = NativeVulkanVideoEngine.getLastError()
                            Log.w(TAG, "Fallo de inicialización en Vulkan: $errorMsg. Activando fallback a OpenGL ES.")
                            mainHandler.post {
                                onFallbackToOpenGL(errorMsg)
                            }
                            return
                        }

                        activeSurface = surface
                        isEngineReady = true

                        // Conectar superficie a ExoPlayer en el hilo principal
                        mainHandler.post {
                            try {
                                player.setVideoSurface(surface)
                                Log.i(TAG, "Superficie Vulkan asignada exitosamente a ExoPlayer.")
                            } catch (e: Throwable) {
                                Log.e(TAG, "Error asignando superficie a ExoPlayer: ${e.message}", e)
                                onFallbackToOpenGL("Error vinculando reproductor: ${e.message}")
                            }
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        Log.i(TAG, "surfaceChanged: Reconfigurando swapchain Vulkan a ${width}x${height}")
                        if (isEngineReady && width > 0 && height > 0) {
                            val resized = NativeVulkanVideoEngine.resize(width, height)
                            if (!resized) {
                                Log.w(TAG, "Advertencia: Fallo al redimensionar Swapchain Vulkan.")
                            }
                        }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Log.i(TAG, "surfaceDestroyed: Liberando recursos Vulkan")
                        isEngineReady = false
                        mainHandler.post {
                            try {
                                player.clearVideoSurface()
                            } catch (_: Throwable) {}
                        }
                        NativeVulkanVideoEngine.release()
                        activeSurface = null
                    }
                })

                surfaceViewRef = this
            }
        },
        update = { view ->
            // Actualizar parámetros visuales de ecualizador en el motor nativo Vulkan si está listo
            if (isEngineReady) {
                try {
                    NativeVulkanVideoEngine.render(
                        brightness = equalizerState.brightness,
                        contrast = equalizerState.contrast,
                        saturation = equalizerState.saturation,
                        gamma = equalizerState.gamma,
                        sharpness = equalizerState.sharpness,
                        blueLightFilter = equalizerState.blueLightFilter,
                        sunMode = equalizerState.sunMode
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "Error en pasada de renderizado Vulkan: ${e.message}")
                }
            }

            // Asegurar que si el surface es válido y ExoPlayer se desconectó, se vuelva a enlazar
            activeSurface?.let { surf ->
                if (surf.isValid) {
                    try {
                        player.setVideoSurface(surf)
                    } catch (_: Throwable) {}
                }
            }
        },
        modifier = modifier
    )
}
