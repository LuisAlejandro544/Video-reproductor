package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
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
 * - Configurado con [AudioAttributes.USAGE_MEDIA] y [AudioAttributes.CONTENT_TYPE_SONIFICATION]
 *   para garantizar su reproducción a través del canal multimedia (STREAM_MUSIC), evitando que
 *   el audio sea silenciado por la configuración global del sistema de "sonidos táctiles" o el modo vibración.
 * - Carga el recurso [R.raw.ui_click] a 48 kHz mono con duración calibrada de 42 ms.
 * - Cuenta con un mecanismo de respaldo automático (fallback) mediante [AudioManager.playSoundEffect]
 *   en caso de que SoundPool aún se encuentre cargando o falle la asignación de canales.
 * - Respeta de forma estricta la preferencia [AppPreferences.isSoundEffectsEnabled] para silenciarse
 *   automáticamente cuando el usuario prefiera una experiencia visual silenciosa.
 */
class SoundEffectManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = AppPreferences.getInstance(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val soundPool: SoundPool
    private var clickSoundId: Int = 0
    @Volatile
    private var isLoaded: Boolean = false

    init {
        // Enrutamiento directo al flujo multimedia (STREAM_MUSIC) para que la respuesta auditiva sea
        // audible e inmune a las restricciones de "sonidos del sistema" de Android.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                if (sampleId == clickSoundId) {
                    isLoaded = true
                }
            } else {
                Log.w("SoundEffectManager", "Aviso en carga de SoundPool: estado $status")
            }
        }

        loadSounds()
    }

    /**
     * Carga el efecto de sonido de click de interfaz desde los recursos raw.
     */
    private fun loadSounds() {
        try {
            val afd = appContext.resources.openRawResourceFd(R.raw.ui_click)
            if (afd != null) {
                clickSoundId = soundPool.load(afd, 1)
                afd.close()
            } else {
                clickSoundId = soundPool.load(appContext, R.raw.ui_click, 1)
            }
        } catch (e: Throwable) {
            try {
                clickSoundId = soundPool.load(appContext, R.raw.ui_click, 1)
            } catch (ex: Throwable) {
                Log.w("SoundEffectManager", "Fallo al precargar audio en SoundPool: ${ex.message}")
            }
        }
    }

    /**
     * Reproduce el sonido de click táctil si los efectos de sonido están habilitados por el usuario.
     *
     * @param volume Volumen relativo del sonido (0.0f a 1.0f).
     * @param pitch Tono/velocidad del sonido (1.0f es tono original).
     */
    fun playClickSound(volume: Float = 0.85f, pitch: Float = 1.0f) {
        if (!preferences.isSoundEffectsEnabled) {
            return
        }

        var playedSuccessfully = false
        if (isLoaded && clickSoundId != 0) {
            try {
                val streamId = soundPool.play(clickSoundId, volume, volume, 1, 0, pitch)
                if (streamId != 0) {
                    playedSuccessfully = true
                }
            } catch (e: Throwable) {
                Log.w("SoundEffectManager", "Excepción al reproducir en SoundPool: ${e.message}")
            }
        }

        // Si SoundPool aún no terminó de cargar o no pudo asignar canal nativo, fallback a AudioManager
        if (!playedSuccessfully) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, volume)
            } catch (_: Throwable) {}
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
