package com.example.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max

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

    var currentEngine: AudioEngineType = AudioEngineType.MEDIA3
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

    // Filtros de estado DSP para el pipeline Media3 (Voces Claras y Compresor DRC Nocturno)
    private var media3VoicePrevLowPass = 0.0f
    private var media3Envelope = 0.0f

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
            // Modo Media3: procesar DSP en tiempo real (Canales, Voces Claras y Compresor Nocturno)
            val isVoiceClarity = OboeAudioEngine.isVoiceClarityEnabled()
            val voiceGain = OboeAudioEngine.voiceClarityGain
            val isCompressor = OboeAudioEngine.isDynamicCompressorEnabled()
            val compIntensity = OboeAudioEngine.compressorIntensity
            val hasDsp = isVoiceClarity || isCompressor || channelMode != AudioChannelMode.STEREO

            val outputBuffer = replaceOutputBuffer(pcmLength)

            if (!hasDsp) {
                // Ruta directa de máxima eficiencia sin cómputo adicional
                outputBuffer.put(pcmData, 0, pcmLength)
            } else {
                val sampleCount = pcmLength / 4
                for (s in 0 until sampleCount) {
                    val sL = (pcmData[s * 4].toInt() and 0xFF or (pcmData[s * 4 + 1].toInt() shl 8)).toShort().toFloat()
                    val sR = (pcmData[s * 4 + 2].toInt() and 0xFF or (pcmData[s * 4 + 3].toInt() shl 8)).toShort().toFloat()

                    var sampleL = sL
                    var sampleR = sR

                    // 1. Enrutamiento de canales (Mono / Pseudo-Estéreo Haas 3D)
                    if (channelMode == AudioChannelMode.MONO) {
                        val mono = (sampleL + sampleR) * 0.5f
                        sampleL = mono
                        sampleR = mono
                    } else if (channelMode == AudioChannelMode.SPATIAL_HAAS) {
                        val mono = (sampleL + sampleR) * 0.5f
                        val delayed = media3HaasBuffer[media3HaasIndex].toFloat()
                        media3HaasBuffer[media3HaasIndex] = mono.toInt().coerceIn(-32768, 32767).toShort()
                        media3HaasIndex = (media3HaasIndex + 1) % haasDelaySamples

                        sampleL = mono * 1.05f
                        sampleR = delayed * 0.90f + mono * 0.15f
                    }

                    // 2. Realce de Diálogos / Voces Claras (Peaking en banda vocal 1.5 kHz - 3.5 kHz)
                    if (isVoiceClarity && voiceGain > 0.01f) {
                        val avgSample = (sampleL + sampleR) * 0.5f
                        val lowPass = 0.72f * media3VoicePrevLowPass + 0.28f * avgSample
                        media3VoicePrevLowPass = lowPass
                        val voiceBand = avgSample - lowPass
                        val boost = voiceBand * (voiceGain * 1.35f)
                        sampleL += boost
                        sampleR += boost
                    }

                    // 3. Compresor Dinámico / Modo Nocturno (DRC - atenúa picos/explosiones, eleva susurros)
                    if (isCompressor && compIntensity > 0.01f) {
                        val maxSample = max(abs(sampleL), abs(sampleR))
                        if (maxSample > media3Envelope) {
                            media3Envelope = 0.08f * maxSample + 0.92f * media3Envelope
                        } else {
                            media3Envelope = 0.002f * maxSample + 0.998f * media3Envelope
                        }

                        val threshold = 9500.0f * (1.0f - compIntensity * 0.35f)
                        if (media3Envelope > threshold) {
                            val excess = media3Envelope - threshold
                            val ratio = 3.5f + compIntensity * 4.5f
                            val compressedEnvelope = threshold + (excess / ratio)
                            val gainReduction = compressedEnvelope / max(1.0f, media3Envelope)
                            sampleL *= gainReduction
                            sampleR *= gainReduction
                        } else if (media3Envelope > 80.0f && media3Envelope < threshold * 0.45f) {
                            val quietBoost = 1.0f + (compIntensity * 0.65f) * (1.0f - (media3Envelope / (threshold * 0.45f)))
                            sampleL *= quietBoost
                            sampleR *= quietBoost
                        }
                    }

                    // 4. Clamping con protección contra clipping digital a 16 bits
                    val outL = sampleL.coerceIn(-32767.0f, 32767.0f).toInt().toShort()
                    val outR = sampleR.coerceIn(-32767.0f, 32767.0f).toInt().toShort()

                    outputBuffer.put((outL.toInt() and 0xFF).toByte())
                    outputBuffer.put(((outL.toInt() shr 8) and 0xFF).toByte())
                    outputBuffer.put((outR.toInt() and 0xFF).toByte())
                    outputBuffer.put(((outR.toInt() shr 8) and 0xFF).toByte())
                }
            }
            outputBuffer.flip()
        }
    }

    override fun onFlush() {
        try {
            media3VoicePrevLowPass = 0.0f
            media3Envelope = 0.0f
            media3HaasIndex = 0
            Arrays.fill(media3HaasBuffer, 0.toShort())

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
            media3VoicePrevLowPass = 0.0f
            media3Envelope = 0.0f
            media3HaasIndex = 0
            Arrays.fill(media3HaasBuffer, 0.toShort())

            if (currentEngine == AudioEngineType.OBOE) {
                OboeAudioEngine.flush()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error en onReset de OboeAudioProcessor: ${e.message}")
        }
    }
}
