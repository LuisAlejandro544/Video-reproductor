# Nova Video Player

> Reproductor multimedia avanzado de alto rendimiento para Android con arquitectura híbrida (Kotlin Jetpack Compose, C++ nativo con Google Oboe y núcleo Rust).

---

## 📋 Descripción General

**Nova Video Player** es un reproductor de video y audio móvil diseñado con los estándares de control, fluidez y flexibilidad de los reproductores clásicos de escritorio. Su arquitectura está orientada al máximo rendimiento en una amplia variedad de dispositivos, desde terminales de gama alta hasta teléfonos con especificaciones ajustadas (Android Go) y procesadores tanto de **32 bits (ARMv7, x86)** como de **64 bits (ARM64, x86_64)**.

---

## ✨ Características Principales

- **Soporte y Gestión de Subtítulos SRT (.srt - SubRip) y WebVTT (.vtt):**
  - Renderizado en tiempo real sincronizado mediante `SubtitleView` sobre la superficie de video OpenGL.
  - Detección automática y selección de pistas de subtítulos internas integradas en contenedores MKV/MP4.
  - Carga e importación de archivos de subtítulos externos (`.srt` y `.vtt`) desde el almacenamiento del dispositivo o tarjeta MicroSD mediante el Storage Access Framework.
  - Panel modal inferior de configuración rápida (`SubtitlesBottomSheet`):
    - Activación y desactivación instantánea de subtítulos.
    - Selector interactivo de pistas disponibles con indicación del idioma o archivo cargado.
    - Personalización de tamaño tipográfico en 4 niveles (Pequeño, Normal, Grande, Extra Grande).
    - Botón de acceso rápido `CC` en la barra de controles con etiqueta visual `CC On` cuando se encuentran activos.
    - Estilizado de alto contraste (texto blanco con borde negro) para legibilidad óptima sobre fondos claros y oscuros.
- **Búfer de Memoria RAM Adaptativo (Protección Anti-OOM para Android Go y Teléfonos Modestos):**
  - Gestión inteligente de memoria en tiempo de ejecución mediante `PlayerLoadControlHelper`:
    - **Perfil Android Go / Modesto (≤ 2.5 GB de RAM o `isLowRamDevice`):** Búfer estricto de 4s a 10s y límite máximo de memoria asignable de 16 MB a 24 MB en `DefaultAllocator`. Previene que el sistema operativo mate la aplicación por el *Low Memory Killer* (LMK) durante la reproducción de videos pesados.
    - **Perfil Estándar / Alto Rendimiento (≥ 3 GB de RAM):** Búfer generoso de 15s a 30s para máxima estabilidad de bitrate y saltos de línea de tiempo instantáneos.
  - Telemetría en la pantalla de configuración que refleja el perfil de búfer asignado, tiempos mínimos/máximos y memoria RAM total del dispositivo.
- **Biblioteca Interactiva de Videos Importados y Vistos (Persistencia Local con Room):**
  - Registro automático y persistente en SQLite (`VideoEntity`, `VideoDao`) de cada video cargado desde la Galería o Gestor de Archivos.
  - Extracción y muestra inmediata de metadatos: **título completo del archivo**, **tamaño** y **duración formateada** (ej. `04:32` o `01:20:15`).
  - Barra de progreso visual interactiva indicando el porcentaje visto y la marca de tiempo de pausa (ej. *En pausa en 02:15* o *Visto completo*).
  - Reproducción o reanudación instantánea con un solo toque directamente desde la posición guardada.
  - Gestión del historial: eliminación de videos individuales o vaciado total mediante confirmación.
- **Ecualizador de Video en Tiempo Real y Efectos Visuales con OpenGL ES (C++ y Shaders GPU):**
  - Postprocesamiento de imagen en tiempo real sin pausas ni interrupciones mediante pipeline gráfico nativo en C++ (`VideoColorEngine`) y textura externa *Zero-Copy* (`GL_TEXTURE_EXTERNAL_OES`).
  - Shaders de fragmentos GLSL ejecutados directamente en los núcleos de sombreado de la GPU.
  - **Filtro de Luz Azul / Modo Descanso Visual (Eye Comfort):**
    - Atenuación selectiva y suave del espectro azul (0% a 100%) con sutil compensación de temperatura ámbar.
    - Reduce la fatiga ocular durante sesiones de visualización prolongadas o en entornos nocturnos.
    - Preset dedicado "Descanso Visual" con un solo toque.
  - **Modo Sol Extremo / Accesibilidad de Alto Contraste (Shader GLSL en GPU):**
    - Algoritmo de transferencia luminosa adaptativa no lineal ejecutado directamente en la GPU (`VideoColorEngine`).
    - Eleva y expande las sombras y áreas empastadas sin sobreexponer las altas luces, compensando el reflejo y la luz ambiental abrasadora sin necesidad de sobrecalentar la pantalla al 100% de brillo manual.
    - Deslizador de intensidad de realce solar (0% a 100%) y perfiles dedicados (*Sol Directo Pleno Día*, *Alto Contraste Accesibilidad*, *Equilibrado al Aire Libre*).
  - **Compresor Dinámico (DRC) y Modo Voces Claras (DSP Nativo en C++ con Google Oboe):**
    - **Filtro Peaking Vocal:** Ganancia selectiva sobre la banda formativa humana (1.5 kHz a 3.5 kHz) para maximizar la inteligibilidad de diálogos y susurros en películas y series.
    - **Compresor Dinámico Nocturno (Dynamic Range Compressor):** Atenuación automática con tiempo de respuesta de microsegundos sobre picos estridentes (explosiones, disparos) mientras eleva pasajes de bajo volumen para disfrutar del cine sin sobresaltos.
    - Cero latencia añadida (0 ms) ejecutado dentro del bucle de callback de audio C++ con punto flotante acelerado por hardware (NEON).
  - **Desenfoque de Fondo para Videos Verticales (Pillarbox Blur):**
    - Sustituye las barras negras laterales generadas al reproducir videos verticales (formato 9:16 o 4:3 en pantallas apaisadas) por una versión ampliada, desenfocada (filtro Gaussiano de 9 toques) y suavemente atenuada del propio video en tiempo real.
    - Ejecutado directamente en GPU mediante doble paso de renderizado sin sobrecarga de decodificación adicional ni lag.
    - Interruptor dinámico para activar o desactivar el efecto instantáneamente según la preferencia del usuario.
  - **AMD FidelityFX™ Super Resolution 1.0 (FSR 1.0):**
    - Algoritmo de escalado y reconstrucción espacial de alta fidelidad adaptado a OpenGL ES:
      - **EASU (Edge-Adaptive Spatial Upsampling):** Análisis direccional de bordes con gradientes de luminancia Rec. 709 para escalar videos de baja resolución minimizando distorsiones y efecto borroso.
      - **RCAS (Robust Contrast-Adaptive Sharpening):** Afilado dinámico dependiente del contraste local con acotamiento de vecindario (*clamping*) para evitar estrictamente artefactos de sobreenfoque (*ringing* o halos).
    - Deslizador de ajuste fino de intensidad RCAS (0% a 100%, con valor sugerido al 75%) y preset directo "Super-Resolución FSR".
  - Controles deslizantes continuos de ajuste fino:
    - **Brillo:** Desplazamiento de luz perceptual (-50% a +50%).
    - **Contraste:** Factor de escala con punto pivote en gris medio (50% a 200%).
    - **Saturación:** Luminancia ponderada estándar Rec. 709 (0% a 200%).
    - **Corrección Gamma:** Curva exponencial de rango dinámico (0.5 a 2.0).
    - **Nitidez (Sharpening):** Realce de bordes acelerado mediante kernel de convolución Laplaciano 3x3.
    - **Filtro Luz Azul:** Factor continuo de calidez y descanso visual (0% a 100%).
  - **Presets de Imagen Instantáneos:** Normal, Descanso Visual, Vívido, Cine, Nocturno, Alto Contraste y Blanco y Negro.
  - Panel inferior moderno e interactivo (`VideoEqualizerSheet`) con botón de restablecimiento rápido.
- **Control de Velocidad de Reproducción (Hasta 2.0x) con Corrección de Tono (Sonic):**
  - Selector de velocidad desde 0.25x hasta un máximo de **2.0x** tanto por presets rápidos como por ajuste fino continuo.
  - Algoritmo de estiramiento temporal *Sonic Pitch Preservation* activo: conserva intacta la tonalidad original de las voces y de los instrumentos musicales, evitando por completo distorsiones acústicas o el efecto "voz de ardilla".
  - Botón dedicado en la barra de controles con etiqueta de velocidad en vivo (`PlaybackSpeedSheet`).
- **Arquitectura Modular de Herramientas del Reproductor (Pantallas Independientes):**
  - Panel lateral interactivo (`PlayerToolsSideSheet`) accesible con un toque desde las barras superior e inferior.
  - Cada herramienta cuenta con su propia pantalla o panel modal independiente y exclusivo para una experiencia enfocada y limpia:
    - **Ecualizador de Video (`VideoEqualizerSheet`):** Calibración de color (brillo, contraste, saturación, gamma), nitidez por convolución y presets de imagen.
    - **Modo Sol Extremo (`SunModeSheet`):** Pantalla independiente de compensación para exteriores bajo luz solar directa y perfiles de alto contraste para accesibilidad.
    - **Relleno Desenfoque Vertical (`PillarboxBlurSheet`):** Pantalla exclusiva para configurar el desenfoque Gaussiano y atenuación de fondo cuando un video vertical se reproduce con bandas negras.
    - **Super-Resolución AMD FSR 1.0 (`FsrUpscaleSheet`):** Pantalla dedicada para activar el escalado espacial adaptativo (EASU) y regular la nitidez dependiente del contraste (RCAS).
    - **Compresor Dinámico y Voces Claras (`VoiceNightAudioSheet`):** Pantalla dedicada para realce de diálogos y compresión de rango dinámico para cine nocturno en DSP C++.
    - **Relación de Aspecto (`AspectRatioSheet`):** Selector independiente de proporción geométrica (*Ajustar*, *Zoom*, *Llenar*).
    - **Motor de Audio (`AudioEngineSheet`):** Selector independiente entre Google Oboe nativo en C++ y Android Media3.
    - **Velocidad de Reproducción (`PlaybackSpeedSheet`):** Panel dedicado de velocidad con *Sonic Pitch Preservation*.
    - **Gestor de Subtítulos (`SubtitlesBottomSheet`):** Configuración de subtítulos internos y externos con ajuste de escala tipográfica.
    - **Bloqueo de Controles (`Lock`):** Modo para inmovilizar gestos y toques accidentales con botón flotante animado de desbloqueo instantáneo.
- **Doble Motor de Audio Seleccionable:**
  - **Google Oboe (Nativo C++):** Motor de ultra baja latencia que interactúa directamente con **AAudio** en Android 8.0+ y realiza fallback automático a **OpenSL ES** en hardware heredado. Elimina microcortes y asegura sincronización estricta A/V.
  - **Media3 (AudioTrack):** Canal estándar de audio del sistema Android para máxima compatibilidad.
- **Importación Dual de Medios:**
  - **Galería Multimedia (Android Photo Picker):** Selección visual rápida sin necesidad de permisos invasivos.
  - **Gestor de Archivos Nativo (Storage Access Framework):** Exploración completa de directorios internos, descargas y tarjetas MicroSD.
- **Controles Multimedia Profesionales (Estilo PC) y Gestos Táctiles:**
  - **Gestos Táctiles con HUD Minimalista:**
    - **Lado Izquierdo (Deslizar vertical):** Control dinámico y directo del brillo de pantalla (1% a 100%).
    - **Lado Derecho (Deslizar vertical):** Control en tiempo real del volumen multimedia físico del dispositivo.
    - **Indicador Flotante Minimalista:** Cápsula estilizada no invasiva con icono dinámico según el nivel, barra vertical graduada y porcentaje numérico que aparece exclusivamente durante el gesto y se oculta automáticamente.
  - Barra de progreso con *scrubbing* en tiempo real.
  - Salto temporal rápido (-10s / +10s).
  - Modos de relación de aspecto instantáneos: *Ajustar (Fit)*, *Zoom (Rellenar)* y *Estirar (Fill)*.
  - Silenciado rápido y control dinámico de volumen.
  - Ocultamiento inteligente de controles tras inactividad táctil.
  - Persistencia de pantalla encendida (*Keep Screen On*) durante la reproducción y restauración automática del brillo al salir.
- **Pantalla Independiente de Configuración y Telemetría:**
  - Pantalla dedicada y desacoplada de diálogos o tarjetas flotantes emergentes.
  - Alternancia 100% real en caliente entre el motor C++ (Google Oboe con AAudio/OpenSL ES) y Android Media3 (AudioTrack).
  - **Prueba de Sonido Real:** Generador de tono senoidal estéreo PCM de 440 Hz integrado para audición física instantánea en el motor seleccionado.
  - **Telemetría Nativa en Tiempo Real:** Diagnóstico en vivo de arquitectura CPU (32/64 bits), backend nativo C++ activo, frecuencia de muestreo (Hz), canales de audio y tramas escritas en buffer.

---

## 🛠️ Requisitos del Sistema

- **Sistema Operativo Mínimo:** Android 8.0 Oreo (**API 26**) o superior.
- **Versión Destino:** Android 14 / 15 (**API 36**).
- **Arquitecturas Compatibles:**
  - 64 bits: `arm64-v8a`, `x86_64`
  - 32 bits: `armeabi-v7a`, `x86`
- **Dispositivos Soportados:** Smartphones, tablets, dispositivos plegables y ediciones **Android Go**.
- **Distribución:** Preparado para instalación vía APK directo en tiendas de terceros (Uptodown, descarga directa, etc.) sin dependencia de servicios propietarios de Google Play.

---

## 🏗️ Stack Tecnológico

| Capa | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Interfaz de Usuario** | Kotlin + Jetpack Compose (Material 3) | UI moderna, fluida y adaptativa con tema oscuro inmersivo. |
| **Canal de Video** | AndroidX Media3 (ExoPlayer 1.5.1) | Decodificación por hardware de codecs universales (H.264, HEVC, AV1, VP9). |
| **Motor de Audio Nativo** | C++17 + Google Oboe 1.9.3 | Procesamiento de audio de ultra baja latencia con AAudio y OpenSL ES. |
| **Motor Gráfico y Postprocesado** | C++17 + OpenGL ES 2.0 / 3.0 (GLSL) | Pipeline de shaders en GPU para ecualizador de video en tiempo real (Zero-Copy OES). |
| **Control de Velocidad** | Sonic Pitch Preservation (Media3) | Time-stretching hasta 2.0x manteniendo tonalidad y timbre acústico natural. |
| **Subtítulos y Cues** | Media3 SubtitleView + SubRip/WebVTT | Renderizado de alta visibilidad, soporte de pistas internas e importación de externos (.srt/.vtt). |
| **Control de Búfer RAM** | `PlayerLoadControlHelper` (ExoPlayer) | Asignación adaptativa de memoria anti-OOM con perfil específico para Android Go. |
| **Núcleo de Alto Rendimiento** | Rust 2021 (`novaplayer_rust`) | Parsing de metadatos, seguridad en memoria y procesamiento concurrente. |

---

## 🚀 Compilación e Instalación

### Requisitos Previos en el Entorno de Desarrollo
- JDK 17 o superior.
- Android SDK (API 26+) con NDK r26d (`26.3.11579264`) y CMake 3.22.1+.
- Rust toolchain con targets Android (`aarch64`, `armv7`, `x86_64`, `i686`).

### Pasos de Compilación

1. **Clonar o descargar el proyecto:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd <CARPETA_DEL_PROYECTO>
   ```

2. **Compilar el APK de depuración (Debug APK):**
   ```bash
   gradle assembleDebug
   ```

3. **Ejecutar la suite de pruebas unitarias:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

4. **Ubicación del APK generado:**
   El paquete instalable resultante se genera en:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🤖 Integración Continua en la Nube (GitHub Actions CI/CD)

El proyecto cuenta con un flujo automatizado en `.github/workflows/build-debug.yml` diseñado para compilar el **APK Debug** directamente en los servidores de GitHub sin depender de un ordenador personal, ideal para desarrolladores y usuarios que operan desde un teléfono móvil.

### Características del Flujo de GitHub Actions:
- **Descarga Completa del Código:** Obtiene todo el repositorio y sus módulos.
- **Entorno Nativo Completo:**
  - Instala **Android NDK r26d** y **CMake 3.22.1** para compilar el código C++ (Google Oboe y Shaders OpenGL ES).
  - Configura el toolchain de **Rust** con los 4 targets Android (`aarch64`, `armv7`, `x86_64`, `i686`) y verifica `rust_core`.
- **Compilación Limpia (SIN CACHÉ):** Las directivas `--no-build-cache` y `cache-disabled: true` obligan a compilar todo desde cero, garantizando que no existan artefactos residuales o binarios desactualizados.
- **Generación Obligatoria de Firma Debug (`scripts/generate_debug_keystore.sh`):**
  - El script crea automáticamente el archivo `debug.keystore` con formato estándar PKCS12 mediante `keytool` antes de que Gradle compile.
  - Elimina cualquier bloqueo o espera interactiva por una firma que no exista en el repositorio.
- **Descarga Directa desde el Teléfono Móvil:**
  - Al terminar la compilación, el archivo APK (`NovaPlayer-Debug-APK`) se publica en la pestaña **Actions > Artifacts** del repositorio en GitHub, listo para descargarse e instalarse directamente en el teléfono sin pasos adicionales.

---

## 📄 Licencia

Este software utiliza componentes de código abierto con licencias permisivas (Apache 2.0 y MIT) que garantizan su uso y distribución sin obligaciones de código abierto forzoso ni copyleft.
