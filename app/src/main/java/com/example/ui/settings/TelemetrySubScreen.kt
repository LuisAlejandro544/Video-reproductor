package com.example.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType
import com.example.player.BufferMemoryProfile
import com.example.vulkan.VulkanCapabilities

/**
 * TelemetrySubScreen.kt - Monitoreo de Hardware y Diagnóstico de Búfer
 *
 * Propósito:
 * Inspecciona métricas de bajo nivel en tiempo real:
 * - Tramas PCM y frecuencia de muestreo leídas desde Oboe C++
 * - Estado de memoria RAM y perfil de Android Go
 * - Diagnóstico de subtítulos y decodificación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetrySubScreen(
    selectedEngine: AudioEngineType,
    apiName: String,
    sampleRate: Int,
    channelCount: Int,
    framesWritten: Long,
    memoryProfile: BufferMemoryProfile,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Telemetría y Rendimiento", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Métricas de bajo nivel en tiempo real", fontSize = 12.sp, color = Color(0xFFA855F7))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_telemetry_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B14)
    ) { padding ->
        val context = LocalContext.current
        val vulkanStatus = remember { VulkanCapabilities.checkCapabilities(context) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Diagnóstico en Tiempo Real",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    TelemetryItem("Motor Seleccionado", selectedEngine.title, highlight = true)
                    TelemetryItem("Backend Nativo C++", if (selectedEngine == AudioEngineType.OBOE) apiName else "En espera (Media3 activo)")
                    TelemetryItem("Frecuencia de Muestreo", if (sampleRate > 0) "$sampleRate Hz" else "48000 Hz")
                    TelemetryItem("Canales Activos", if (channelCount > 0) "$channelCount canales" else "2 canales")
                    TelemetryItem("Tramas Escritas en C++", "$framesWritten tramas")
                    TelemetryItem("Arquitectura CPU", Build.SUPPORTED_ABIS.firstOrNull() ?: "Universal")
                    TelemetryItem("Versión SO", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    TelemetryItem("Soporte Nativo AAudio", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "Disponible (API >= 26)" else "No disponible")
                    TelemetryItem("Perfil Búfer RAM", "${memoryProfile.profileName} (${memoryProfile.maxBufferRamMb} MB)", highlight = true)
                    TelemetryItem("Búfer Dinámico", "${memoryProfile.minBufferSec.toInt()}s - ${memoryProfile.maxBufferSec.toInt()}s")
                    TelemetryItem("RAM Total Dispositivo", "${memoryProfile.totalRamMb} MB (Android Go: ${if (memoryProfile.isLowRamDevice) "Sí" else "No"})")
                    TelemetryItem("Motor de Subtítulos", "SRT (SubRip), WebVTT y SSA/ASS (Rust Core)")
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("telemetry_vulkan_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Motor Gráfico & Vulkan 1.1+",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val badgeColor = if (vulkanStatus.isVulkan11OrHigher) {
                            Color(0xFF10B981)
                        } else if (vulkanStatus.isSupported) {
                            Color(0xFFF59E0B)
                        } else {
                            Color(0xFFEF4444)
                        }
                        val badgeText = if (vulkanStatus.isVulkan11OrHigher) {
                            "Vulkan 1.1+ Listo"
                        } else if (vulkanStatus.isSupported) {
                            "Vulkan 1.0 (Básico)"
                        } else {
                            "No Compatible"
                        }
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    TelemetryItem("Pipeline Activo", "OpenGL ES 2.0 / 3.0 (Zero-Copy OES)", highlight = true)
                    TelemetryItem("Versión API Vulkan", vulkanStatus.apiVersionString)
                    TelemetryItem("Nivel de Hardware", vulkanStatus.hardwareLevelString)
                    TelemetryItem("GPU Detectada", vulkanStatus.deviceName)
                    TelemetryItem("Driver de Hardware", vulkanStatus.driverVersionString)
                    TelemetryItem("Enlace Nativo NDK", "libvulkan.so vinculado en CMakeLists", highlight = true)
                    TelemetryItem("Diagnóstico C++", vulkanStatus.nativeDiagnostics)
                }
            }
        }
    }
}

/**
 * Fila de visualización para métricas de telemetría.
 */
@Composable
fun TelemetryItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) Color(0xFF38BDF8) else Color.White
        )
    }
}
