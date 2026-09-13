package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Formatos y Compatibilidad Multimedia (FormatsSubScreen.kt)
 *
 * Propósito:
 * Muestra de manera clara y accesible para el usuario todos los formatos de archivo,
 * códecs de video, pistas de audio y subtítulos que Nova Video Player es capaz de
 * decodificar nativamente y acelerar por hardware en su procesador.
 *
 * Esta pantalla sustituye la antigua tarjeta estática de la pantalla principal,
 * proporcionando una guía completa y organizada en categorías sin estorbar la biblioteca.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormatsSubScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Formatos y Compatibilidad",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Contenedores, códecs y aceleración por GPU",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subscreen_formats_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta Hero: Aceleración por Hardware y Fluidez
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Decodificación por Hardware",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Los videos se reproducen directamente en el chip gráfico (GPU) de tu teléfono, ahorrando batería y evitando sobrecalentamiento.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Categoría 1: Formatos de Video (Contenedores)
            FormatCategoryCard(
                icon = Icons.Default.Movie,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Formatos de Video Soportados",
                description = "Formatos de archivos de video que puedes abrir e importar directamente desde tu almacenamiento interno o tarjeta SD:",
                chips = listOf("MP4", "MKV", "WebM", "AVI", "MOV", "TS / M2TS", "3GP", "FLV"),
                details = listOf(
                    "MP4: Formato universal para grabaciones de cámara, streaming y redes.",
                    "MKV: Ideal para películas y series con múltiples pistas de audio y subtítulos.",
                    "WebM: Formato de alta compresión comúnmente descargado de la web.",
                    "MOV / AVI / TS: Compatibilidad amplia para videos importados de PC y cámaras."
                )
            )

            // Categoría 2: Códecs de Video
            FormatCategoryCard(
                icon = Icons.Default.CheckCircle,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Códecs de Video (Compresión)",
                description = "Nova Player aprovecha los decodificadores internos del procesador para reproducir:",
                chips = listOf("H.264 (AVC)", "H.265 (HEVC)", "VP9", "AV1 (Zero-Copy)"),
                details = listOf(
                    "H.264 / AVC: Máxima compatibilidad en cualquier teléfono Android.",
                    "H.265 / HEVC: Alta definición en resoluciones Full HD y 4K con poco espacio.",
                    "AV1 & VP9: Los formatos abiertos más eficientes para transmisiones en alta calidad."
                )
            )

            // Categoría 3: Códecs de Audio
            FormatCategoryCard(
                icon = Icons.Default.Audiotrack,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Formatos y Pistas de Audio",
                description = "Compatibilidad con pistas estéreo, multicanal y sonido envolvente:",
                chips = listOf("AAC", "Opus", "MP3", "FLAC (Hi-Res)", "AC-3 (Dolby)", "E-AC-3", "Vorbis / OGG"),
                details = listOf(
                    "AAC / Opus: Audio nítido con consumo de recursos casi imperceptible.",
                    "FLAC: Reproducción de audio sin pérdidas (Lossless) con rango dinámico completo.",
                    "AC-3 / E-AC-3: Decodificación y mezcla inteligente de sonido envolvente de películas."
                )
            )

            // Categoría 4: Formatos de Subtítulos
            FormatCategoryCard(
                icon = Icons.Default.Subtitles,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Subtítulos Integrados y Externos",
                description = "Carga automática de archivos adjuntos y pistas incrustadas en el contenedor:",
                chips = listOf(".SRT (SubRip)", ".VTT (WebVTT)", ".ASS / .SSA (Estilizado)"),
                details = listOf(
                    "SRT: El estándar universal de subtítulos de texto plano.",
                    "VTT: Subtítulos modernos con marcas de tiempo precisas.",
                    "ASS / SSA: Procesamiento de estilos tipográficos avanzados acelerado en Rust."
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta para representar cada categoría de compatibilidad multimedia.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatCategoryCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    chips: List<String>,
    details: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            // Chips con los formatos
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                chips.forEach { item ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Puntos explicativos
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                details.forEach { detail ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                        Text(
                            text = detail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
