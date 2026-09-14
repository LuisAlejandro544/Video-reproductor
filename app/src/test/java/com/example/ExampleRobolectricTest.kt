package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEffectManager
import com.example.data.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nova Video Player", appName)
  }

  @Test
  fun `sound effect manager initialization and preferences verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = AppPreferences.getInstance(context)
    val soundManager = SoundEffectManager.getInstance(context)

    assertNotNull(soundManager)
    // Verificar que los efectos de sonido están activos por defecto
    assertEquals(true, preferences.isSoundEffectsEnabled)

    // Ejecutar reproducción segura sin arrojar excepciones
    soundManager.playClickSound(volume = 0.85f, pitch = 1.0f)

    // Desactivar y verificar persistencia
    preferences.isSoundEffectsEnabled = false
    assertEquals(false, preferences.isSoundEffectsEnabled)
    soundManager.playClickSound()

    // Restaurar estado
    preferences.isSoundEffectsEnabled = true
    assertEquals(true, preferences.isSoundEffectsEnabled)
  }
}
