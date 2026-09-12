package com.example.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.example.ui.AspectRatioMode
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGLVideoSurface - Vista de Reproducción Acelerada por GPU con OpenGL ES y C++
 *
 * Conecta el decodificador nativo de hardware de ExoPlayer con una textura externa OES
 * (GL_TEXTURE_EXTERNAL_OES). Cada frame es postprocesado en tiempo real en la GPU
 * mediante los fragment shaders compilados en C++ por NativeVideoFilter.
 */
class OpenGLVideoRenderer(
    private val onSurfaceCreatedCallback: (Surface) -> Unit
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "OpenGLVideoRenderer"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null
    private var glSurfaceView: GLSurfaceView? = null

    // Matrices de transformación
    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Dimensiones de superficie y de video para cálculo de relación de aspecto
    private var surfaceWidth: Int = 1
    private var surfaceHeight: Int = 1
    private var videoWidth: Int = 1
    private var videoHeight: Int = 1
    private var aspectMode: AspectRatioMode = AspectRatioMode.FIT

    // Parámetros de postprocesado en GPU
    @Volatile
    var equalizerState: VideoEqualizerState = VideoEqualizerState.DEFAULT

    fun attachView(view: GLSurfaceView) {
        this.glSurfaceView = view
    }

    fun updateVideoDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            this.videoWidth = width
            this.videoHeight = height
            glSurfaceView?.requestRender()
        }
    }

    fun updateAspectRatioMode(mode: AspectRatioMode) {
        this.aspectMode = mode
        glSurfaceView?.requestRender()
    }

    fun updateEqualizer(state: VideoEqualizerState) {
        this.equalizerState = state
        glSurfaceView?.requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            Log.i(TAG, "onSurfaceCreated: Inicializando pipeline OpenGL ES nativo...")
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // Inicializar motor de shaders nativo en C++
            try {
                NativeVideoFilter.nativeInit()
            } catch (e: Throwable) {
                Log.e(TAG, "Error inicializando NativeVideoFilter: ${e.message}")
            }

            // Generar textura externa OES para el stream de decodificación por hardware
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            // Crear SurfaceTexture asociada a la textura GPU y envolver en un Surface de Android.
            // CRÍTICO: Debe pasarse 'mainHandler' porque GLThread no posee un Looper.
            // Sin un Handler asociado a un Looper activo, Android descarta el callback OnFrameAvailable
            // y la pantalla permanece completamente en negro durante la reproducción.
            val st = SurfaceTexture(textureId)
            st.setOnFrameAvailableListener(this, mainHandler)
            surfaceTexture = st

            val surface = Surface(st)
            videoSurface = surface

            // CRÍTICO: La asignación de superficie a ExoPlayer DEBE realizarse en el hilo principal
            // (Main Looper) para evitar una IllegalStateException que bloquee la aplicación.
            mainHandler.post {
                try {
                    onSurfaceCreatedCallback(surface)
                    glSurfaceView?.requestRender()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error en onSurfaceCreatedCallback en el hilo principal: ${e.message}", e)
                }
            }

            Matrix.setIdentityM(stMatrix, 0)
            Matrix.setIdentityM(mvpMatrix, 0)
        } catch (e: Throwable) {
            Log.e(TAG, "Error crítico durante onSurfaceCreated: ${e.message}", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged: ${width}x${height}")
        surfaceWidth = if (width > 0) width else 1
        surfaceHeight = if (height > 0) height else 1
        try {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        } catch (e: Throwable) {
            Log.w(TAG, "Error estableciendo glViewport: ${e.message}")
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val st = surfaceTexture ?: return
            synchronized(this) {
                try {
                    st.updateTexImage()
                    st.getTransformMatrix(stMatrix)
                } catch (e: Exception) {
                    Log.w(TAG, "Frame drop transitorio al actualizar textura: ${e.message}")
                    return
                }
            }

            // Calcular relación de aspecto de superficie y de video
            val currentEq = equalizerState
            val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
            val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

            // Detectar si el video genera barras laterales (pillarboxes), común en videos verticales en pantallas apaisadas
            val hasPillarbox = aspectMode == AspectRatioMode.FIT && videoAspect < surfaceAspect

            // PASO 1: Si Pillarbox Blur está activo y hay barras laterales, renderizar el fondo desenfocado y atenuado
            if (currentEq.pillarboxBlur && hasPillarbox) {
                val bgMvpMatrix = FloatArray(16)
                Matrix.setIdentityM(bgMvpMatrix, 0)
                // Zoom proporcional para cubrir toda la superficie sin barras negras
                val bgScaleY = surfaceAspect / videoAspect
                Matrix.scaleM(bgMvpMatrix, 0, 1.0f, bgScaleY, 1.0f)

                NativeVideoFilter.nativeRender(
                    textureId = textureId,
                    stMatrix = stMatrix,
                    mvpMatrix = bgMvpMatrix,
                    brightness = currentEq.brightness,
                    contrast = currentEq.contrast,
                    saturation = currentEq.saturation,
                    gamma = currentEq.gamma,
                    sharpness = 0.0f,
                    texWidth = videoWidth.toFloat(),
                    texHeight = videoHeight.toFloat(),
                    blueLightFilter = currentEq.blueLightFilter,
                    blurRadius = 14.0f,
                    backgroundDim = 0.45f,
                    fsrEnabled = 0.0f,
                    fsrSharpness = 0.0f
                )
            }

            // PASO 2: Calcular matriz de transformación para modo de aspecto (FIT, ZOOM, FILL)
            calculateMvpMatrix()

            // Delegar el renderizado frontal al shader C++ (con AMD FSR 1.0 si está activado)
            NativeVideoFilter.nativeRender(
                textureId = textureId,
                stMatrix = stMatrix,
                mvpMatrix = mvpMatrix,
                brightness = currentEq.brightness,
                contrast = currentEq.contrast,
                saturation = currentEq.saturation,
                gamma = currentEq.gamma,
                sharpness = currentEq.sharpness,
                texWidth = videoWidth.toFloat(),
                texHeight = videoHeight.toFloat(),
                blueLightFilter = currentEq.blueLightFilter,
                blurRadius = 0.0f,
                backgroundDim = 0.0f,
                fsrEnabled = if (currentEq.fsrEnabled) 1.0f else 0.0f,
                fsrSharpness = currentEq.fsrSharpness
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Excepción transitoria en onDrawFrame: ${e.message}")
        }
    }

    private fun calculateMvpMatrix() {
        Matrix.setIdentityM(mvpMatrix, 0)

        if (videoWidth <= 0 || videoHeight <= 0 || surfaceWidth <= 0 || surfaceHeight <= 0) {
            return
        }

        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

        var scaleX = 1.0f
        var scaleY = 1.0f

        when (aspectMode) {
            AspectRatioMode.FIT -> {
                if (videoAspect > surfaceAspect) {
                    scaleY = surfaceAspect / videoAspect
                } else {
                    scaleX = videoAspect / surfaceAspect
                }
            }
            AspectRatioMode.ZOOM -> {
                if (videoAspect > surfaceAspect) {
                    scaleX = videoAspect / surfaceAspect
                } else {
                    scaleY = surfaceAspect / videoAspect
                }
            }
            AspectRatioMode.FILL -> {
                scaleX = 1.0f
                scaleY = 1.0f
            }
        }

        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1.0f)
    }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        try {
            glSurfaceView?.requestRender()
        } catch (e: Throwable) {
            Log.w(TAG, "Error en requestRender: ${e.message}")
        }
    }

    fun release() {
        try {
            glSurfaceView?.queueEvent {
                try {
                    NativeVideoFilter.nativeRelease()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error liberando recursos nativos en GLThread: ${e.message}")
                }
            }
            mainHandler.post {
                try {
                    videoSurface?.release()
                    videoSurface = null
                    surfaceTexture?.release()
                    surfaceTexture = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error liberando recursos de superficie: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando recursos OpenGL: ${e.message}")
        }
    }
}

/**
 * Composable que incrusta la superficie OpenGL en el árbol de Jetpack Compose.
 */
@Composable
fun OpenGLVideoPlayerView(
    player: ExoPlayer,
    equalizerState: VideoEqualizerState,
    aspectRatioMode: AspectRatioMode,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val renderer = remember {
        OpenGLVideoRenderer { surface ->
            mainHandler.post {
                try {
                    player.setVideoSurface(surface)
                } catch (e: Throwable) {
                    Log.e("OpenGLVideoPlayerView", "Error asignando superficie a ExoPlayer: ${e.message}", e)
                }
            }
        }
    }

    // Actualizar propiedades en caliente hacia el renderer
    renderer.updateEqualizer(equalizerState)
    renderer.updateAspectRatioMode(aspectRatioMode)
    renderer.updateVideoDimensions(videoWidth, videoHeight)

    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, glSurfaceViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        glSurfaceViewRef?.onPause()
                    } catch (e: Throwable) {
                        Log.w("OpenGLVideoPlayerView", "Error en onPause de GLSurfaceView: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        glSurfaceViewRef?.onResume()
                    } catch (e: Throwable) {
                        Log.w("OpenGLVideoPlayerView", "Error en onResume de GLSurfaceView: ${e.message}")
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

    DisposableEffect(player) {
        onDispose {
            mainHandler.post {
                try {
                    player.clearVideoSurface()
                } catch (e: Throwable) {
                    Log.w("OpenGLVideoPlayerView", "Error limpiando superficie en ExoPlayer: ${e.message}")
                }
            }
            renderer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                renderer.attachView(this)
                glSurfaceViewRef = this
            }
        },
        update = { view ->
            renderer.updateEqualizer(equalizerState)
            renderer.updateAspectRatioMode(aspectRatioMode)
            renderer.updateVideoDimensions(videoWidth, videoHeight)
            try {
                view.requestRender()
            } catch (e: Throwable) {
                Log.w("OpenGLVideoPlayerView", "Error solicitando renderizado: ${e.message}")
            }
        },
        modifier = modifier
    )
}
