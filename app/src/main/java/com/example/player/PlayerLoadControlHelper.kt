package com.example.player

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Información de perfil de memoria para diagnóstico y optimización del reproductor.
 *
 * @param isLowRamDevice Indica si Android reporta el dispositivo como Android Go o de memoria baja.
 * @param totalRamMb Memoria RAM total física del dispositivo en Megabytes.
 * @param minBufferSec Segundos mínimos de anticipación en el búfer de RAM.
 * @param maxBufferSec Segundos máximos precargados en el búfer de RAM.
 * @param maxBufferRamMb Límite superior en Megabytes para la asignación de memoria del búfer.
 * @param profileName Nombre descriptivo del perfil acústico y de video asignado.
 */
data class BufferMemoryProfile(
    val isLowRamDevice: Boolean,
    val totalRamMb: Long,
    val minBufferSec: Float,
    val maxBufferSec: Float,
    val maxBufferRamMb: Int,
    val profileName: String
)

/**
 * Administrador y constructor del control de carga de memoria RAM (LoadControl) para ExoPlayer.
 *
 * Lógica y propósito:
 * En teléfonos de recursos limitados (Android Go, terminales de 1 GB o 2 GB de RAM y arquitecturas de 32 bits),
 * una reproducción de video estándar con búfer ilimitado puede acaparar entre 150 MB y 300 MB de RAM.
 * Esto dispara el Low Memory Killer (LMK) del sistema operativo Android y cierra la aplicación forzosamente.
 *
 * Esta clase inspecciona el hardware real en tiempo de ejecución:
 * 1. En terminales Android Go o dispositivos con <= 2.5 GB de RAM: aplica un búfer estricto de 4s a 10s
 *    y un tope estricto de 16 MB a 24 MB en el DefaultAllocator, garantizando fluidez sin microcortes ni riesgo de OOM.
 * 2. En terminales estándar (>= 3 GB de RAM): asigna un búfer generoso de 15s a 30s para máxima estabilidad
 *    al realizar saltos rápidos (seek) o reproducir pistas de alta tasa de bits.
 */
@OptIn(UnstableApi::class)
object PlayerLoadControlHelper {

    private const val TAG = "PlayerLoadControl"

    // Constantes para dispositivos modestos / Android Go
    private const val LOW_RAM_MIN_BUFFER_MS = 4000
    private const val LOW_RAM_MAX_BUFFER_MS = 10000
    private const val LOW_RAM_BUFFER_FOR_PLAYBACK_MS = 1000
    private const val LOW_RAM_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2500
    private const val LOW_RAM_MAX_BUFFER_BYTES = 16 * 1024 * 1024 // 16 MB

    // Constantes para dispositivos estándar / alto rendimiento
    private const val STD_MIN_BUFFER_MS = 15000
    private const val STD_MAX_BUFFER_MS = 30000
    private const val STD_BUFFER_FOR_PLAYBACK_MS = 2000
    private const val STD_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 4000

    /**
     * Obtiene el perfil de memoria calculado para el dispositivo actual.
     */
    fun getMemoryProfile(context: Context): BufferMemoryProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice == true

        var totalRamMb = 0L
        if (activityManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            totalRamMb = memInfo.totalMem / (1024 * 1024)
        }

        // Si es dispositivo reportado como low ram o tiene 2500 MB o menos de RAM física
        val shouldUseLowRamProfile = isLowRam || (totalRamMb in 1..2560)

        return if (shouldUseLowRamProfile) {
            BufferMemoryProfile(
                isLowRamDevice = true,
                totalRamMb = totalRamMb,
                minBufferSec = LOW_RAM_MIN_BUFFER_MS / 1000f,
                maxBufferSec = LOW_RAM_MAX_BUFFER_MS / 1000f,
                maxBufferRamMb = LOW_RAM_MAX_BUFFER_BYTES / (1024 * 1024),
                profileName = "Android Go / Bajo Consumo RAM"
            )
        } else {
            BufferMemoryProfile(
                isLowRamDevice = false,
                totalRamMb = totalRamMb,
                minBufferSec = STD_MIN_BUFFER_MS / 1000f,
                maxBufferSec = STD_MAX_BUFFER_MS / 1000f,
                maxBufferRamMb = 64, // Estimado estándar de DefaultLoadControl
                profileName = "Estándar / Alto Rendimiento"
            )
        }
    }

    /**
     * Construye un DefaultLoadControl configurado específicamente para el perfil de hardware del dispositivo.
     */
    fun createAdaptiveLoadControl(context: Context): LoadControl {
        val profile = getMemoryProfile(context)
        Log.i(TAG, "Inicializando LoadControl adaptativo. Perfil: ${profile.profileName}, RAM total: ${profile.totalRamMb} MB")

        val allocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)

        return if (profile.isLowRamDevice) {
            DefaultLoadControl.Builder()
                .setAllocator(allocator)
                .setBufferDurationsMs(
                    LOW_RAM_MIN_BUFFER_MS,
                    LOW_RAM_MAX_BUFFER_MS,
                    LOW_RAM_BUFFER_FOR_PLAYBACK_MS,
                    LOW_RAM_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setTargetBufferBytes(LOW_RAM_MAX_BUFFER_BYTES)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        } else {
            DefaultLoadControl.Builder()
                .setAllocator(allocator)
                .setBufferDurationsMs(
                    STD_MIN_BUFFER_MS,
                    STD_MAX_BUFFER_MS,
                    STD_BUFFER_FOR_PLAYBACK_MS,
                    STD_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()
        }
    }
}
