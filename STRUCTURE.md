# Estructura del Proyecto (Architecture & Directory Layout)

Este documento detalla la organización de carpetas, responsabilidades de cada módulo y el flujo de comunicación entre Kotlin, C++ y Rust en **Nova Video Player**.

---

## 🌳 Árbol de Directorios

```text
/
├── .gitignore                   # Exclusiones globales (builds, CMake, Rust, llaves)
├── .github/                     # Automatización CI/CD con GitHub Actions
│   └── workflows/
│       └── build-debug.yml      # Flujo de compilación limpia de APK Debug en la nube
├── scripts/                     # Scripts utilitarios del proyecto
│   ├── generate_debug_keystore.sh # Generador no interactivo de 'debug.keystore' para CI/CD
│   └── convert_audio_asset.sh   # Convertidor y optimizador de muestras de audio para la app (FFmpeg -> Ogg Vorbis mono 48kHz)
├── README.md                    # Documentación general del proyecto
├── ROADMAP.md                   # Hoja de ruta y próximos hitos técnicos
├── STRUCTURE.md                 # Mapa de arquitectura y flujo de componentes
├── AI_CONTEXT.md                # Contexto y directrices de ingeniería para asistentes IA
├── AGENTS.md                    # Reglas persistentes y restricciones del agente
├── build.gradle.kts             # Configuración Gradle a nivel de proyecto raíz
├── settings.gradle.kts          # Inclusión de módulos y repositorios
├── gradle/
│   └── libs.versions.toml       # Catálogo centralizado de versiones y dependencias
│
├── rust_core/                   # Módulo Nativo Rust (Subtítulos SSA/ASS y alto rendimiento)
│   ├── Cargo.toml               # Dependencias de Rust (jni, serde, serde_json, regex)
│   └── src/
│       └── lib.rs               # Parser nativo SSA/ASS y exportación JNI (libnova_rust.so)
│
├── app/                         # Módulo principal de Android (Kotlin + C++)
│   ├── .gitignore               # Exclusiones locales de compilación y .cxx
│   ├── build.gradle.kts         # Configuración del módulo de aplicación (NDK, Prefab, CMake)
│   ├── proguard-rules.pro       # Reglas de ofuscación y conservación de JNI
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Manifiesto de permisos y actividades
│       │   │
│       │   ├── cpp/                  # Capa Nativa C++ (Audio Oboe, OpenGL ES y Vulkan 1.1+)
│       │   │   ├── CMakeLists.txt    # Script de compilación CMake (enlaza Oboe, GLESv2, EGL, vulkan, log)
│       │   │   ├── native-lib.cpp    # Puntos de entrada JNI (Oboe, VideoColorEngine y nativeQueryVulkanDriver)
│       │   │   ├── OboeAudioEngine.h # Declaración de la clase del motor de audio Oboe (búfer circular estático, flush() y DSP)
│       │   │   ├── OboeAudioEngine.cpp # Implementación nativa con búfer de anillo estático, vaciado atómico instantáneo y filtros DSP
│       │   │   ├── VideoColorEngine.h # Declaración del motor de sombreadores OpenGL ES (incluye uniforms y texturas FBO)
│       │   │   ├── VideoColorEngine.cpp # Motor modular de procesamiento visual en GPU (GLES 2.0 / 3.0)
│       │   │   └── shaders/          # Catálogo modular de código sombreador en GPU
│       │   │       └── VideoShaders.h # GLSL Shaders centralizados: OES, Color/Nitidez, Pillarbox Blur, AMD FSR 1.0 (EASU+RCAS) y Anime4K
│       │   │
│       │   ├── java/com/example/     # Código fuente Kotlin (UI y Lógica)
│       │   │   ├── MainActivity.kt   # Actividad raíz, orquestador de UI y transiciones cinemáticas AnimatedContent
│       │   │   │
│       │   │   ├── audio/            # Capa de integración de audio nativo y decodificación FFmpeg
│       │   │   │   ├── AudioChannelMode.kt    # Enum: STEREO, MONO, SPATIAL_HAAS (Efecto Haas 3D)
│       │   │   │   ├── AudioEngineType.kt     # Enum: OBOE vs MEDIA3
│       │   │   │   ├── OboeAudioEngine.kt     # Wrapper JNI con flush(), control de volumen y DSP nativo C++
│       │   │   │   ├── OboeAudioProcessor.kt  # Procesador de audio universal Media3/Oboe con soporte DSP en tiempo real (Voces Claras, DRC, Mono, Haas 3D)
│       │   │   │   └── SoundEffectManager.kt  # Gestor de efectos sonoros nativos (SoundPool) para retroalimentación táctil de interfaz
│       │   │   │
│       │   │   ├── opengl/           # Capa de renderizado acelerado por GPU
│       │   │   │   ├── NativeVideoFilter.kt   # Puente JNI con VideoColorEngine en C++
│       │   │   │   ├── VideoEqualizerState.kt # Modelo de parámetros de ecualización, modo sol, descanso visual, pillarbox blur, AMD FSR 1.0 y Anime4kMode
│       │   │   │   └── OpenGLVideoSurface.kt  # GLSurfaceView.Renderer (doble paso con Pillarbox Blur, Modo Sol, AMD FSR 1.0 y Anime4K) y VideoPlayerView
│       │   │   │
│       │   │   ├── vulkan/           # Capa de capacidades e infraestructura Vulkan 1.1+ (Fase 6 y 7)
│       │   │   │   ├── GraphicsEngineType.kt  # Enum: OPENGL_ES vs VULKAN con persistencia
│       │   │   │   └── VulkanCapabilities.kt  # Detección de FEATURE_VULKAN_HARDWARE_VERSION/LEVEL y puente JNI hacia el driver C++
│       │   │   │
│       │   │   ├── data/             # Persistencia local con Room (SQLite v2) y SharedPreferences
│       │   │   │   ├── AppDatabase.kt         # Base de datos Room singleton con esquema v2
│       │   │   │   ├── AppPreferences.kt      # Almacenamiento persistente de configuraciones (motores, tema, bienvenida, mensajería)
│       │   │   │   ├── VideoDao.kt            # Operaciones reactivas DAO con Flow
│       │   │   │   ├── VideoEntity.kt         # Entidad persistente ampliada: progreso y 18 configuraciones por video
│       │   │   │   └── VideoRepository.kt     # Abstracción y operaciones asíncronas
│       │   │   │
│       │   │   ├── rust/             # Capa de integración JNI con núcleo nativo Rust
│       │   │   │   └── NovaRustCore.kt        # Carga de libnova_rust.so y llamadas JNI a parseAssFull
│       │   │   │
│       │   │   ├── subtitles/        # Modelos y capa de renderizado de subtítulos avanzados
│       │   │   │   ├── AssSubtitleOverlay.kt  # Renderizado dinámico Compose de diálogos ASS/SSA (posicionamiento 9 puntos y contornos)
│       │   │   │   └── SubtitleModels.kt      # Modelos serializables de estilos ASS, diálogos y tamaños tipográficos
│       │   │   │
│       │   │   ├── model/            # Modelos de datos
│       │   │   │   └── VideoItem.kt  # Modelo de metadatos de video (URI, nombre, tamaño, duración)
│       │   │   │
│       │   │   ├── player/           # Optimización y control de carga del reproductor
│       │   │   │   └── PlayerLoadControlHelper.kt # Búfer de RAM adaptativo anti-OOM para Android Go y terminales modestos
│       │   │   │
│       │   │   ├── ui/               # Componentes visuales Jetpack Compose
│       │   │   │   ├── Anime4KSheet.kt        # Pantalla exclusiva e independiente de Reconstrucción de Animación Anime4K
│       │   │   │   ├── AspectRatioMode.kt     # Modos de relación de aspecto geométrico (FIT, ZOOM, FILL)
│       │   │   │   ├── AspectRatioSheet.kt    # Pantalla exclusiva e independiente de relación de aspecto
│       │   │   │   ├── AudioEngineSheet.kt    # Pantalla exclusiva e independiente de selección de motor de audio
│       │   │   │   ├── FsrUpscaleSheet.kt     # Pantalla exclusiva e independiente de Super Resolución AMD FSR 1.0
│       │   │   │   ├── MainViewModel.kt       # ViewModel central de la biblioteca e importados con StateFlow de audio
│       │   │   │   ├── PillarboxBlurSheet.kt  # Pantalla exclusiva e independiente de desenfoque de fondo vertical (Pillarbox)
│       │   │   │   ├── PlaybackSpeedSheet.kt  # Pantalla exclusiva e independiente de velocidad de reproducción (hasta 2x)
│       │   │   │   ├── PlayerToolsSideSheet.kt # Panel lateral interactivo con acceso directo a herramientas del reproductor
│       │   │   │   ├── SettingsScreen.kt      # Coordinador principal de configuración con navegación Hub-and-Spoke
│       │   │   │   ├── StereoMonoSheet.kt     # Pantalla exclusiva e independiente de enrutamiento estéreo/mono y efecto Haas 3D
│       │   │   │   ├── SubtitlesBottomSheet.kt # Panel modal de selección de subtítulos internos/externos y tamaño tipográfico
│       │   │   │   ├── SunModeSheet.kt        # Pantalla exclusiva e independiente de Modo Sol Extremo y Accesibilidad en GPU
│       │   │   │   ├── VideoEqualizerSheet.kt # Pantalla exclusiva e independiente de ecualización de video (color, nitidez, descanso)
│       │   │   │   ├── VideoImportScreen.kt   # Coordinador modular de biblioteca e importación de videos con tarjeta de mensajería
│       │   │   │   ├── VideoPlayerScreen.kt   # Coordinador modular de reproducción con ExoPlayer y OpenGL
│       │   │   │   ├── VideoSourceDialog.kt   # Diálogo para alternar Galería / Gestor de archivos
│       │   │   │   ├── VoiceNightAudioSheet.kt # Pantalla interactiva de Audio Inteligente DSP (Voces Claras y DRC para Oboe C++ y Media3)
│       │   │   │   │
│       │   │   │   ├── onboarding/       # Asistente guiado de configuración inicial (Onboarding)
│       │   │   │   │   ├── AudioEngineSelectionStep.kt    # Paso 2: Selección Oboe vs Media3 con pros y contras
│       │   │   │   │   ├── GraphicsEngineSelectionStep.kt # Paso 3: Selección OpenGL ES vs Vulkan 1.1+ adaptativo
│       │   │   │   │   ├── MessagingScanStep.kt           # Paso 5: Elección de escaneo de WhatsApp/Telegram o Privado
│       │   │   │   │   ├── OnboardingComponents.kt        # Componentes UI compartidos (indicador de progreso, tarjeta con pros/contras)
│       │   │   │   │   ├── OnboardingScreen.kt            # Coordinador del flujo completo con animaciones
│       │   │   │   │   ├── OnboardingStep.kt              # Enum de pasos secuenciales del asistente
│       │   │   │   │   ├── SummaryStep.kt                 # Paso 6: Resumen de configuración elegida y confirmación
│       │   │   │   │   ├── ThemeSelectionStep.kt          # Paso 4: Selección de tema y Material You dinámico
│       │   │   │   │   └── WelcomePermissionsStep.kt      # Paso 1: Bienvenida y solicitud interactiva de permisos
│       │   │   │   │
│       │   │   │   ├── library/          # Módulos desacoplados de la biblioteca de medios
│       │   │   │   │   ├── ImportQuickCard.kt    # Tarjeta de importación rápida con selector SAF / Galería
│       │   │   │   │   ├── LibraryDialogs.kt     # Diálogos de confirmación para eliminación individual o vaciado
│       │   │   │   │   ├── LibraryFooterCards.kt # Tarjetas informativas de formatos compatibles y arquitectura
│       │   │   │   │   ├── LibraryHeaders.kt     # Barra superior con acceso a configuración y bienvenida accesible
│       │   │   │   │   └── VideoHistoryCard.kt   # Tarjeta de elemento de video con progreso y acciones directas
│       │   │   │   │
│       │   │   │   ├── player/           # Módulos desacoplados del reproductor de video
│       │   │   │   │   ├── PlayerGestureDetector.kt # Detección de gestos (brillo, volumen, avance 2X, doble toque ±5s)
│       │   │   │   │   ├── PlayerHudIndicators.kt   # Indicadores visuales flotantes dinámicos con animaciones spring y auras neón (HUD ±5s, 2X pill pulsante, medidor brillo/volumen)
│       │   │   │   │   ├── PlayerOrientationHandler.kt # Detección por sensor de hardware (OrientationEventListener)
│       │   │   │   │   └── PlayerOverlayControls.kt # Barras superior e inferior y controles de reproducción centrales
│       │   │   │   │
│       │   │   │   ├── settings/         # Subpantallas independientes de ajustes (Hub-and-Spoke)
│       │   │   │   │   ├── AboutSubScreen.kt        # Licencias permisivas, arquitectura 32/64 bits y distribución APK
│       │   │   │   │   ├── AppearanceSubScreen.kt   # Pantalla exclusiva e independiente de Apariencia, selector de tema y Material You
│       │   │   │   │   ├── AudioChannelsSubScreen.kt # Configuración de enrutamiento estéreo, mono centrado y efecto Haas 3D
│       │   │   │   │   ├── AudioEngineSubScreen.kt  # Configuración detallada de motores (Oboe vs Media3)
│       │   │   │   │   ├── AudioTestManager.kt      # Gestor de sintetizador senoidal de 440 Hz PCM
│       │   │   │   │   ├── AudioTestSubScreen.kt     # Pantalla de prueba acústica de salida física
│       │   │   │   │   ├── FormatsSubScreen.kt       # Pantalla de formatos multimedia y códecs soportados (video, audio, subtítulos)
│       │   │   │   │   ├── SettingsHubView.kt       # Menú principal con tarjetas categorizadas (Hub)
│       │   │   │   │   ├── SettingsSubScreen.kt     # Enums y rutas de subpantallas de ajustes
│       │   │   │   │   └── TelemetrySubScreen.kt     # Diagnóstico en vivo de tramas C++, buffers y perfil de memoria
│       │   │   │   │
│       │   │   │   └── theme/            # Paleta de colores, tipografía, Material You y temas
│       │   │   │       ├── AppThemeMode.kt          # Enum de modos de tema (SYSTEM, LIGHT, DARK)
│       │   │   │       ├── Color.kt                 # Paleta de colores M3 (Light/Dark tokens de alto contraste)
│       │   │   │       ├── Theme.kt                 # Tema M3 con Material You (dynamicColor), selector de tema y escala de fuente aislada fija (fontScale = 1.0f)
│       │   │   │       └── Type.kt                  # Tipografía M3
│       │   │   │
│       │   │   └── utils/            # Utilidades auxiliares
│       │   │       ├── MessagingMediaScanner.kt # Indexación y escaneo de videos en carpetas de WhatsApp y Telegram
│       │   │       ├── SubtitleUtils.kt # Detección de formato MIME (SRT/VTT) y resolución de nombres
│       │   │       └── VideoUtils.kt    # Extracción y formateo seguro de metadatos y duración
│       │   │
│       │   └── res/                  # Recursos de la aplicación (strings, drawables, audio raw)
│       │       ├── raw/
│       │       │   └── ui_click.ogg  # Muestra de sonido optimizada en Ogg Vorbis mono a 48 kHz para clics de UI
│       │       └── values/
│       │           └── strings.xml   # Textos traducibles y nombre de la app
│       │
│       └── test/                     # Pruebas unitarias locales (JVM / Robolectric)
│           └── java/com/example/
│               └── ExampleRobolectricTest.kt
│
└── rust_core/                   # Módulo de procesamiento de alto rendimiento en Rust
    ├── Cargo.toml               # Dependencias de Rust (jni, cfg-if, optimizaciones LTO)
    └── src/
        └── lib.rs               # Punto de entrada JNI_OnLoad y exportación FFI
```

---

## 🔄 Diagrama de Flujo de Datos

```text
[ Video seleccionado (URI de Galería o SAF) ]
                     │
                     ▼
             [ Media3 ExoPlayer ]
                     │
         (Decodificación por Hardware)
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
   [ Tramas de Video ]   [ Tramas de Audio PCM ]
          │                     │
          ▼                     ▼
  [ SurfaceTexture OES ] [ OboeAudioProcessor ]
          │                     │
          ▼                     │
[ OpenGLVideoRenderer ]         │
          │                     │
          ▼                     │
[ NativeVideoFilter JNI ]       │
          │                     │
          ▼                     │
[ VideoColorEngine C++ ]        │
          │                     │
(Shaders GLSL: Brillo,          │
 Contraste, Saturación, Gamma,  │
 Nitidez, Descanso Visual,      │
 Pillarbox Blur y AMD FSR 1.0)  │
          │                     │
          ▼                     │
 [ Pantalla Dispositivo ]       │
                                │
               ┌────────────────┴────────────────┐
               │                                 │
     (Modo Oboe Seleccionado)          (Modo Media3 Seleccionado)
               │                                 │
               ▼                                 ▼
   [ OboeAudioEngine (JNI) ]           [ Android AudioTrack ]
               │                                 │
               ▼                                 ▼
   [ Google Oboe C++ Nativo ]             [ Sistema Android ]
               │                                 │
      ┌────────┴────────┐                        │
      ▼                 ▼                        │
   [ AAudio ]    [ OpenSL ES ]                   │
  (Android 8+)     (Legacy)                      │
      │                 │                        │
      └────────┬────────┘                        │
               ▼                                 ▼
         [ Salida de Audio del Dispositivo (Bocina / Auriculares) ]
```

---

## 🏛️ Decisiones de Diseño y Responsabilidades

1. **Desacoplamiento Estricto:** La interfaz gráfica no interactúa directamente con los punteros de C++; se comunica exclusivamente mediante los singletons tipados `OboeAudioEngine` y `NativeVideoFilter` en Kotlin.
2. **Procesador de Audio Universal en Pipeline (`OboeAudioProcessor`):** Al insertarse como procesador de audio dentro de `DefaultAudioSink`, no se requiere re-instanciar el reproductor para alternar entre Oboe y AudioTrack. Además, ejecuta de forma integrada el pipeline DSP en tiempo real (Voces Claras con filtro peaking biquad, Compresor Dinámico Nocturno DRC y enrutamiento Mono/Haas 3D) directamente sobre las muestras PCM de Media3, garantizando que ambos motores de audio disfruten de las mismas capacidades acústicas avanzadas sin latencia perceptible.
3. **Pipeline de Video Acelerado por GPU (OpenGL ES & C++):** El decodificador por hardware escribe directamente en un `SurfaceTexture` conectado a `GL_TEXTURE_EXTERNAL_OES`. El renderizado pasa por un pipeline nativo en C++ (`VideoColorEngine`) que aplica correcciones de color y convolución en el fragment shader sin provocar pausas ni consumir ciclos de CPU.
4. **Preservación Acústica de Tono (Sonic Pitch Preservation):** El control de velocidad (hasta 2.0x) implementa el algoritmo Sonic integrado en Media3, permitiendo acelerar o ralentizar la reproducción conservando la afinación y timbre de voces e instrumentos.
5. **Soporte Arquitectural Universal:** La compilación de C++ y Rust está parametrizada para generar binarios tanto en 32 bits (`armeabi-v7a`, `x86`) como en 64 bits (`arm64-v8a`, `x86_64`), permitiendo que el mismo código fuente ejecute en cualquier dispositivo.
6. **Sin Dependencias de Servicios Propietarios:** No se utilizan APIs de Google Play Services ni bibliotecas cerradas con licencias restrictivas, facilitando la publicación en plataformas abiertas y tiendas alternativas.
7. **Pantalla Independiente de Configuración (Arquitectura Hub-and-Spoke):** Organizada mediante un menú raíz limpio (`SettingsMainHub`) que navega hacia subpantallas modulares dedicadas (`AudioEngineSubScreen`, `AudioOutputSubScreen`, `VideoSubScreen`, `StorageSubScreen`, `ArchitectureSubScreen`), preservando la posición de reproducción y ofreciendo pruebas acústicas senoidales y telemetría en tiempo real.
8. **Persistencia Local Reactiva con Room:** Registro estructurado de videos importados y vistos en SQLite local (`VideoEntity`, `VideoDao`, `AppDatabase`). La interfaz observa de forma reactiva un `Flow<List<VideoEntity>>` a través de `MainViewModel`, mostrando títulos, duraciones, tamaños, barras de progreso y reanudación instantánea desde la última posición guardada.
9. **Compilación en la Nube Autónoma (CI/CD sin Caché):** Flujo de GitHub Actions (`build-debug.yml`) que garantiza compilación limpia desde cero con generación no interactiva de firma digital (`generate_debug_keystore.sh`). Permite a desarrolladores sin PC compilar y descargar el APK de depuración directamente en su teléfono móvil.
10. **Búfer de RAM Adaptativo y Protección Anti-OOM (`PlayerLoadControlHelper`):** Asignación estricta de memoria de carga en ExoPlayer ajustada a la memoria RAM real del dispositivo y edición de Android (Android Go). En dispositivos con ≤ 2.5 GB de RAM, impone un búfer conservador de 4s a 10s y un tope de 16 MB a 24 MB en `DefaultAllocator`, previniendo que el Low Memory Killer (LMK) cierre la aplicación en segundo plano o durante videos de alta tasa de bits.
11. **Gestión Desacoplada de Subtítulos (SRT y WebVTT):** `SubtitleView` integrado en capa superior sobre la superficie de video nativa OpenGL. Soporte dual para selección de pistas internas decodificadas automáticamente por el demuxer e inyección en caliente de archivos externos (`.srt` / `.vtt`) mediante `MediaItem.SubtitleConfiguration` y Storage Access Framework, sin reiniciar el pipeline de video ni perder la posición de reproducción.
12. **Arquitectura Modular de Herramientas del Reproductor:** Cada funcionalidad de ajuste visual o acústico dispone de su propia pantalla o panel modal independiente y exclusivo (`VideoEqualizerSheet` para color/nitidez/descanso visual, `PillarboxBlurSheet` para relleno desenfocado de fondos verticales, `FsrUpscaleSheet` para Super Resolución AMD FSR 1.0, `AspectRatioSheet` para relación de aspecto, `AudioEngineSheet` para conmutación de motor de audio, `PlaybackSpeedSheet` para velocidad y `SubtitlesBottomSheet` para subtítulos). La navegación hacia estas herramientas se orquesta a través del panel lateral derecho `PlayerToolsSideSheet`, evitando sobrecargar la pantalla principal y facilitando la adición de nuevas herramientas de manera desacoplada.
13. **Desarrollo Modular y Desacoplamiento de UI & Shaders C++:**
    - **Capa C++ / GLSL:** Extracción de todos los sombreadores GLSL en `shaders/VideoShaders.h`, dejando `VideoColorEngine.cpp` enfocado estrictamente en la gestión de programas de sombreado, compilación de shaders, enlace de uniforms y orquestación de renderizado FBO en dos fases.
    - **Biblioteca (`com.example.ui.library`):** Fragmentación de `VideoImportScreen.kt` en componentes reutilizables con responsabilidades claras: cabecera (`LibraryHeaders.kt`), tarjeta de importación rápida (`ImportQuickCard.kt`), tarjeta de historial de video con mini-progreso (`VideoHistoryCard.kt`), diálogos de borrado seguro (`LibraryDialogs.kt`) y tarjetas de pie (`LibraryFooterCards.kt`).
    - **Configuración (`com.example.ui.settings`):** Descomposición de `SettingsScreen.kt` en subpantallas modulares desacopladas que reducen la carga cognitiva y permiten extender ajustes de audio, video o almacenamiento sin alterar el coordinador.
    - **Reproductor (`com.example.ui.player`):** Descomposición de `VideoPlayerScreen.kt` en controladores especializados: gestos táctiles concurrentes (`PlayerGestureDetector.kt`), indicadores HUD no intrusivos (`PlayerHudIndicators.kt`), rotación reactiva por sensor de hardware (`PlayerOrientationHandler.kt`) y controles táctiles superpuestos en pantalla completa (`PlayerOverlayControls.kt`).
14. **Personalización Material You y Sistema de Temas (`AppThemeMode`):** Soporte nativo para extracción de colores dinámicos del sistema en Android 12+ (`dynamicDarkColorScheme`, `dynamicLightColorScheme`), con conmutación en caliente entre Modo Oscuro, Claro y del Sistema, persistencia reactiva en `AppPreferences` y blindaje tipográfico estricto (`fontScale = 1.0f`).
15. **Infraestructura y Detección de Capacidades Vulkan 1.1+ (Fase 6):** Implementación del módulo `VulkanCapabilities.kt` y puente JNI `nativeQueryVulkanDriver` en C++. Realiza una doble verificación en tiempo de ejecución: consulta `FEATURE_VULKAN_HARDWARE_VERSION` (requiriendo versión >= 1.1 `0x401000`) y `FEATURE_VULKAN_HARDWARE_LEVEL` a nivel de Android, y ejecuta una consulta nativa directa al loader de Vulkan y a la GPU física (`vkEnumerateInstanceVersion` y `vkGetPhysicalDeviceProperties`). Asimismo, `CMakeLists.txt` enlaza la biblioteca del sistema `libvulkan.so` en todas las arquitecturas soportadas (32 bits y 64 bits), preparando la base para la migración del pipeline gráfico a Vulkan con `VK_ANDROID_external_memory_android_hardware_buffer` manteniendo degradación elegante (*graceful fallback*) hacia OpenGL ES.
