package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.AudioEngineType

/**
 * AppPreferences - Administrador central de configuración y preferencias persistentes en disco.
 *
 * Utiliza SharedPreferences para almacenar de manera atómica y duradera los ajustes del usuario,
 * garantizando que las preferencias (como el motor de audio seleccionado: Media3 o Google Oboe)
 * se mantengan intactas incluso tras cerrar o reiniciar la aplicación.
 *
 * Por defecto, el motor de audio inicial es Media3 (AudioTrack estándar de Android)
 * para asegurar máxima compatibilidad de sincronización A/V y Bluetooth.
 */
class AppPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Motor de audio configurado por el usuario (MEDIA3 por defecto).
     */
    var selectedAudioEngine: AudioEngineType
        get() {
            val storedName = prefs.getString(KEY_AUDIO_ENGINE, AudioEngineType.MEDIA3.name)
            return try {
                AudioEngineType.valueOf(storedName ?: AudioEngineType.MEDIA3.name)
            } catch (_: Throwable) {
                AudioEngineType.MEDIA3
            }
        }
        set(value) {
            prefs.edit().putString(KEY_AUDIO_ENGINE, value.name).apply()
        }

    companion object {
        private const val PREFS_NAME = "nova_player_user_prefs"
        private const val KEY_AUDIO_ENGINE = "pref_selected_audio_engine"

        @Volatile
        private var instance: AppPreferences? = null

        /**
         * Obtiene la instancia singleton de AppPreferences.
         */
        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
