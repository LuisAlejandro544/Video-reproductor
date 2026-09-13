package com.example.ui.settings

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * AudioTestManager.kt - Generador Senoidal y Validador de Salida de Audio
 *
 * Propósito:
 * Proporciona generación en tiempo real de un tono de prueba puro (440 Hz / La4)
 * en formato PCM lineal de 16 bits sin dependencias simuladas.
 * Permite verificar físicamente la salida acústica de los altavoces o auriculares
 * tanto en el motor de bajo nivel Google Oboe C++ (AAudio / OpenSL ES) como
 * en el subsistema estándar AudioTrack de Android Media3.
 */
object AudioTestManager {
    private const val SAMPLE_RATE = 48000
    private const val DURATION_SECONDS = 1.5
    private const val FREQUENCY = 440.0 // Tono A4 (440 Hz)

    suspend fun playTone(
        engine: AudioEngineType,
        onTrackCreated: (AudioTrack?) -> Unit,
        onFinish: () -> Unit
    ) {
        val totalSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val numChannels = 2
        val pcmBytes = ByteArray(totalSamples * numChannels * 2)

        // Sintetizar tono senoidal puro de 440 Hz con envolvente suave (fade in / out)
        var byteIndex = 0
        for (i in 0 until totalSamples) {
            val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / FREQUENCY)
            val envelope = when {
                i < 2400 -> i / 2400.0
                i > totalSamples - 2400 -> (totalSamples - i) / 2400.0
                else -> 1.0
            }
            val sampleVal = (sin(angle) * 16000.0 * envelope).toInt().toShort()
            val lowByte = (sampleVal.toInt() and 0xFF).toByte()
            val highByte = ((sampleVal.toInt() shr 8) and 0xFF).toByte()

            // Canal izquierdo
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
            // Canal derecho
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
        }

        if (engine == AudioEngineType.OBOE) {
            // Reproducción 100% real mediante Google Oboe C++ nativo
            OboeAudioEngine.init(SAMPLE_RATE, numChannels)
            OboeAudioEngine.setVolume(1.0f)
            OboeAudioEngine.start()
            OboeAudioEngine.write(pcmBytes, 0, pcmBytes.size)

            // Esperar duración del tono y finalizar
            delay((DURATION_SECONDS * 1000).toLong())
            onFinish()
        } else {
            // Reproducción 100% real mediante AudioTrack estándar de Android
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufSize, pcmBytes.size))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                onTrackCreated(track)
                track.write(pcmBytes, 0, pcmBytes.size)
                track.play()

                delay((DURATION_SECONDS * 1000).toLong())
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                onTrackCreated(null)
                onFinish()
            } catch (e: Exception) {
                onFinish()
            }
        }
    }

    fun stopTone(audioTrack: AudioTrack?) {
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        OboeAudioEngine.stop()
    }
}
