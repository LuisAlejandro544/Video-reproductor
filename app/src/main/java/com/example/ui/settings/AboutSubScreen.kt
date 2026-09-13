package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AboutSubScreen.kt - Información de Arquitectura, Licencias y Distribución
 *
 * Propósito:
 * Explica las especificaciones técnicas del reproductor:
 * - Soporte nativo para 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64)
 * - Canal de distribución independiente (Uptodown y tiendas de APK de terceros)
 * - Cumplimiento de licencias permisivas de software (Apache 2.0)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSubScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arquitectura y Distribución", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Detalles de portabilidad y compilación", fontSize = 12.sp, color = Color(0xFFF59E0B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_about_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B14)
    ) { padding ->
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Canal de Distribución y Portabilidad", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Nova Video Player está diseñado y optimizado para su distribución independiente y tiendas alternativas (Uptodown / descarga directa de APK). No depende de servicios propietarios de Google Play Services para su núcleo multimedia.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Compilado nativamente para 32-bit (armeabi-v7a, x86) y 64-bit (arm64-v8a, x86_64) con soporte de bajo consumo para dispositivos de recursos contenidos (Android Go).",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Licencias permisivas: todas las bibliotecas utilizadas (Google Oboe, Jetpack Compose, Media3) operan bajo licencia Apache 2.0 garantizando máxima libertad y portabilidad.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
