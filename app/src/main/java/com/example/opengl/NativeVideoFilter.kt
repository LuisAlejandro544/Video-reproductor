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
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("novaplayer_native")
            isLibraryLoaded = true
            Log.i(TAG, "Biblioteca C++ novaplayer_native cargada para OpenGL ES.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Error cargando novaplayer_native: ${e.message}")
            isLibraryLoaded = false
        } catch (e: Throwable) {
            Log.e(TAG, "Excepción inesperada cargando novaplayer_native: ${e.message}")
            isLibraryLoaded = false
        }
    }

    /**
     * Verifica si la biblioteca nativa C++ está disponible y cargada.
     */
    fun isAvailable(): Boolean = isLibraryLoaded

    /**
     * Inicializa los shaders GLSL y compila el programa en GPU.
     */
    fun nativeInit(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            internalNativeInit()
        } catch (e: Throwable) {
            Log.e(TAG, "Error invocando internalNativeInit: ${e.message}")
            false
        }
    }

    /**
     * Renderiza el frame actual de video con postprocesado en GPU.
     */
    fun nativeRender(
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
    ): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            internalNativeRender(
                textureId,
                stMatrix,
                mvpMatrix,
                brightness,
                contrast,
                saturation,
                gamma,
                sharpness,
                texWidth,
                texHeight
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error invocando internalNativeRender: ${e.message}")
            false
        }
    }

    /**
     * Libera los recursos de GPU asociados al programa de sombreado.
     */
    fun nativeRelease() {
        if (!isLibraryLoaded) return
        try {
            internalNativeRelease()
        } catch (e: Throwable) {
            Log.e(TAG, "Error invocando internalNativeRelease: ${e.message}")
        }
    }

    @JvmStatic
    private external fun internalNativeInit(): Boolean

    @JvmStatic
    private external fun internalNativeRender(
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

    @JvmStatic
    private external fun internalNativeRelease()
}
