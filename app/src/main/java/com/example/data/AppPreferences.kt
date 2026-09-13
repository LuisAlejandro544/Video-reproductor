package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.AudioEngineType
import com.example.ui.theme.AppThemeMode

/**
 * AppPreferences - Administrador central de configuración y preferencias persistentes en disco.
 *
 * Utiliza SharedPreferences para almacenar de manera atómica y duradera los ajustes del usuario,
 * garantizando que las preferencias (motor de audio, modo de tema oscuro/claro/sistema y Material You)
 * se mantengan intactas incluso tras cerrar o reiniciar la aplicación.
 *
 * Por defecto, el motor de audio inicial es Media3 (AudioTrack estándar de Android)
 * y el tema visual se sincroniza con el sistema (AppThemeMode.SYSTEM) con Material You activado.
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

    /**
     * Modo de tema visual (SYSTEM, LIGHT, DARK). Por defecto se sincroniza con el sistema.
     */
    var appThemeMode: AppThemeMode
        get() {
            val storedName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
            return try {
                AppThemeMode.valueOf(storedName ?: AppThemeMode.SYSTEM.name)
            } catch (_: Throwable) {
                AppThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    /**
     * Activa o desactiva la paleta dinámica Material You basada en el fondo de pantalla (Android 12+).
     * Por defecto está habilitado (true).
     */
    var useDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "nova_player_user_prefs"
        private const val KEY_AUDIO_ENGINE = "pref_selected_audio_engine"
        private const val KEY_THEME_MODE = "pref_app_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_use_dynamic_color"

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
