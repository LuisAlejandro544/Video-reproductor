package com.example.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.example.subtitles.AssSubtitleOverlay
import com.example.subtitles.SubtitleSize
import com.example.subtitles.SubtitleTrackItem

/**
 * PlayerSubtitleLayer.kt - Capa modular de renderizado de subtítulos
 *
 * Responsabilidad:
 * Soporta de forma transparente:
 * 1. Subtítulos complejos SSA / ASS procesados y parseados en Rust Core con estilos de posición,
 *    colores RGB, contornos y tamaños dinámicos.
 * 2. Subtítulos estándar (SRT / WebVTT / pistas embebidas del contenedor) mediante SubtitleView
 *    de AndroidX Media3 con bordes nítidos y tamaño fraccional configurable.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerSubtitleLayer(
    subtitlesEnabled: Boolean,
    externalSubtitle: SubtitleTrackItem?,
    currentCues: List<Cue>,
    currentPositionMs: Long,
    subtitleSize: SubtitleSize,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    if (!subtitlesEnabled) return

    if (externalSubtitle?.assDialogues?.isNotEmpty() == true) {
        // Capa de Subtítulos Complejos SSA/ASS
        AssSubtitleOverlay(
            dialogues = externalSubtitle.assDialogues,
            styles = externalSubtitle.assStyles,
            currentPositionMs = currentPositionMs,
            subtitleSize = subtitleSize,
            showControls = showControls,
            modifier = modifier.fillMaxSize()
        )
    } else if (currentCues.isNotEmpty()) {
        // Capa de Subtítulos estándar (SRT / WebVTT y pistas embebidas)
        AndroidView(
            factory = { ctx ->
                SubtitleView(ctx).apply {
                    setUserDefaultStyle()
                    setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null
                        )
                    )
                    setFractionalTextSize(subtitleSize.fraction)
                }
            },
            update = { view ->
                view.setFractionalTextSize(subtitleSize.fraction)
                view.setCues(currentCues)
            },
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = if (showControls) 96.dp else 28.dp
                )
        )
    }
}
