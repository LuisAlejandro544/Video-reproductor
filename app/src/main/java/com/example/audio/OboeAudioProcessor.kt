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

    /**
     * Modo de canal (Estéreo, Mono o Pseudo-Estéreo Haas) en tiempo real.
     */
    var channelMode: AudioChannelMode = AudioChannelMode.STEREO
        set(value) {
            field = value
            OboeAudioEngine.setChannelMode(value)
        }

    private var inputChannelCount = 2
    private var tempByteArray: ByteArray = ByteArray(0)
    private var stereoExpandBuffer: ByteArray = ByteArray(0)
    private var silenceByteArray: ByteArray = ByteArray(0)

    // Buffer Haas para el modo Media3 (fallback)
    private val haasDelaySamples = 768
    private val media3HaasBuffer = ShortArray(haasDelaySamples)
    private var media3HaasIndex = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Solo procesamos tramas PCM de 16 bits estándar
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        inputChannelCount = inputAudioFormat.channelCount
        // Salida estandarizada a 2 canales (estéreo) para permitir conversión mono->estéreo
        val outputChannels = 2

        // Inicializar el motor nativo de Oboe con 2 canales estéreo y la frecuencia del archivo
        try {
            OboeAudioEngine.init(
                sampleRate = inputAudioFormat.sampleRate,
                channelCount = outputChannels
            )
            OboeAudioEngine.setChannelMode(channelMode)

            if (currentEngine == AudioEngineType.OBOE) {
                OboeAudioEngine.start()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error inicializando OboeAudioEngine: ${e.message}")
        }

        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            outputChannels,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val pcmData: ByteArray
        val pcmLength: Int

        if (inputChannelCount == 1) {
            // Conversión Mono -> Estéreo duplicando cada muestra de 16 bits (2 bytes -> 4 bytes)
            val monoSamples = remaining / 2
            val stereoBytes = monoSamples * 4
            if (stereoExpandBuffer.size < stereoBytes) {
                stereoExpandBuffer = ByteArray(stereoBytes)
            }

            for (i in 0 until monoSamples) {
                val b0 = inputBuffer.get()
                val b1 = inputBuffer.get()
                // Canal izquierdo
                stereoExpandBuffer[i * 4] = b0
                stereoExpandBuffer[i * 4 + 1] = b1
                // Canal derecho
                stereoExpandBuffer[i * 4 + 2] = b0
                stereoExpandBuffer[i * 4 + 3] = b1
            }
            pcmData = stereoExpandBuffer
            pcmLength = stereoBytes
        } else {
            // Ya es estéreo (2 canales)
            if (tempByteArray.size < remaining) {
                tempByteArray = ByteArray(remaining)
            }
            inputBuffer.get(tempByteArray, 0, remaining)
            pcmData = tempByteArray
            pcmLength = remaining
        }

        if (currentEngine == AudioEngineType.OBOE) {
            // Enviar datos al motor nativo Oboe en C++ donde opera el DSP en tiempo real
            try {
                OboeAudioEngine.write(pcmData, 0, pcmLength)
            } catch (e: Throwable) {
                Log.e(TAG, "Error escribiendo en OboeAudioEngine: ${e.message}")
            }

            // Alimentar silencio PCM al sink estándar para mantener el reloj de hardware sincronizado
            // en ExoPlayer sin generar duplicación de sonido con Oboe
            if (silenceByteArray.size < pcmLength) {
                silenceByteArray = ByteArray(pcmLength)
            }
            val outputBuffer = replaceOutputBuffer(pcmLength)
            outputBuffer.put(silenceByteArray, 0, pcmLength)
            outputBuffer.flip()
        } else {
            // Modo Media3: aplicar DSP de canales en caso de estar activo
            val outputBuffer = replaceOutputBuffer(pcmLength)
            if (channelMode == AudioChannelMode.MONO) {
                val sampleCount = pcmLength / 4
                for (s in 0 until sampleCount) {
                    val sL = (pcmData[s * 4].toInt() and 0xFF or (pcmData[s * 4 + 1].toInt() shl 8)).toShort()
                    val sR = (pcmData[s * 4 + 2].toInt() and 0xFF or (pcmData[s * 4 + 3].toInt() shl 8)).toShort()
                    val mono = ((sL.toInt() + sR.toInt()) / 2).coerceIn(-32768, 32767).toShort()
                    outputBuffer.put((mono.toInt() and 0xFF).toByte())
                    outputBuffer.put(((mono.toInt() shr 8) and 0xFF).toByte())
                    outputBuffer.put((mono.toInt() and 0xFF).toByte())
                    outputBuffer.put(((mono.toInt() shr 8) and 0xFF).toByte())
                }
            } else if (channelMode == AudioChannelMode.SPATIAL_HAAS) {
                val sampleCount = pcmLength / 4
                for (s in 0 until sampleCount) {
                    val sL = (pcmData[s * 4].toInt() and 0xFF or (pcmData[s * 4 + 1].toInt() shl 8)).toShort()
                    val sR = (pcmData[s * 4 + 2].toInt() and 0xFF or (pcmData[s * 4 + 3].toInt() shl 8)).toShort()
                    val mono = ((sL.toInt() + sR.toInt()) / 2).toShort()

                    val delayed = media3HaasBuffer[media3HaasIndex]
                    media3HaasBuffer[media3HaasIndex] = mono
                    media3HaasIndex = (media3HaasIndex + 1) % haasDelaySamples

                    val outL = (mono.toInt() * 1.05f).toInt().coerceIn(-32768, 32767).toShort()
                    val outR = (delayed.toInt() * 0.90f + mono.toInt() * 0.15f).toInt().coerceIn(-32768, 32767).toShort()

                    outputBuffer.put((outL.toInt() and 0xFF).toByte())
                    outputBuffer.put(((outL.toInt() shr 8) and 0xFF).toByte())
                    outputBuffer.put((outR.toInt() and 0xFF).toByte())
                    outputBuffer.put(((outR.toInt() shr 8) and 0xFF).toByte())
                }
            } else {
                outputBuffer.put(pcmData, 0, pcmLength)
            }
            outputBuffer.flip()
        }
    }

    override fun onFlush() {
        try {
            if (currentEngine == AudioEngineType.OBOE) {
                // Al adelantar/retroceder o reiniciar, vaciar el búfer inmediatamente sin detener el hardware
                OboeAudioEngine.flush()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error en onFlush de OboeAudioProcessor: ${e.message}")
        }
    }

    override fun onReset() {
        try {
            if (currentEngine == AudioEngineType.OBOE) {
                OboeAudioEngine.flush()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error en onReset de OboeAudioProcessor: ${e.message}")
        }
    }
}
