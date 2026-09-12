package com.example.opengl

import android.util.Log

/**
 * NativeVideoFilter - Enlace JNI con el Motor Gráfico C++ y Shaders OpenGL ES
 *
 * Expone las funciones de bajo nivel compiladas en C++ (libnovaplayer_native.so)
 * para inicializar el pipeline gráfico, compilar los shaders GLSL en GPU y
 * renderizar cada fotograma de video aplicando ecualización en tiempo real.
 *
 * Compatible con arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */
object NativeVideoFilter {

    private const val TAG = "NativeVideoFilter"

    init {
        try {
            System.loadLibrary("novaplayer_native")
            Log.i(TAG, "Biblioteca C++ novaplayer_native cargada para OpenGL ES.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Error cargando novaplayer_native: ${e.message}")
        }
    }

    /**
     * Inicializa los shaders GLSL y compila el programa en GPU.
     */
    external fun nativeInit(): Boolean

    /**
     * Renderiza el frame actual de video con postprocesado en GPU.
     *
     * @param textureId Identificador de la textura GL_TEXTURE_EXTERNAL_OES
     * @param stMatrix Matriz de transformación de coordenadas de SurfaceTexture
     * @param mvpMatrix Matriz de Modelo-Vista-Proyección (control de aspecto/escalado)
     * @param brightness Brillo [-0.5f, 0.5f] (0.0f = neutro)
     * @param contrast Contraste [0.5f, 2.0f] (1.0f = neutro)
     * @param saturation Saturación [0.0f, 2.0f] (1.0f = neutro)
     * @param gamma Corrección Gamma [0.5f, 2.0f] (1.0f = neutro)
     * @param sharpness Realce de bordes / Nitidez [0.0f, 1.5f] (0.0f = desactivado)
     * @param texWidth Ancho de la textura para el kernel de nitidez
     * @param texHeight Alto de la textura para el kernel de nitidez
     */
    external fun nativeRender(
        textureId: Int,
        stMatrix: FloatArray,
        mvpMatrix: FloatArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        gamma: Float,
        sharpness: Float,
        texWidth: Float,
        texHeight: Float
    ): Boolean

    /**
     * Libera los recursos de GPU asociados al programa de sombreado.
     */
    external fun nativeRelease()
}
