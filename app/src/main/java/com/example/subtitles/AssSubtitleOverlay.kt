package com.example.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rust.AssDialogueItem
import com.example.rust.AssStyleItem

/**
 * AssSubtitleOverlay - Motor de Renderizado en Compose para Subtítulos Complejos SSA/ASS
 *
 * Renderiza en tiempo real los eventos de diálogo procesados nativamente por Rust Core:
 * - Posicionamiento de 9 cuadrantes numpad (1 a 9).
 * - Colores primarios por estilo o por tag `\c&H...&`.
 * - Estilos tipográficos (negrita, cursiva, multilínea `\N`).
 * - Contorno y sombra de alto contraste para máxima legibilidad contra cualquier fondo de video.
 * - Ajuste dinámico de márgenes según la visibilidad de los controles de reproducción.
 */
@Composable
fun AssSubtitleOverlay(
    dialogues: List<AssDialogueItem>,
    styles: List<AssStyleItem> = emptyList(),
    currentPositionMs: Long,
    subtitleSize: SubtitleSize,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    if (dialogues.isEmpty()) return

    // Buscar todos los diálogos activos en la marca de tiempo actual
    val activeDialogues = remember(dialogues, currentPositionMs) {
        dialogues.filter { currentPositionMs in it.startMs..it.endMs }
    }

    if (activeDialogues.isEmpty()) return

    val stylesMap = remember(styles) {
        styles.associateBy { it.name.lowercase() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("ass_subtitle_overlay")
    ) {
        val bottomMargin = if (showControls) 96.dp else 24.dp
        val topMargin = if (showControls) 80.dp else 24.dp
        val sideMargin = 24.dp

        // Agrupar diálogos por su alineación para no superponer textos en la misma coordenada
        val groupedByAlignment = remember(activeDialogues) {
            activeDialogues.groupBy { it.alignment }
        }

        groupedByAlignment.forEach { (alignmentInt, items) ->
            val composeAlignment = when (alignmentInt) {
                1 -> Alignment.BottomStart
                2 -> Alignment.BottomCenter
                3 -> Alignment.BottomEnd
                4 -> Alignment.CenterStart
                5 -> Alignment.Center
                6 -> Alignment.CenterEnd
                7 -> Alignment.TopStart
                8 -> Alignment.TopCenter
                9 -> Alignment.TopEnd
                else -> Alignment.BottomCenter
            }

            val textAlign = when (alignmentInt) {
                1, 4, 7 -> TextAlign.Start
                3, 6, 9 -> TextAlign.End
                else -> TextAlign.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = sideMargin,
                        end = sideMargin,
                        top = topMargin,
                        bottom = bottomMargin
                    ),
                contentAlignment = composeAlignment
            ) {
                Column(
                    horizontalAlignment = when (alignmentInt) {
                        1, 4, 7 -> Alignment.Start
                        3, 6, 9 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                ) {
                    items.forEach { dialogue ->
                        val matchingStyle = stylesMap[dialogue.style.lowercase()]

                        // Calcular color del texto
                        val textColor = remember(dialogue.primaryColor, matchingStyle?.primaryColor) {
                            parseColorOrDefault(dialogue.primaryColor ?: matchingStyle?.primaryColor, Color.White)
                        }

                        // Determinar negrita y cursiva
                        val isBold = dialogue.isBold || (matchingStyle?.bold == true)
                        val isItalic = dialogue.isItalic || (matchingStyle?.italic == true)

                        // Calcular tamaño de fuente
                        val baseSp = when (subtitleSize) {
                            SubtitleSize.SMALL -> 18.sp
                            SubtitleSize.MEDIUM -> 22.sp
                            SubtitleSize.LARGE -> 28.sp
                        }

                        // Sombra y contorno de alto contraste característicos de subtítulos ASS
                        val outlineShadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )

                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = dialogue.plainText,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = baseSp,
                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = textAlign,
                                    shadow = outlineShadow,
                                    lineHeight = (baseSp.value * 1.25f).sp
                                ),
                                modifier = Modifier.widthIn(max = 700.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parsea un código de color Hex (#AARRGGBB o #RRGGBB) con tolerancia a errores.
 */
private fun parseColorOrDefault(hex: String?, defaultColor: Color): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        val clean = hex.removePrefix("#")
        when (clean.length) {
            8 -> {
                val alpha = clean.substring(0, 2).toInt(16)
                val red = clean.substring(2, 4).toInt(16)
                val green = clean.substring(4, 6).toInt(16)
                val blue = clean.substring(6, 8).toInt(16)
                Color(red, green, blue, alpha)
            }
            6 -> {
                val red = clean.substring(0, 2).toInt(16)
                val green = clean.substring(2, 4).toInt(16)
                val blue = clean.substring(4, 6).toInt(16)
                Color(red, green, blue, 255)
            }
            else -> defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}
