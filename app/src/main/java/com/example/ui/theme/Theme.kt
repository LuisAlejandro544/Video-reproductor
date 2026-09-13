package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Paleta de Modo Oscuro para Nova Video Player
 *
 * Optimizada para sesiones de cine y visualización nocturna en pantallas OLED/AMOLED.
 * Utiliza negros y grises azulados profundos (#0B0F17, #131924) para ahorrar batería
 * y reducir el deslumbramiento, complementado con acentos ámbar y cian.
 */
private val DarkColorScheme =
  darkColorScheme(
    primary = PlayerAmber,
    onPrimary = Color(0xFF1E1000),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFD8B4),
    secondary = PlayerCyan,
    onSecondary = Color(0xFF002236),
    secondaryContainer = Color(0xFF004D73),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = PlayerAmberVariant,
    onTertiary = Color.White,
    background = PlayerDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = PlayerDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = PlayerDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = PlayerDarkOutline,
    outlineVariant = Color(0xFF1E293B)
  )

/**
 * Paleta de Modo Claro para Nova Video Player
 *
 * Proporciona un entorno luminoso, limpio y de alto contraste para uso bajo luz solar o en exteriores.
 * Conserva la identidad visual del reproductor mediante tonos ámbar cálidos y cian con fondo blanco/marfil suave.
 */
private val LightColorScheme =
  lightColorScheme(
    primary = PlayerAmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = PlayerCyanVariant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = PlayerAmberVariant,
    onTertiary = Color.White,
    background = PlayerLightBg,
    onBackground = PlayerLightOnSurface,
    surface = PlayerLightSurface,
    onSurface = PlayerLightOnSurface,
    surfaceVariant = PlayerLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = PlayerLightOutline,
    outlineVariant = Color(0xFFE2E8F0)
  )

/**
 * Tema principal de Nova Video Player
 *
 * Características:
 * 1. Material You (Color Dinámico): En Android 12+ (API 31+), extrae la paleta tonal del fondo de pantalla
 *    del dispositivo del usuario si [dynamicColor] está activo.
 * 2. Modos de Tema: Soporta sincronización automática con el sistema (AppThemeMode.SYSTEM),
 *    o forzado manual en Claro (AppThemeMode.LIGHT) u Oscuro (AppThemeMode.DARK).
 * 3. Aislamiento de escala de fuente: Fija [fontScale] a 1.0f para garantizar que la diagramación
 *    y proporciones visuales de controles, subtítulos y paneles se mantengan perfectas.
 */
@Composable
fun MyApplicationTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  darkTheme: Boolean = when (themeMode) {
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  },
  // Dynamic color (Material You) está disponible a partir de Android 12 (API 31)
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  // Aislamiento de escala de tipografía: la aplicación mantiene su propio tamaño de fuente óptimo (fontScale = 1.0f)
  // sin verse afectada por configuraciones externas de tamaño de letra del sistema operativo del usuario.
  val currentDensity = LocalDensity.current
  val fixedFontDensity = Density(
    density = currentDensity.density,
    fontScale = 1.0f
  )

  CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

