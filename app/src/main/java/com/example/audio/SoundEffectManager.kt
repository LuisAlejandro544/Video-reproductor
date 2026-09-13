package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.R
import com.example.data.AppPreferences

/**
 * SoundEffectManager.kt - Gestor centralizado de retroalimentación sonora y efectos de UI.
 *
 * Arquitectura y funcionamiento:
 * - Utiliza [SoundPool] de Android para precargar micro-efectos sonoros en memoria RAM y reproducirlos
 *   con latencia cercana a 0 ms sin interferir con la pista de audio principal del reproductor de video.
 * - Carga el recurso comprimido en formato Ogg Vorbis [R.raw.ui_click] a 48 kHz (generado mediante
 *   el script `scripts/convert_audio_asset.sh`).
 * - Respeta de forma estricta la preferencia [AppPreferences.isSoundEffectsEnabled] para silenciarse
 *   automáticamente cuando el usuario prefiera una experiencia visual silenciosa.
 */
class SoundEffectManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = AppPreferences.getInstance(appContext)

    private val soundPool: SoundPool
    private var clickSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                if (sampleId == clickSoundId) {
                    isLoaded = true
                }
            } else {
                Log.w("SoundEffectManager", "Error al cargar sonido en SoundPool: estado $status")
            }
        }

        loadSounds()
    }

    /**
     * Carga el efecto de sonido de click de interfaz desde los recursos raw.
     */
    private fun loadSounds() {
        try {
            clickSoundId = soundPool.load(appContext, R.raw.ui_click, 1)
        } catch (e: Throwable) {
            Log.w("SoundEffectManager", "No se pudo cargar R.raw.ui_click: ${e.message}")
        }
    }

    /**
     * Reproduce el sonido de click táctil si los efectos de sonido están habilitados por el usuario.
     *
     * @param volume Volumen relativo del sonido (0.0f a 1.0f).
     * @param pitch Tono/velocidad del sonido (1.0f es tono original, 1.05f ligeramente más agudo).
     */
    fun playClickSound(volume: Float = 0.7f, pitch: Float = 1.0f) {
        if (!preferences.isSoundEffectsEnabled) {
            return
        }

        if (isLoaded && clickSoundId != 0) {
            try {
                soundPool.play(clickSoundId, volume, volume, 1, 0, pitch)
            } catch (e: Throwable) {
                Log.w("SoundEffectManager", "Fallo al reproducir click de sonido: ${e.message}")
            }
        }
    }

    /**
     * Libera los recursos nativos de SoundPool cuando la aplicación finaliza.
     */
    fun release() {
        try {
            soundPool.release()
            isLoaded = false
            clickSoundId = 0
        } catch (_: Throwable) {}
    }

    companion object {
        @Volatile
        private var instance: SoundEffectManager? = null

        /**
         * Obtiene o crea la instancia singleton de [SoundEffectManager].
         */
        fun getInstance(context: Context): SoundEffectManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context).also { instance = it }
            }
        }
    }
}
