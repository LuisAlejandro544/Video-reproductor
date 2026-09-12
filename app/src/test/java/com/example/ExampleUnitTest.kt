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
}
