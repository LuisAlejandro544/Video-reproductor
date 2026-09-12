package com.example.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * OboeAudioProcessor - Procesador de Audio Media3/ExoPlayer con salida hacia Google Oboe C++
 *
 * Intercepta las tramas PCM 16-bit decodificadas del flujo de video.
 * - Si el motor seleccionado es OBOE: Envía los búferes de audio directamente al motor
 *   nativo C++ de Oboe para reproducción por hardware en ultra baja latencia (AAudio / OpenSL ES)
 *   y emite silencio a AudioTrack para preservar la sincronización de reloj A/V perfecta en ExoPlayer.
 * - Si el motor seleccionado es MEDIA3: Transfiere el búfer íntegramente hacia el AudioTrack
 *   estándar de Android.
 */
@UnstableApi
class OboeAudioProcessor : BaseAudioProcessor() {

    private val TAG = "OboeAudioProcessor"

    var currentEngine: AudioEngineType = AudioEngineType.OBOE
        set(value) {
            field = value
            try {
                if (value == AudioEngineType.OBOE) {
                    OboeAudioEngine.start()
                } else {
                    OboeAudioEngine.stop()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error alternando motor de audio a $value: ${e.message}")
            }
        }

    private var tempByteArray: ByteArray = ByteArray(0)
    private var silenceByteArray: ByteArray = ByteArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Solo procesamos tramas PCM de 16 bits estándar
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // Inicializar el motor nativo de Oboe con la tasa de muestreo y número de canales del video
        try {
            OboeAudioEngine.init(
                sampleRate = inputAudioFormat.sampleRate,
                channelCount = inputAudioFormat.channelCount
            )

            if (currentEngine == AudioEngineType.OBOE) {
                OboeAudioEngine.start()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error inicializando OboeAudioEngine: ${e.message}")
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (currentEngine == AudioEngineType.OBOE) {
            // Reutilizar o redimensionar arreglo temporal de bytes
            if (tempByteArray.size < remaining) {
                tempByteArray = ByteArray(remaining)
            }
            inputBuffer.get(tempByteArray, 0, remaining)

            // Enviar datos al motor nativo Oboe en C++
            try {
                OboeAudioEngine.write(tempByteArray, 0, remaining)
            } catch (e: Throwable) {
                Log.e(TAG, "Error escribiendo en OboeAudioEngine: ${e.message}")
            }

            // Alimentar silencio PCM al sink estándar para mantener el reloj de hardware sincronizado
            // en ExoPlayer sin generar duplicación de sonido con Oboe
            if (silenceByteArray.size < remaining) {
                silenceByteArray = ByteArray(remaining)
            }
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(silenceByteArray, 0, remaining)
            outputBuffer.flip()
        } else {
            // Modo Media3: pasar los bytes directamente al pipeline estándar
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
        }
    }

    override fun onFlush() {
        try {
            if (currentEngine == AudioEngineType.OBOE) {
                OboeAudioEngine.stop()
                OboeAudioEngine.start()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error en onFlush de OboeAudioProcessor: ${e.message}")
        }
    }

    override fun onReset() {
        try {
            OboeAudioEngine.stop()
        } catch (e: Throwable) {
            Log.e(TAG, "Error en onReset de OboeAudioProcessor: ${e.message}")
        }
    }
}
