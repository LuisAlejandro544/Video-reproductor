package com.example

import com.example.audio.AudioEngineType
import com.example.opengl.VideoEqualizerState
import com.example.ui.AspectRatioMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias locales para la lógica de dominio y estados por defecto.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun defaultVideoEqualizerState_isNeutral() {
        val defaultState = VideoEqualizerState.DEFAULT
        assertEquals(0.0f, defaultState.brightness, 0.001f)
        assertEquals(1.0f, defaultState.contrast, 0.001f)
        assertEquals(1.0f, defaultState.saturation, 0.001f)
        assertEquals(1.0f, defaultState.gamma, 0.001f)
        assertEquals(0.0f, defaultState.sharpness, 0.001f)
    }

    @Test
    fun audioEngineTypes_areDefined() {
        val oboeType = AudioEngineType.OBOE
        val media3Type = AudioEngineType.MEDIA3
        assertNotEquals(oboeType, media3Type)
    }

    @Test
    fun aspectRatioModes_areDefined() {
        assertEquals(3, AspectRatioMode.values().size)
    }

    @Test
    fun oboeAudioProcessor_configuresAndFlushesSafely() {
        val processor = com.example.audio.OboeAudioProcessor()
        processor.setEngine(AudioEngineType.OBOE)
        assertEquals(AudioEngineType.OBOE, processor.getCurrentEngine())

        // Configuración de formato PCM 48kHz estéreo 16-bit
        val inputFormat = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            48000, 2, androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        val outputFormat = processor.configure(inputFormat)
        assertEquals(inputFormat.sampleRate, outputFormat.sampleRate)
        assertEquals(inputFormat.channelCount, outputFormat.channelCount)

        // flush no debe lanzar excepciones
        processor.flush()
        processor.reset()
    }
}
