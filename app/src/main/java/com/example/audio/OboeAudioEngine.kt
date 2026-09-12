package com.example.audio

import android.util.Log

/**
 * OboeAudioEngine - Controlador Kotlin del Motor Nativo Oboe C++
 *
 * Administra el ciclo de vida del flujo de audio nativo compilado con Google Oboe.
 * Carga dinámicamente la biblioteca compartida 'libnovaplayer_native.so'.
 */
object OboeAudioEngine {
    private const val TAG = "OboeAudioEngine"
    private var isLibraryLoaded = false
    private var isCompressorActive = false
    private var isVoiceClarityActive = false

    init {
        try {
            System.loadLibrary("novaplayer_native")
            isLibraryLoaded = true
            Log.i(TAG, "Biblioteca nativa novaplayer_native cargada con éxito.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Error cargando la biblioteca nativa novaplayer_native", e)
            isLibraryLoaded = false
        }
    }

    fun isAvailable(): Boolean = isLibraryLoaded

    fun isDynamicCompressorEnabled(): Boolean = isCompressorActive

    fun isVoiceClarityEnabled(): Boolean = isVoiceClarityActive

    fun init(sampleRate: Int = 48000, channelCount: Int = 2): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeInit(sampleRate, channelCount)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeInit", e)
            false
        }
    }

    fun start(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeStart()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeStart", e)
            false
        }
    }

    fun pause(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativePause()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativePause", e)
            false
        }
    }

    fun stop(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeStop()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeStop", e)
            false
        }
    }

    fun release() {
        if (!isLibraryLoaded) return
        try {
            nativeRelease()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeRelease", e)
        }
    }

    fun write(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!isLibraryLoaded) return 0
        return try {
            nativeWrite(buffer, offset, length)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeWrite", e)
            0
        }
    }

    fun setVolume(volume: Float) {
        if (!isLibraryLoaded) return
        try {
            nativeSetVolume(volume)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeSetVolume", e)
        }
    }

    /**
     * Activa o desactiva el Compresor Dinámico / Modo Nocturno (DRC) en C++.
     * Atenúa picos estrepitosos (explosiones, disparos) y eleva sonidos suaves.
     */
    fun setDynamicCompressor(enabled: Boolean, intensity: Float = 0.8f) {
        isCompressorActive = enabled
        if (!isLibraryLoaded) return
        try {
            nativeSetDynamicCompressor(enabled, intensity)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeSetDynamicCompressor", e)
        }
    }

    /**
     * Activa o desactiva el Realce de Diálogos / Modo Voces Claras en C++.
     * Aplica ganancia selectiva sobre la banda vocal (1.5 kHz a 3.5 kHz).
     */
    fun setVoiceClarity(enabled: Boolean, gain: Float = 0.75f) {
        isVoiceClarityActive = enabled
        if (!isLibraryLoaded) return
        try {
            nativeSetVoiceClarity(enabled, gain)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en nativeSetVoiceClarity", e)
        }
    }

    fun isPlaying(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeIsPlaying()
        } catch (e: Exception) {
            false
        }
    }

    fun getApiName(): String {
        if (!isLibraryLoaded) return "No disponible"
        return try {
            nativeGetApiName()
        } catch (e: Exception) {
            "Error al consultar API"
        }
    }

    fun getSampleRate(): Int {
        if (!isLibraryLoaded) return 0
        return try {
            nativeGetSampleRate()
        } catch (e: Exception) {
            0
        }
    }

    fun getChannelCount(): Int {
        if (!isLibraryLoaded) return 0
        return try {
            nativeGetChannelCount()
        } catch (e: Exception) {
            0
        }
    }

    fun getFramesWritten(): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeGetFramesWritten()
        } catch (e: Exception) {
            0L
        }
    }

    // Declaraciones de métodos nativos JNI
    private external fun nativeInit(sampleRate: Int, channelCount: Int): Boolean
    private external fun nativeStart(): Boolean
    private external fun nativePause(): Boolean
    private external fun nativeStop(): Boolean
    private external fun nativeRelease()
    private external fun nativeWrite(buffer: ByteArray, offset: Int, length: Int): Int
    private external fun nativeSetVolume(volume: Float)
    private external fun nativeSetDynamicCompressor(enabled: Boolean, intensity: Float)
    private external fun nativeSetVoiceClarity(enabled: Boolean, gain: Float)
    private external fun nativeIsPlaying(): Boolean
    private external fun nativeGetApiName(): String
    private external fun nativeGetSampleRate(): Int
    private external fun nativeGetChannelCount(): Int
    private external fun nativeGetFramesWritten(): Long
}
