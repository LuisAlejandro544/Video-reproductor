package com.example.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
        Log.i(TAG, "onSurfaceCreated: Inicializando pipeline OpenGL ES nativo...")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Inicializar motor de shaders nativo en C++
        NativeVideoFilter.nativeInit()

        // Generar textura externa OES para el stream de decodificación por hardware
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Crear SurfaceTexture asociada a la textura GPU y envolver en un Surface de Android
        val st = SurfaceTexture(textureId)
        st.setOnFrameAvailableListener(this)
        surfaceTexture = st

        val surface = Surface(st)
        videoSurface = surface
        onSurfaceCreatedCallback(surface)

        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged: ${width}x${height}")
        surfaceWidth = if (width > 0) width else 1
        surfaceHeight = if (height > 0) height else 1
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
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

        // Calcular matriz de transformación para modo de aspecto (FIT, ZOOM, FILL)
        calculateMvpMatrix()

        // Delegar el renderizado al shader C++
        val currentEq = equalizerState
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
            texHeight = videoHeight.toFloat()
        )
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
        glSurfaceView?.requestRender()
    }

    fun release() {
        try {
            videoSurface?.release()
            videoSurface = null
            surfaceTexture?.release()
            surfaceTexture = null
            NativeVideoFilter.nativeRelease()
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
    val context = LocalContext.current

    val renderer = remember {
        OpenGLVideoRenderer { surface ->
            player.setVideoSurface(surface)
        }
    }

    // Actualizar propiedades en caliente hacia el renderer
    renderer.updateEqualizer(equalizerState)
    renderer.updateAspectRatioMode(aspectRatioMode)
    renderer.updateVideoDimensions(videoWidth, videoHeight)

    DisposableEffect(Unit) {
        onDispose {
            player.setVideoSurface(null)
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
            }
        },
        update = { view ->
            renderer.updateEqualizer(equalizerState)
            renderer.updateAspectRatioMode(aspectRatioMode)
            renderer.updateVideoDimensions(videoWidth, videoHeight)
            view.requestRender()
        },
        modifier = modifier
    )
}
