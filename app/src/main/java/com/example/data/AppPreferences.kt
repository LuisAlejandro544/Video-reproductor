package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.AudioEngineType
import com.example.model.GraphicsEngineType
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

    /**
     * Indica si el usuario ya completó el asistente inicial de bienvenida y configuración.
     * Por defecto es false en la primera ejecución.
     */
    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    /**
     * Motor gráfico de renderizado preferido (OPENGL_ES o VULKAN).
     * Por defecto es OPENGL_ES (máxima estabilidad y efectos de imagen completos).
     */
    var selectedGraphicsEngine: GraphicsEngineType
        get() {
            val stored = prefs.getString(KEY_GRAPHICS_ENGINE, GraphicsEngineType.OPENGL_ES.name)
            return try {
                GraphicsEngineType.valueOf(stored ?: GraphicsEngineType.OPENGL_ES.name)
            } catch (_: Throwable) {
                GraphicsEngineType.OPENGL_ES
            }
        }
        set(value) {
            prefs.edit().putString(KEY_GRAPHICS_ENGINE, value.name).apply()
        }

    /**
     * Define si la aplicación escanea carpetas de aplicaciones de mensajería (WhatsApp, Telegram)
     * para indexar videos automáticamente en la biblioteca, o si opera en modo privado (solo importar).
     */
    var scanMessagingApps: Boolean
        get() = prefs.getBoolean(KEY_SCAN_MESSAGING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SCAN_MESSAGING, value).apply()
        }

    /**
     * Activa o desactiva la retroalimentación auditiva (clicks sutiles al pulsar botones y controles).
     * Habilitado por defecto para una experiencia táctil prémium.
     */
    var isSoundEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_EFFECTS_ENABLED, value).apply()
        }

    /**
     * Reinicia el estado de bienvenida para permitir ejecutar el asistente nuevamente.
     */
    fun resetOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "nova_player_user_prefs"
        private const val KEY_AUDIO_ENGINE = "pref_selected_audio_engine"
        private const val KEY_THEME_MODE = "pref_app_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_use_dynamic_color"
        private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        private const val KEY_GRAPHICS_ENGINE = "pref_selected_graphics_engine"
        private const val KEY_SCAN_MESSAGING = "pref_scan_messaging_apps"
        private const val KEY_SOUND_EFFECTS_ENABLED = "pref_sound_effects_enabled"

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
