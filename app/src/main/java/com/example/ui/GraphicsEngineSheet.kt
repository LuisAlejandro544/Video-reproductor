package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GraphicsEngineType
import com.example.vulkan.NativeVulkanVideoEngine
import com.example.vulkan.VulkanCapabilities

/**
 * GraphicsEngineSheet.kt - Selector de Motor Gráfico (OpenGL ES vs Vulkan 1.1+)
 *
 * Propósito:
 * Permite al usuario seleccionar el motor de renderizado de video en tiempo de ejecución,
 * visualizando en vivo las capacidades de hardware detectadas en su dispositivo (GPU, versión de Vulkan,
 * soporte de Zero-Copy AHardwareBuffer y estado del Fallback Automático).
 *
 * Licencia: Apache 2.0. Compatible con 32 y 64 bits (minSdk 26).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsEngineSheet(
    currentEngine: GraphicsEngineType,
    onEngineSelected: (GraphicsEngineType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vulkanStatus = remember { VulkanCapabilities.checkCapabilities(context) }
    val isAhbSupported = remember { NativeVulkanVideoEngine.isHardwareBufferSupported() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161A),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("graphics_engine_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Motor Gráfico de Video",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Aceleración por GPU y gestión de pantalla",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta Informativa de Hardware y Vulkan
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F28)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "GPU: ${vulkanStatus.deviceName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Versión Vulkan: ${vulkanStatus.apiVersionString} (${vulkanStatus.hardwareLevelString})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (vulkanStatus.isVulkan11OrHigher) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "• Zero-Copy (AHardwareBuffer): ${if (isAhbSupported) "Disponible (Memoria compartida)" else "Modo estándar SurfaceView"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isAhbSupported) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "• Shaders: Bytecode SPIR-V nativo compilado en C++",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opción 1: OpenGL ES 3.0+
            GraphicsEngineOptionCard(
                title = GraphicsEngineType.OPENGL_ES.displayName,
                subtitle = "Máxima compatibilidad y shaders maduros (Anime4K, FSR, Pillarbox Blur)",
                badge = "Estable",
                badgeColor = Color(0xFF10B981),
                icon = Icons.Default.Shield,
                isSelected = currentEngine == GraphicsEngineType.OPENGL_ES,
                onClick = {
                    onEngineSelected(GraphicsEngineType.OPENGL_ES)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Opción 2: Vulkan 1.1+
            GraphicsEngineOptionCard(
                title = GraphicsEngineType.VULKAN.displayName,
                subtitle = if (vulkanStatus.isVulkan11OrHigher)
                    "Menor latencia de CPU, Swapchain directo y Zero-Copy con fallback automático"
                else
                    "Vulkan 1.1+ no soportado en este chip (se activará fallback automático a OpenGL ES)",
                badge = if (vulkanStatus.isVulkan11OrHigher) "Bajo nivel" else "Incompatible",
                badgeColor = if (vulkanStatus.isVulkan11OrHigher) Color(0xFF8B5CF6) else Color(0xFFEF4444),
                icon = Icons.Default.Speed,
                isSelected = currentEngine == GraphicsEngineType.VULKAN,
                onClick = {
                    onEngineSelected(GraphicsEngineType.VULKAN)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GraphicsEngineOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF2E2E38)
    val bgColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.12f) else Color(0xFF1C1C24)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF2A2A38),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
