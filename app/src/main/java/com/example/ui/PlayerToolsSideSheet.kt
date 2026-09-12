package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Identificadores de las herramientas independientes disponibles en el reproductor.
 */
enum class PlayerToolItem {
    LOCK,
    PLAYBACK_SPEED,
    VIDEO_EQUALIZER,
    SUN_MODE,
    PILLARBOX_BLUR,
    FSR_SUPER_RESOLUTION,
    VOICE_NIGHT_AUDIO,
    AUDIO_ENGINE,
    SUBTITLES,
    ASPECT_RATIO
}

/**
 * PlayerToolsSideSheet - Panel lateral derecho de navegación de herramientas.
 *
 * Inspirado en la interfaz solicitada por el usuario (estilo panel lateral semitransparente derecho).
 * Desacopla la navegación de las herramientas para que cada una viva en su pantalla exclusiva
 * sin saturar la barra de reproducción ni mezclar configuraciones dispares.
 *
 * @param visible Indica si el menú lateral está desplegado.
 * @param onDismiss Callback cuando el usuario toca fuera del panel o lo cierra.
 * @param onSelectTool Callback que se dispara al seleccionar una herramienta específica.
 */
@Composable
fun PlayerToolsSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectTool: (PlayerToolItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_tools_overlay")
    ) {
        // Fondo atenuado en el área restante del video (al pulsar cierra el menú)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Panel lateral deslizante anclado a la derecha de la pantalla
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = Color(0xEE141419),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 240.dp, max = 300.dp)
                    .testTag("player_tools_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp)
                ) {
                    // Cabecera del panel de herramientas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Herramientas",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_tools_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar herramientas",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.12f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Lista de herramientas con scroll vertical si la pantalla es reducida
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Bloquear controles táctiles
                        ToolMenuItem(
                            icon = Icons.Default.Lock,
                            label = "Bloquear",
                            description = "Evita toques involuntarios",
                            testTag = "tool_item_lock",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.LOCK)
                            }
                        )

                        // 2. Velocidad de reproducción
                        ToolMenuItem(
                            icon = Icons.Default.Speed,
                            label = "Velocidad de reproducción",
                            description = "De 0.25x hasta 2.0x",
                            testTag = "tool_item_speed",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.PLAYBACK_SPEED)
                            }
                        )

                        // 3. Ecualizador de video exclusivo (color, brillo, contraste)
                        ToolMenuItem(
                            icon = Icons.Default.Tune,
                            label = "Ecualizador",
                            description = "Color, brillo y contraste",
                            testTag = "tool_item_equalizer",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.VIDEO_EQUALIZER)
                            }
                        )

                        // 4. Modo Sol Extremo y Accesibilidad de Alto Contraste
                        ToolMenuItem(
                            icon = Icons.Default.WbSunny,
                            label = "Modo Sol Extremo",
                            description = "Máxima visibilidad en exteriores y accesibilidad",
                            testTag = "tool_item_sun_mode",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.SUN_MODE)
                            }
                        )

                        // 5. Relleno de fondo desenfocado para videos verticales (Pillarbox Blur)
                        ToolMenuItem(
                            icon = Icons.Default.BlurOn,
                            label = "Relleno de fondo",
                            description = "Desenfoque para videos verticales",
                            testTag = "tool_item_pillarbox",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.PILLARBOX_BLUR)
                            }
                        )

                        // 6. AMD FidelityFX Super Resolution
                        ToolMenuItem(
                            icon = Icons.Default.AutoAwesome,
                            label = "Super Resolución (FSR)",
                            description = "Escalado y afilado en GPU",
                            testTag = "tool_item_fsr",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.FSR_SUPER_RESOLUTION)
                            }
                        )

                        // 7. Compresor Dinámico / Modo Voces Claras (Night Mode Audio)
                        ToolMenuItem(
                            icon = Icons.Default.GraphicEq,
                            label = "Audio DSP Inteligente",
                            description = "Voces claras y compresor nocturno",
                            testTag = "tool_item_voice_night_audio",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.VOICE_NIGHT_AUDIO)
                            }
                        )

                        // 8. Motor de Audio
                        ToolMenuItem(
                            icon = Icons.Default.Audiotrack,
                            label = "Motor de audio",
                            description = "Google Oboe C++ / Media3",
                            testTag = "tool_item_audio_engine",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.AUDIO_ENGINE)
                            }
                        )

                        // 9. Subtítulos
                        ToolMenuItem(
                            icon = Icons.Default.Subtitles,
                            label = "Subtítulos",
                            description = "Pistas SRT, VTT y tamaño",
                            testTag = "tool_item_subtitles",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.SUBTITLES)
                            }
                        )

                        // 10. Modo de pantalla (Aspect Ratio)
                        ToolMenuItem(
                            icon = Icons.Default.AspectRatio,
                            label = "Relación de aspecto",
                            description = "Ajustar, Zoom o Estirar",
                            testTag = "tool_item_aspect_ratio",
                            onClick = {
                                onDismiss()
                                onSelectTool(PlayerToolItem.ASPECT_RATIO)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente individual de fila para cada opción del menú lateral de herramientas.
 */
@Composable
private fun ToolMenuItem(
    icon: ImageVector,
    label: String,
    description: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            )
        }
    }
}
