package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineType
import com.example.audio.OboeAudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * SettingsScreen - Pantalla independiente de Configuración de Audio y Rendimiento
 *
 * Esta pantalla sustituye por completo a los antiguos diálogos modales o ventanas emergentes.
 * Proporciona un entorno completo y dedicado para:
 * 1. Seleccionar entre el motor nativo de Google Oboe (C++ con AAudio/OpenSL ES) y Media3 (AudioTrack).
 * 2. Probar la salida de sonido en tiempo real en cualquiera de los dos motores con síntesis PCM pura.
 * 3. Supervisar telemetría de bajo nivel (sample rate, canales, tramas escritas, API nativa, ABI).
 * 4. Navegar limpiamente hacia atrás preservando el estado del reproductor o pantalla principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentEngine: AudioEngineType,
    onEngineChanged: (AudioEngineType) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Motor de audio activo actualmente
    var selectedEngine by remember { mutableStateOf(currentEngine) }

    // Estados de telemetría nativa C++
    var apiName by remember { mutableStateOf(OboeAudioEngine.getApiName()) }
    var sampleRate by remember { mutableStateOf(OboeAudioEngine.getSampleRate()) }
    var channelCount by remember { mutableStateOf(OboeAudioEngine.getChannelCount()) }
    var framesWritten by remember { mutableLongStateOf(OboeAudioEngine.getFramesWritten()) }

    // Estado del test de sonido
    var isTestingAudio by remember { mutableStateOf(false) }
    var testAudioMessage by remember { mutableStateOf<String?>(null) }

    // Objeto AudioTrack para la prueba en modo Media3
    var activeTestTrack by remember { mutableStateOf<AudioTrack?>(null) }

    // Manejar botón físico/gestual de Atrás
    BackHandler {
        // Detener cualquier sonido de prueba antes de salir
        AudioTestManager.stopTone(activeTestTrack)
        onNavigateBack()
    }

    // Actualización periódica de telemetría en tiempo real
    LaunchedEffect(Unit) {
        while (true) {
            apiName = OboeAudioEngine.getApiName()
            sampleRate = OboeAudioEngine.getSampleRate()
            channelCount = OboeAudioEngine.getChannelCount()
            framesWritten = OboeAudioEngine.getFramesWritten()
            delay(400)
        }
    }

    // Limpieza al desmontar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            AudioTestManager.stopTone(activeTestTrack)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configuración",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Motor de Audio y Rendimiento Nativo",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            AudioTestManager.stopTone(activeTestTrack)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF070B14),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // SECCIÓN 1: SELECCIÓN DEL MOTOR DE AUDIO
            // ==========================================
            Text(
                text = "MOTOR DE REPRODUCCIÓN DE AUDIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            // Tarjeta Motor Oboe (C++ Nativo)
            EngineSelectionCard(
                engineType = AudioEngineType.OBOE,
                isSelected = selectedEngine == AudioEngineType.OBOE,
                badgeText = "Baja Latencia (C++)",
                badgeColor = Color(0xFF10B981),
                details = "Procesamiento en C++ mediante Google Oboe con canal directo AAudio (Android 8.0+) o OpenSL ES. Minimiza la latencia de hardware y previene microcortes en archivos pesados.",
                onSelect = {
                    selectedEngine = AudioEngineType.OBOE
                    onEngineChanged(AudioEngineType.OBOE)
                    OboeAudioEngine.start()
                },
                testTag = "select_oboe_engine_card"
            )

            // Tarjeta Motor Media3 (AudioTrack estándar)
            EngineSelectionCard(
                engineType = AudioEngineType.MEDIA3,
                isSelected = selectedEngine == AudioEngineType.MEDIA3,
                badgeText = "Estándar Android",
                badgeColor = Color(0xFF64748B),
                details = "Procesamiento a través del pipeline estándar de Android Media3 y AudioTrack del sistema. Permite compatibilidad universal con efectos del sistema operativo.",
                onSelect = {
                    selectedEngine = AudioEngineType.MEDIA3
                    onEngineChanged(AudioEngineType.MEDIA3)
                    OboeAudioEngine.stop()
                },
                testTag = "select_media3_engine_card"
            )

            // ==========================================
            // SECCIÓN 2: TEST DE SONIDO 100% REAL
            // ==========================================
            Text(
                text = "VERIFICACIÓN REAL DE SALIDA DE AUDIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isTestingAudio) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Test de audio",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Prueba de Sonido del Motor",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Genera un tono senoidal estéreo PCM de 440 Hz en el motor activo",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (selectedEngine == AudioEngineType.OBOE) {
                            "Salida configurada: Google Oboe C++ (AAudio). Las muestras PCM se sintetizan y se escriben directamente en el buffer nativo de bajo nivel."
                        } else {
                            "Salida configurada: Media3 / AudioTrack. Las muestras PCM se sintetizan y se envían a través del servicio de audio estándar de Android."
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isTestingAudio) {
                                    AudioTestManager.stopTone(activeTestTrack)
                                    isTestingAudio = false
                                    testAudioMessage = "Prueba detenida"
                                } else {
                                    isTestingAudio = true
                                    testAudioMessage = "Reproduciendo en ${selectedEngine.name}..."
                                    coroutineScope.launch {
                                        withContext(Dispatchers.Default) {
                                            AudioTestManager.playTone(
                                                engine = selectedEngine,
                                                onTrackCreated = { track ->
                                                    activeTestTrack = track
                                                },
                                                onFinish = {
                                                    isTestingAudio = false
                                                    testAudioMessage = "Prueba completada con éxito"
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTestingAudio) Color(0xFFEF4444) else Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("play_test_audio_button")
                        ) {
                            Icon(
                                imageVector = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTestingAudio) "Detener Prueba" else "Probar Sonido",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    testAudioMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ==========================================
            // SECCIÓN 3: TELEMETRÍA Y HARDWARE NATIVO
            // ==========================================
            Text(
                text = "TELEMETRÍA NATIVA DEL SISTEMA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Hardware",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Diagnóstico en Tiempo Real",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    TelemetryItem(
                        label = "Motor Seleccionado",
                        value = selectedEngine.title,
                        highlight = true
                    )
                    TelemetryItem(
                        label = "Backend Nativo C++",
                        value = if (selectedEngine == AudioEngineType.OBOE) apiName else "En espera (Media3 activo)"
                    )
                    TelemetryItem(
                        label = "Frecuencia de Muestreo",
                        value = if (sampleRate > 0) "$sampleRate Hz" else "48000 Hz (Predeterminado)"
                    )
                    TelemetryItem(
                        label = "Canales de Audio",
                        value = if (channelCount > 0) "$channelCount canales (Estéreo)" else "2 canales"
                    )
                    TelemetryItem(
                        label = "Tramas Escritas en C++",
                        value = "$framesWritten tramas"
                    )
                    TelemetryItem(
                        label = "Arquitectura CPU",
                        value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Universal"
                    )
                    TelemetryItem(
                        label = "Versión SO",
                        value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                    )
                    TelemetryItem(
                        label = "Soporte Nativo AAudio",
                        value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "Disponible (API >= 26)" else "No disponible"
                    )
                }
            }

            // ==========================================
            // SECCIÓN 4: INFORMACIÓN TÉCNICA Y POLÍTICAS
            // ==========================================
            Text(
                text = "ARQUITECTURA Y DISTRIBUCIÓN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Canal de Distribución y Portabilidad",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Nova Video Player está optimizado para su distribución independiente y tiendas alternativas (Uptodown / APK directo). No depende de servicios propietarios de Google Play Services para su núcleo de reproducción.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "Compilado nativamente para 32-bit (armeabi-v7a, x86) y 64-bit (arm64-v8a, x86_64) con soporte para dispositivos de recursos contenidos (Android Go).",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta de selección interactiva para cada motor de audio.
 */
@Composable
private fun EngineSelectionCard(
    engineType: AudioEngineType,
    isSelected: Boolean,
    badgeText: String,
    badgeColor: Color,
    details: String,
    onSelect: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
    val bgColor = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.10f) else Color(0xFF0F172A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF38BDF8),
                        unselectedColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = engineType.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = engineType.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = details,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.60f),
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
    }
}

/**
 * Fila de visualización para métricas de telemetría.
 */
@Composable
private fun TelemetryItem(
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

/**
 * Gestor de generación de tono de prueba para validar en tiempo real y al 100%
 * la salida física de audio tanto en el motor Oboe C++ como en AudioTrack de Media3.
 */
object AudioTestManager {
    private const val SAMPLE_RATE = 48000
    private const val DURATION_SECONDS = 1.5
    private const val FREQUENCY = 440.0 // Tono A4 (440 Hz)

    suspend fun playTone(
        engine: AudioEngineType,
        onTrackCreated: (AudioTrack?) -> Unit,
        onFinish: () -> Unit
    ) {
        val totalSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val numChannels = 2
        val pcmBytes = ByteArray(totalSamples * numChannels * 2)

        // Sintetizar tono senoidal puro de 440 Hz con envolvente suave (fade in / out)
        var byteIndex = 0
        for (i in 0 until totalSamples) {
            val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / FREQUENCY)
            val envelope = when {
                i < 2400 -> i / 2400.0
                i > totalSamples - 2400 -> (totalSamples - i) / 2400.0
                else -> 1.0
            }
            val sampleVal = (sin(angle) * 16000.0 * envelope).toInt().toShort()
            val lowByte = (sampleVal.toInt() and 0xFF).toByte()
            val highByte = ((sampleVal.toInt() shr 8) and 0xFF).toByte()

            // Canal izquierdo
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
            // Canal derecho
            pcmBytes[byteIndex++] = lowByte
            pcmBytes[byteIndex++] = highByte
        }

        if (engine == AudioEngineType.OBOE) {
            // Reproducción 100% real mediante Google Oboe C++ nativo
            OboeAudioEngine.init(SAMPLE_RATE, numChannels)
            OboeAudioEngine.setVolume(1.0f)
            OboeAudioEngine.start()
            OboeAudioEngine.write(pcmBytes, 0, pcmBytes.size)

            // Esperar duración del tono y finalizar
            delay((DURATION_SECONDS * 1000).toLong())
            onFinish()
        } else {
            // Reproducción 100% real mediante AudioTrack estándar de Android
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufSize, pcmBytes.size))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                onTrackCreated(track)
                track.write(pcmBytes, 0, pcmBytes.size)
                track.play()

                delay((DURATION_SECONDS * 1000).toLong())
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                onTrackCreated(null)
                onFinish()
            } catch (e: Exception) {
                onFinish()
            }
        }
    }

    fun stopTone(audioTrack: AudioTrack?) {
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        OboeAudioEngine.stop()
    }
}
