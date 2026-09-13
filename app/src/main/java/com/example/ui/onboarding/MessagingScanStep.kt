package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * MessagingScanStep.kt - Pantalla 5 del Asistente de Bienvenida.
 *
 * Propósito:
 * Permite al usuario decidir el nivel de descubrimiento de medios en su dispositivo:
 * 1. Escaneo Automático de Mensajería (WhatsApp y Telegram):
 *    Inspecciona carpetas de medios conocidas para indexar videos recibidos en la biblioteca.
 * 2. Modo Privado (Solo Importar):
 *    No escanea ninguna carpeta en segundo plano. La biblioteca solo mostrará los videos
 *    que el usuario seleccione explícitamente a través del explorador de archivos o la galería.
 */
@Composable
fun MessagingScanStep(
    scanMessagingApps: Boolean,
    onScanMessagingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Descubrimiento de Videos",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "¿Deseas que Nova Player busque automáticamente videos en tus aplicaciones de mensajería descargadas o prefieres un modo privado sin escaneo?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Opción 1: Escanear WhatsApp y Telegram
        SelectionCardWithProsCons(
            title = "Escanear Mensajería (WhatsApp y Telegram)",
            badgeText = "Detección Automática",
            badgeColor = Color(0xFF10B981),
            icon = Icons.Default.QuestionAnswer,
            isSelected = scanMessagingApps,
            onSelect = { onScanMessagingChanged(true) },
            advantages = listOf(
                "Tus videos recibidos en WhatsApp y Telegram aparecen al instante en la biblioteca.",
                "Te ahorra tener que navegar manualmente por carpetas profundas del almacenamiento.",
                "Actualización rápida de nuevos videos con un solo toque."
            ),
            disadvantages = listOf(
                "Pueden aparecer videos cortos, notas o memes de grupos si no se filtran."
            ),
            testTag = "onboarding_scan_messaging_enabled"
        )

        // Opción 2: Solo Importar (Modo Privado)
        SelectionCardWithProsCons(
            title = "Solo Importar (Modo Privado)",
            badgeText = "Máxima Privacidad",
            badgeColor = Color(0xFF6366F1),
            icon = Icons.Default.Lock,
            isSelected = !scanMessagingApps,
            onSelect = { onScanMessagingChanged(false) },
            advantages = listOf(
                "100% de privacidad: la app no lee ni analiza ninguna carpeta de mensajería.",
                "Tu biblioteca contendrá única y exclusivamente los videos que tú decidas abrir o importar.",
                "Cero consumo de batería o almacenamiento por tareas de escaneo en segundo plano."
            ),
            disadvantages = listOf(
                "Deberás abrir los videos recibidos manualmente mediante el gestor de archivos o la galería."
            ),
            testTag = "onboarding_scan_messaging_disabled"
        )
    }
}
