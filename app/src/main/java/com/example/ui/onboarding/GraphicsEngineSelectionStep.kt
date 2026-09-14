package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GraphicsEngineType
import com.example.vulkan.VulkanStatus

/**
 * GraphicsEngineSelectionStep.kt - Pantalla 3 del Asistente de Bienvenida.
 *
 * Propósito:
 * Permite seleccionar el motor de renderizado gráfico de la GPU:
 * - OpenGL ES 3.0+: Máxima madurez y compatibilidad con todos los efectos de imagen.
 * - Vulkan 1.1+: API de bajo nivel con acceso directo a la GPU.
 *
 * Requisito cumplido:
 * - Se detecta si el hardware soporta Vulkan mediante [vulkanStatus].
 * - Si soporta Vulkan, se ofrecen ambas opciones detallando ventajas y desventajas,
 *   y advirtiendo explícitamente que Vulkan continúa en desarrollo activo de funciones.
 * - Si no soporta Vulkan, se informa claramente y se fija OpenGL ES por defecto.
 */
@Composable
fun GraphicsEngineSelectionStep(
    selectedEngine: GraphicsEngineType,
    vulkanStatus: VulkanStatus,
    onEngineSelected: (GraphicsEngineType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isVulkanSupported = vulkanStatus.isSupported

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Motor Gráfico y Aceleración GPU",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Elige la API gráfica utilizada para dibujar los fotogramas de video y aplicar filtros en la pantalla de tu dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isVulkanSupported) {
            // Hardware compatible con Vulkan detectado
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GPU compatible con Vulkan detectada: ${vulkanStatus.deviceName.ifBlank { "Controlador Vulkan 1.1+" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF047857)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Opción 1: OpenGL ES 3.0+
            SelectionCardWithProsCons(
                title = "OpenGL ES 3.0+",
                badgeText = "Estable y Probado",
                badgeColor = Color(0xFF10B981),
                icon = Icons.Default.VideogameAsset,
                isSelected = selectedEngine == GraphicsEngineType.OPENGL_ES,
                onSelect = { onEngineSelected(GraphicsEngineType.OPENGL_ES) },
                advantages = listOf(
                    "Compatibilidad absoluta con el 100% de dispositivos Android.",
                    "Soporte total de shaders: Sun Mode, FSR, Anime4K y Pillarbox."
                ),
                disadvantages = listOf(
                    "Mayor sobrecarga (overhead) en la CPU para despachar comandos gráficos."
                ),
                testTag = "onboarding_select_opengles"
            )

            // Opción 2: Vulkan 1.1+ (Bajo Nivel)
            SelectionCardWithProsCons(
                title = "Vulkan 1.1+",
                badgeText = "En Desarrollo",
                badgeColor = Color(0xFFF59E0B),
                icon = Icons.Default.Speed,
                isSelected = selectedEngine == GraphicsEngineType.VULKAN,
                onSelect = { onEngineSelected(GraphicsEngineType.VULKAN) },
                advantages = listOf(
                    "Acceso directo al silicio GPU con menor consumo de CPU y batería.",
                    "Pipelines gráficos precompilados de alta velocidad de fotogramas."
                ),
                disadvantages = listOf(
                    "Comportamiento dependiente del controlador Vulkan del fabricante."
                ),
                warningNote = "Aviso: El motor Vulkan está en desarrollo experimental en esta versión.",
                testTag = "onboarding_select_vulkan"
            )
        } else {
            // Hardware sin soporte para Vulkan
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Vulkan No Disponible en este Dispositivo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "El hardware o la versión del sistema de tu dispositivo no cuentan con controlador Vulkan 1.1+. Nova Player utilizará automáticamente el motor OpenGL ES 3.0+, el cual ofrece el 100% de funciones, efectos de cine y compatibilidad perfecta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SelectionCardWithProsCons(
                        title = "OpenGL ES 3.0+ (Predeterminado)",
                        badgeText = "Activo Automático",
                        badgeColor = Color(0xFF10B981),
                        icon = Icons.Default.VideogameAsset,
                        isSelected = true,
                        onSelect = { onEngineSelected(GraphicsEngineType.OPENGL_ES) },
                        advantages = listOf(
                            "Totalmente compatible con tu GPU actual.",
                            "Acceso a todos los modos de ecualizador de video y filtros sin restricciones."
                        ),
                        disadvantages = emptyList(),
                        testTag = "onboarding_opengles_fallback"
                    )
                }
            }
        }
    }
}
