package com.example.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * OboeAudioProcessor - Procesador de Audio Media3/ExoPlayer con salida hacia Google Oboe C++
 *
 * Intercepta las tramas PCM 16-bit decodificadas del flujo de video.
 * - Si el motor seleccionado es OBOE: Envía los búferes de audio directamente al motor
 *   nativo C++ de Oboe para reproducción por hardware en ultra baja latencia (AAudio / OpenSL ES)
 *   y vacía la salida para silenciar el AudioTrack estándar.
 * - Si el motor seleccionado es MEDIA3: Transfiere el búfer íntegramente hacia el AudioTrack
 *   estándar de Android.
 */
@UnstableApi
class OboeAudioProcessor : BaseAudioProcessor() {

    var currentEngine: AudioEngineType = AudioEngineType.OBOE
        set(value) {
            field = value
            if (value == AudioEngineType.OBOE) {
                OboeAudioEngine.start()
            } else {
                OboeAudioEngine.stop()
            }
        }

    private var tempByteArray: ByteArray = ByteArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Solo procesamos tramas PCM de 16 bits estándar
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // Inicializar el motor nativo de Oboe con la tasa de muestreo y número de canales del video
        OboeAudioEngine.init(
            sampleRate = inputAudioFormat.sampleRate,
            channelCount = inputAudioFormat.channelCount
        )

        if (currentEngine == AudioEngineType.OBOE) {
            OboeAudioEngine.start()
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
            val position = inputBuffer.position()
            inputBuffer.get(tempByteArray, 0, remaining)

            // Enviar datos al motor nativo Oboe en C++
            OboeAudioEngine.write(tempByteArray, 0, remaining)

            // Consumir el buffer y no emitir nada aguas abajo para evitar duplicación con AudioTrack
            replaceOutputBuffer(0)
        } else {
            // Modo Media3: pasar los bytes directamente al pipeline estándar
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
        }
    }

    override fun onFlush() {
        if (currentEngine == AudioEngineType.OBOE) {
            OboeAudioEngine.stop()
            OboeAudioEngine.start()
        }
    }

    override fun onReset() {
        OboeAudioEngine.stop()
    }
}
