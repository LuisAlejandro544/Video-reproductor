package com.example.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audio.AudioEngineType
import com.example.model.GraphicsEngineType
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.MessagingMediaScanner
import com.example.vulkan.VulkanCapabilities

/**
 * OnboardingScreen.kt - Coordinador Principal del Asistente de Bienvenida y Configuración Inicial.
 *
 * Flujo Guiado:
 * 1. Bienvenida y permisos (READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE).
 * 2. Selección de motor de audio (Media3 vs Google Oboe C++ con pros y contras).
 * 3. Selección de motor gráfico (OpenGL ES vs Vulkan experimental, con advertencia de desarrollo).
 * 4. Elección de tema (Oscuro, Claro, Sistema + Material You dinámico).
 * 5. Escaneo de carpetas de mensajería (WhatsApp y Telegram) vs Modo Privado (solo importar).
 * 6. Resumen de configuración y confirmación final.
 */
@Composable
fun OnboardingScreen(
    initialAudioEngine: AudioEngineType = AudioEngineType.MEDIA3,
    initialGraphicsEngine: GraphicsEngineType = GraphicsEngineType.OPENGL_ES,
    initialThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    initialDynamicColor: Boolean = true,
    initialScanMessaging: Boolean = false,
    onLiveThemeChanged: ((AppThemeMode, Boolean) -> Unit)? = null,
    onComplete: (
        audioEngine: AudioEngineType,
        graphicsEngine: GraphicsEngineType,
        themeMode: AppThemeMode,
        dynamicColor: Boolean,
        scanMessaging: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Estado del paso actual (1 al 6)
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME_PERMISSIONS) }

    // Reinicia automáticamente la posición del scroll al inicio de cada paso para evitar desalineaciones o tarjetas estiradas
    LaunchedEffect(currentStep) {
        scrollState.scrollTo(0)
    }

    // Estados de configuración elegidos por el usuario
    var selectedAudioEngine by remember { mutableStateOf(initialAudioEngine) }
    var selectedGraphicsEngine by remember { mutableStateOf(initialGraphicsEngine) }
    var selectedThemeMode by remember { mutableStateOf(initialThemeMode) }
    var useDynamicColor by remember { mutableStateOf(initialDynamicColor) }
    var scanMessagingApps by remember { mutableStateOf(initialScanMessaging) }

    // Detección de capacidades Vulkan del hardware
    val vulkanStatus = remember { VulkanCapabilities.checkCapabilities(context) }

    // Verificación y solicitud interactiva de permisos
    var hasMediaPermission by remember { mutableStateOf(MessagingMediaScanner.hasMediaPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMediaPermission = results.values.any { it } || MessagingMediaScanner.hasMediaPermission(context)
    }

    val requestPermissions = {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    // Manejo del botón físico o gesto Atrás
    BackHandler(enabled = currentStep.stepIndex > 1) {
        val previousIndex = currentStep.stepIndex - 1
        OnboardingStep.values().find { it.stepIndex == previousIndex }?.let {
            currentStep = it
        }
    }

    // El asistente reacciona dinámicamente en tiempo real a los cambios de tema y Material You seleccionados
    MyApplicationTheme(
        themeMode = selectedThemeMode,
        dynamicColor = useDynamicColor
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Indicador de pasos superior
            StepProgressIndicator(
                currentStepIndex = currentStep.stepIndex,
                totalSteps = OnboardingStep.values().size
            )

            // Contenido dinámico del paso actual con scroll vertical
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.stepIndex > initialState.stepIndex) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboarding_step_animation"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME_PERMISSIONS -> {
                            WelcomePermissionsStep(
                                hasMediaPermission = hasMediaPermission,
                                onRequestPermission = requestPermissions
                            )
                        }
                        OnboardingStep.AUDIO_ENGINE -> {
                            AudioEngineSelectionStep(
                                selectedEngine = selectedAudioEngine,
                                onEngineSelected = { selectedAudioEngine = it }
                            )
                        }
                        OnboardingStep.GRAPHICS_ENGINE -> {
                            GraphicsEngineSelectionStep(
                                selectedEngine = selectedGraphicsEngine,
                                vulkanStatus = vulkanStatus,
                                onEngineSelected = { selectedGraphicsEngine = it }
                            )
                        }
                        OnboardingStep.THEME_COLOR -> {
                            ThemeSelectionStep(
                                selectedThemeMode = selectedThemeMode,
                                useDynamicColor = useDynamicColor,
                                onThemeModeSelected = { newMode ->
                                    selectedThemeMode = newMode
                                    onLiveThemeChanged?.invoke(newMode, useDynamicColor)
                                },
                                onDynamicColorChanged = { newDynamic ->
                                    useDynamicColor = newDynamic
                                    onLiveThemeChanged?.invoke(selectedThemeMode, newDynamic)
                                }
                            )
                        }
                        OnboardingStep.MESSAGING_SCAN -> {
                            MessagingScanStep(
                                scanMessagingApps = scanMessagingApps,
                                onScanMessagingChanged = { scanMessagingApps = it }
                            )
                        }
                        OnboardingStep.SUMMARY -> {
                            SummaryStep(
                                hasPermission = hasMediaPermission,
                                selectedAudioEngine = selectedAudioEngine,
                                selectedGraphicsEngine = selectedGraphicsEngine,
                                selectedThemeMode = selectedThemeMode,
                                useDynamicColor = useDynamicColor,
                                scanMessagingApps = scanMessagingApps
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra inferior de navegación de pasos (Atrás / Siguiente / Finalizar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep.stepIndex > 1) {
                    OutlinedButton(
                        onClick = {
                            val previousIndex = currentStep.stepIndex - 1
                            OnboardingStep.values().find { it.stepIndex == previousIndex }?.let {
                                currentStep = it
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atrás")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (currentStep == OnboardingStep.SUMMARY) {
                    Button(
                        onClick = {
                            onComplete(
                                selectedAudioEngine,
                                selectedGraphicsEngine,
                                selectedThemeMode,
                                useDynamicColor,
                                scanMessagingApps
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("onboarding_finish_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Comenzar a Disfrutar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val nextIndex = currentStep.stepIndex + 1
                            OnboardingStep.values().find { it.stepIndex == nextIndex }?.let {
                                currentStep = it
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("onboarding_next_button")
                    ) {
                        Text(
                            text = "Siguiente",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
}
