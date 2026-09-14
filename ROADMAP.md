# Hoja de Ruta (Roadmap) - Nova Video Player

Este documento detalla las fases de desarrollo planificadas para convertir a **Nova Video Player** en un reproductor multimedia móvil avanzado, eficiente y adaptable a cualquier gama de hardware.

---

## 📍 Estado del Proyecto: En Desarrollo Activo

```text
[✅] Fase 1: Base de Importación y Reproducción de Video
[✅] Fase 2: Configuración Nativa C++, Google Oboe y Rust
[✅] Fase 3: Pipeline de Renderizado con OpenGL ES y Ecualizador de Video
[✅] Fase 4: Subtítulos SRT/VTT y Búfer de Memoria RAM Adaptativo (Android Go)
[✅] Fase 5: Aceleración con Núcleo Rust, Subtítulos SSA/ASS y Persistencia por Video
[✅] Fase 6: Asistente de Bienvenida, Motores Gráficos y Escaneo de Mensajería
[🔄] Fase 7: Pipeline de Renderizado Vulkan 1.1+ (Detección, Enlace NDK y Selección en UI completados)
[⏳] Fase 8: Transmisión y Pantalla Compartida a TV (Casting / Mirroring)
[⏳] Fase 9: Empaquetado y Distribución Externa (Uptodown / APK Autónomo)
```

---

## 🎯 Desglose por Fases

### ✅ Fase 1: Importación Flexible, Biblioteca y Reproducción Base (Completada)
- [x] Selector dual de entrada: Galería del sistema (`PickVisualMedia`) y Gestor de Archivos nativo (`OpenDocument` / SAF).
- [x] **Biblioteca Local Persistente con Room (SQLite):** Registro de archivos importados y vistos mostrando título completo, duración real formateada (`MediaMetadataRetriever`), tamaño en MB/GB y barra de progreso.
- [x] Reanudación con un solo toque directamente desde el último punto de visualización guardado en base de datos.
- [x] Interfaz de usuario inspirada en controles de PC (Seekbar, saltos de 10s, modos de relación de aspecto).
- [x] **Modo Inmersivo Completo:** Ocultamiento automático de la barra de estado (reloj, batería, notificaciones) y barra de navegación durante la reproducción.
- [x] **Pellizcar para Zoom Táctil Continuo (Pinch-to-Zoom hasta x10):** Soporte multitáctil fluido con dos dedos para ampliar desde 1.0x hasta 10.0x, desplazamiento panorámico (*pan*) sobre la imagen ampliada, doble toque para restablecer a 1.0x, indicador flotante HUD interactivo (`ZoomHudIndicator`) y panel inferior de ajuste fino con presets (`ZoomBottomSheet`).
- [x] **Gesto de Avance Rápido a 2X:** Activación instantánea a 2.0x al mantener presionado el lateral derecho (700 ms) con badge flotante HUD y reversión automática a la velocidad previa al soltar.
- [x] **Gestos Táctiles con HUD Minimalista:** Control de brillo deslizando en la mitad izquierda y control de volumen físico en la mitad derecha con indicador flotante no invasivo.
- [x] Gestión de pantalla encendida (`FLAG_KEEP_SCREEN_ON`), restauración de brillo y ciclo de vida de la actividad.
- [x] Compatibilidad con formatos populares (MP4, MKV, WebM, AVI, MOV).

---

### ✅ Fase 2: Motores de Audio y Capa Nativa (Completada)
- [x] Elevación de versión mínima de sistema a **Android 8.0 (API 26)** para garantizar acceso nativo a AAudio.
- [x] Integración de **Google Oboe 1.9.3** mediante Prefab y CMake en C++17.
- [x] **Optimización de Oboe con Búfer Circular Estático:** Eliminación de allocations dinámicas en el bucle de renderizado y soporte de vaciado instantáneo (`flush()`) para saltos temporales (seek) sin cuelgues de audio.
- [x] **Integración de Decodificadores FFmpeg Puros:** Integración de `media3-ffmpeg-decoder` (DTS, AC3, TrueHD, FLAC, Opus) sin wrappers obsoletos.
- [x] `OboeAudioProcessor` conectado a Media3 para desviar tramas PCM hacia el motor nativo en C++.
- [x] Selector dinámico en la interfaz para alternar entre **Oboe C++ (Baja Latencia)** y **Media3 (AudioTrack estándar)**.
- [x] **Audio DSP Inteligente Universal (Google Oboe C++ y Android Media3):** Expansión del procesamiento en tiempo real de Voces Claras y Compresor Dinámico Nocturno (DRC) a ambos motores, eliminando cualquier bloqueo en Media3 y habilitando ecualización y compresión acústica universal sin latencia.
- [x] **Aislamiento de Escala Tipográfica del Sistema:** Fijación de `fontScale = 1.0f` en el tema de Jetpack Compose para asegurar legibilidad y diagramación predecible independiente de las preferencias del sistema operativo.
- [x] Transición del diálogo modal hacia una **Pantalla Independiente de Configuración (`SettingsScreen`)** con navegación desacoplada y conservación de estado de reproducción.
- [x] Verificación de salida física al 100% mediante sintetizador senoidal de tono PCM integrado para Oboe y Media3.
- [x] Telemetría nativa en tiempo real con monitoreo periódico JNI (tramas C++, backend AAudio/OpenSL ES, sample rate, canales y ABI).
- [x] Configuración inicial del módulo Rust (`novaplayer_rust`) con targets para 32 y 64 bits.

---

### ✅ Fase 3: Pipeline Gráfico con OpenGL ES y Ecualizador en Tiempo Real (Completada)
- [x] Motor nativo de sombreadores en C++ (`VideoColorEngine.h`, `VideoColorEngine.cpp`).
- [x] Enlace de bibliotecas nativas `GLESv2` y `EGL` en `CMakeLists.txt` con compatibilidad 32 y 64 bits.
- [x] Texturizado *Zero-Copy* por hardware mediante la extensión `GL_TEXTURE_EXTERNAL_OES` y `SurfaceTexture`.
- [x] Fragment shader dinámico GLSL ejecutado 100% en GPU para ecualización y efectos visuales en tiempo real sin pausar la reproducción:
  - Brillo dinámico con desplazamiento perceptual [-0.5f a 0.5f].
  - Contraste con pivote en gris neutro [0.5f a 2.0f].
  - Saturación cromática Rec. 709 [0.0f a 2.0f].
  - Corrección de rango dinámico Gamma [0.5f a 2.0f].
  - Filtro de nitidez (*sharpening*) mediante convolución Laplaciano 3x3 en GPU.
  - **Filtro de Luz Azul / Modo Descanso Visual:** Atenuación selectiva del espectro azul y calidez ámbar [0.0f a 1.0f] para confort nocturno.
  - **Desenfoque de Fondo para Videos Verticales (Pillarbox Blur):** Relleno dinámico de barras negras laterales con versión ampliada, desenfocada (Gaussiano de 9 toques) y atenuada del video acelerada por GPU.
  - **AMD FidelityFX Super Resolution 1.0 (FSR 1.0):** Reconstrucción espacial adaptativa (EASU) con detección de aristas por gradiente y afilado dependiente del contraste local (RCAS) en shader GLSL para escalado y nitidez de videos de baja resolución.
  - **Reconstrucción y Realce Anime4K (bloc97):** Filtros GLSL en GPU para series y anime con modos Lite (bilateral rápido), Pro (perfilado y adelgazamiento de trazos) y Restauración (denoise/despeckle para fondos lisos) con regulador de intensidad.
- [x] Presets preconfigurados instantáneos: *Normal*, *Anime 4K (Pro)*, *Super-Resolución FSR*, *Descanso Visual*, *Vívido*, *Cine*, *Nocturno*, *Alto Contraste* y *Blanco y Negro*.
- [x] Control de velocidad de reproducción (hasta 2.0x) con *Sonic Pitch Preservation* activo sin distorsión de audio.
- [x] **Arquitectura Modular de Herramientas del Reproductor (Pantallas Independientes):**
  - Panel lateral derecho interactivo (`PlayerToolsSideSheet`) para navegación limpia y desacoplada entre herramientas.
  - Pantallas y hojas exclusivas e independientes para cada funcionalidad:
    - `VideoEqualizerSheet`: Ajuste dedicado de brillo, contraste, saturación, gamma, nitidez y presets de color.
    - `Anime4KSheet`: Pantalla dedicada de configuración de modos Anime4K (Lite, Pro, Restauración) y deslizador de fuerza.
    - `SunModeSheet`: Pantalla independiente de compensación para exteriores bajo luz solar intensa y perfiles de alto contraste para accesibilidad visual.
    - `PillarboxBlurSheet`: Control exclusivo para desenfoque y relleno de barras laterales en videos verticales.
    - `FsrUpscaleSheet`: Pantalla dedicada de Super Resolución AMD FidelityFX FSR 1.0 (EASU + RCAS).
    - `VoiceNightAudioSheet`: Pantalla dedicada de Audio Inteligente DSP (Modo Voces Claras y Compresor Dinámico Nocturno para Oboe C++ y Media3).
    - `AspectRatioSheet`: Selección de modo de pantalla geométrico (Ajustar, Zoom, Llenar).
    - `AudioEngineSheet`: Conmutador de motor de audio (Oboe C++ vs Media3 AudioTrack).
    - `PlaybackSpeedSheet`: Selector de velocidad de reproducción (hasta 2.0x).
    - `SubtitlesBottomSheet`: Gestión de subtítulos internos y externos con selector tipográfico.
    - Modo de Bloqueo de Pantalla (`Lock`): Desactiva toques y gestos accidentales con botón flotante de desbloqueo.
- [x] **Procesamiento de Audio DSP en Tiempo Real Universal (Google Oboe C++ y Media3):**
  - **Filtro Peaking Vocal (1.5 kHz a 3.5 kHz):** Realce inteligente de diálogos y frecuencias fonéticas clave.
  - **Compresor de Rango Dinámico (DRC):** Normalización de picos para cine nocturno (suaviza explosiones y levanta susurros).
  - **Implementación Dual:** Aceleración SIMD NEON bajo Oboe C++ y transformación de flujo PCM optimizada en `OboeAudioProcessor` bajo Media3.
  - **Gestor de Canales en Tiempo Real (Estéreo / Mono / Pseudo-Estéreo Haas 3D):**
    - Enrutamiento estéreo original.
    - Conversión y duplicación de pistas mono a ambos auriculares.
    - Modo mono centrado `(L + R) / 2`.
    - Efecto Haas 3D con retardo interaural de 15 ms en canal derecho mediante DSP en C++.
- [x] **Rotación Automática Forzada por Hardware (`OrientationEventListener`):**
  - Alternancia entre modo horizontal y vertical según la posición del teléfono, incluso con el giro automático desactivado en Android.
  - Retorno garantizado a vertical (`SCREEN_ORIENTATION_PORTRAIT`) al finalizar el video o al volver a la biblioteca principal.
- [x] **Centro de Configuración Ergonómico Modular (`SettingsScreen`):**
  - Reemplazo de la lista larga monolítica por una arquitectura por subpantallas independientes (Motor de Audio, Canales, Prueba de Sonido, Telemetría y Acerca de).
- [x] Componentes modulares Jetpack Compose: `OpenGLVideoSurface`, `VideoEqualizerSheet`, `Anime4KSheet`, `StereoMonoSheet`, `SunModeSheet`, `VoiceNightAudioSheet`, `PillarboxBlurSheet`, `FsrUpscaleSheet`, `AspectRatioSheet`, `AudioEngineSheet` y `PlaybackSpeedSheet`.

---

### ✅ Fase 4: Subtítulos SRT/VTT y Búfer de Memoria RAM Adaptativo (Completada)
- [x] **Gestión Dinámica de Búfer de RAM (`PlayerLoadControlHelper`):**
  - Perfil inteligente anti-OOM para terminales con memoria ajustada y Android Go (≤ 2.5 GB RAM): búfer de 4s a 10s y límite de 16 MB a 24 MB en `DefaultAllocator`.
  - Perfil estándar para terminales con ≥ 3 GB de RAM: búfer de 15s a 30s.
  - Telemetría en `SettingsScreen` mostrando perfil activo, rangos de segundos, tope en MB y RAM física del dispositivo.
- [x] **Motor de Subtítulos SRT (.srt) y WebVTT (.vtt):**
  - Renderizado en tiempo real con `SubtitleView` sobre la superficie de video OpenGL.
  - Detección automática y selección de pistas internas en archivos contenedores (MKV, MP4).
  - Carga de subtítulos externos mediante Storage Access Framework (`.srt` y `.vtt`).
  - Panel modal inferior interactivo (`SubtitlesBottomSheet`) con selector de pistas y ajuste de tamaño tipográfico (Pequeño, Normal, Grande, Extra Grande).
  - Botón de acceso directo `CC` / `CC On` en la barra de controles inferior.
- [x] **Persistencia de Motor de Audio y Media3 como Predeterminado:**
  - Media3 configurado como motor de audio estándar por defecto con sincronización A/V óptima y soporte Bluetooth universal.
  - Google Oboe mantenido como opción C++ de baja latencia seleccionable.
  - Persistencia completa en disco con `AppPreferences` para conservar la selección tras cerrar y reabrir la app.
- [x] **Salto Rápido por Doble Toque (+5s / -5s):**
  - Doble toque lateral izquierdo: atrasa 5 segundos (-5s).
  - Doble toque lateral derecho: adelanta 5 segundos (+5s).
  - Indicador visual HUD circular animado y respuesta háptica.
- [x] **Confirmación de Seguridad al Eliminar Videos:**
  - Diálogo modal de confirmación antes de eliminar videos individuales en la biblioteca.
  - Aclaración de protección de archivos originales en el almacenamiento del dispositivo.

---

### ✅ Fase 5: Aceleración con Núcleo Rust, Subtítulos SSA/ASS y Persistencia por Video (Completada)
- [x] **Enlace bidireccional JNI / FFI entre Rust y Android (`NovaRustCore`):**
  - Módulo nativo compilado con `cargo ndk` para los 4 targets Android (`aarch64`, `armv7`, `x86_64`, `i686`).
  - Carga segura y sin fallbacks falsos de `libnova_rust.so` mediante `System.loadLibrary`.
- [x] **Parser nativo de subtítulos SSA/ASS de alto rendimiento en Rust:**
  - Decodificación en Cero-Copia de encabezados `[Script Info]`, metadatos de resolución virtual `PlayResX` / `PlayResY`, estilos `[V4+ Styles]` y eventos `[Events]`.
  - Normalización de colores hexadecimales BGR/ABGR de SSA hacia RGB/ARGB estándar.
  - Parseo de etiquetas de tiempo `H:MM:SS.CC` a milisegundos absolutos y extracción limpia de texto eliminando secuencias de control complejas `{\tag}`.
  - Serialización estructurada a JSON con `serde` / `serde_json` transmitida a través de JNI con deserialización rápida mediante `kotlinx.serialization`.
- [x] **Componente de Renderizado Avanzado `AssSubtitleOverlay` (Jetpack Compose):**
  - Renderizado dinámico sincronizado al milisegundo durante la reproducción del video.
  - Posicionamiento virtual proporcional calibrado según `PlayResX` y `PlayResY`.
  - Soporte de alineación posicional de 9 puntos estilo teclado numérico del estándar ASS (Bottom-Center, Top-Center, Middle, etc.).
  - Doble paso de renderizado tipográfico con contorno (*stroke outline*) para contraste absoluto sobre cualquier escena.
  - Respeto del tamaño dinámico configurado por el usuario (`SubtitleSize`).
- [x] **Persistencia Completa de Configuraciones por Video (Room SQLite v2):**
  - Migración y ampliación de la entidad `VideoEntity` a la versión 2 del esquema con migración destructiva controlada para desarrollo ágil.
  - Almacenamiento y restauración automática e individualizada por video de:
    - **Velocidad de reproducción** (`playbackSpeed`).
    - **Relación de aspecto geométrico** (`aspectRatioMode`: Fit, Zoom, Fill).
    - **Motor de audio seleccionado** (`audioEngine`: Media3 o Google Oboe C++).
    - **Modo de canales de audio** (`audioChannelMode`: Estéreo, Mono o Pseudo-Estéreo Haas 3D).
    - **Estado y tamaño de subtítulos** (`subtitlesEnabled`, `subtitleSize`).
    - **Subtítulo externo asignado** (`externalSubtitleUri`, `externalSubtitleName`).
    - **Ajustes completos del Ecualizador y Shaders OpenGL ES en C++:** Brillo, contraste, saturación, gamma, nitidez, filtro de luz azul, desenfoque de barras laterales (Pillarbox Blur), AMD FSR 1.0 (activación y nitidez RCAS), Modo Sol Extremo y perfiles Anime4K (modo y fuerza).
  - Reactividad instantánea: los ajustes se sincronizan en caliente al cerrar cada modal o salir de la pantalla de reproducción.
- [x] **Personalización Visual Material You y Selector de Tema (Oscuro, Claro y del Sistema):**
  - Integración profunda de **Material You** con extracción armónica de colores dinámicos desde el fondo de pantalla en Android 12+ (`dynamicDarkColorScheme` y `dynamicLightColorScheme`).
  - Selector de tema de 3 modos configurables: **Modo Oscuro**, **Modo Claro** y **Seguir al Sistema** (`AppThemeMode`).
  - Control de activación / desactivación manual de colores dinámicos con paleta M3 personalizada de respaldo de alto contraste.
  - Subpantalla de navegación independiente `AppearanceSubScreen` en el menú de Configuración con tarjeta de previsualización en vivo.
  - Persistencia reactiva inmediata mediante `AppPreferences` y `MainViewModel` (`StateFlow`).
  - Blindaje estricto de la escala tipográfica fija (`fontScale = 1.0f`) previniendo solapamientos o desbordamientos en cualquier combinación visual.

---

### ✅ Fase 6: Asistente de Bienvenida, Motores Gráficos, Efectos Sonoros y Transiciones Gestuales (Completada)
- [x] **Asistente de Bienvenida y Configuración Inicial (`OnboardingScreen`):**
  - Flujo guiado interactivo de 6 pasos con indicador visual de progreso (`StepProgressIndicator`), animaciones horizontales y soporte de botón de retorno.
  - **Paso 1 (Bienvenida y Permisos):** Solicitud interactiva de permisos de lectura de almacenamiento (`READ_MEDIA_VIDEO` en Android 13+ y `READ_EXTERNAL_STORAGE` en Android 8 a 12), con distintivo de estado en tiempo real y garantía de privacidad local (sin nube).
  - **Paso 2 (Motor de Audio):** Elección entre Android Media3 (AudioTrack estándar) y Google Oboe (Nativo C++ de baja latencia), desglosando ventajas (sincronía Bluetooth vs latencia nula) y desventajas técnicas.
  - **Paso 3 (Motor Gráfico):** Detección automática del soporte Vulkan 1.1+ del hardware. Si es compatible, permite seleccionar entre OpenGL ES 3.0+ (estable y compatible con todos los efectos) y Vulkan 1.1+ (bajo nivel y menor consumo de CPU), con advertencia explícita de desarrollo activo en funciones avanzadas de video. Si no es compatible, fija OpenGL ES automáticamente con mensaje explicativo.
  - **Paso 4 (Apariencia y Colores):** Selector de modo de tema (Oscuro para pantallas OLED/Cine, Claro para exteriores y Sincronizado con el sistema), con interruptor para Material You (colores dinámicos del fondo de pantalla en Android 12+).
  - **Paso 5 (Descubrimiento de Medios):** Selector entre escaneo automático de carpetas de mensajería (WhatsApp y Telegram) o Modo Privado (solo importar archivos manualmente mediante el gestor o galería).
  - **Paso 6 (Resumen y Arranque):** Tarjeta recapitulativa de opciones seleccionadas y botón directo de inicio hacia la biblioteca.
- [x] **Módulo de Escaneo de Videos en Mensajería (`MessagingMediaScanner`):**
  - Consulta segura a `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` filtrando rutas conocidas (`/WhatsApp/Media/WhatsApp Video/`, `/Telegram/Telegram Video/`, etc.).
  - Extracción de metadatos (título, URI, tamaño, duración) e inserción no destructiva en la base de datos Room evitando duplicados.
  - Tarjeta de escaneo dedicada en la pantalla principal (`VideoImportScreen`) con indicador de carga y botón de escaneo manual para refrescar videos recibidos en cualquier momento.
- [x] **Efectos de Sonido de Interfaz Nativos con SoundPool (`SoundEffectManager`):**
  - **Función perfeccionada y consolidada tras fase de desarrollo:** Se resolvió la incidencia que impedía la audición de clicks en dispositivos con modos silenciosos o perfiles estándar de Android.
  - **Enrutamiento Robusto de Audio:** Migración arquitectónica de `USAGE_ASSISTANCE_SONIFICATION` (restringido al canal `STREAM_SYSTEM` que se silencia por ajustes globales de fábrica o modo vibración) hacia `AudioAttributes.USAGE_MEDIA` + `CONTENT_TYPE_SONIFICATION` (enrutado a `STREAM_MUSIC`, audible con el volumen de medios de la app).
  - **Calibración del Recurso Acústico:** Sustitución del micro-asset preliminar (que causaba bajo flujo por sub-paquete en decodificadores Vorbis de Stagefright y truncado por rampa de encendido de amplificadores DAC) por una muestra acústica optimizada de 42 ms (2,016 muestras a 48 kHz mono) con ataque suave anti-pop, chasquido de 2.4 kHz y cuerpo resonante a 520 Hz con decaimiento exponencial a -0.9 dBFS.
  - **Mecanismo de Degradación Elegante (Fallback Inmediato):** Delegación automática en `AudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)` si SoundPool aún se encuentra precargando el recurso en memoria o agota la asignación de canales nativos, garantizando 0% de pulsaciones silenciosas.
  - **Integración Táctil Global:** Retroalimentación auditiva en botones de control, navegación de subpantallas de ajustes, panel lateral de herramientas del reproductor, doble toque temporal (+5s/-5s) y selección de videos.
  - Control de activación en *Configuración > Apariencia* con persistencia en `AppPreferences.isSoundEffectsEnabled`.
  - Script ejecutable de conversión de audio (`scripts/convert_audio_asset.sh`) para transcodificar cualquier audio a Ogg Vorbis ligero.
- [x] **Indicadores Gestuales Dinámicos y Transición entre Pantallas:**
  - Animaciones reactivas con físicas de resorte (`spring`) en los indicadores HUD de brillo y volumen (`MinimalistGestureIndicator`), variando anchura y resplandor según la intensidad.
  - Animación elástica de escala y neón turquesa para el indicador de doble toque (`DoubleTapSeekIndicator`).
  - Animación cíclica pulsante en la insignia de avance rápido 2X (`FastForward2xBadge`).
  - Transición fluida con `AnimatedContent` entre Home, Reproductor y Ajustes (desplazamientos verticales y horizontales con desvanecimiento cruzado).
- [x] **Persistencia de Configuración Inicial (`AppPreferences` y `MainViewModel`):**
  - Almacenamiento seguro de `isOnboardingCompleted`, `selectedGraphicsEngine`, `scanMessagingApps` y `isSoundEffectsEnabled` con persistencia en `SharedPreferences`.

---

### 🔄 Fase 7: Pipeline de Renderizado Gráfico Vulkan 1.1+ (En Progreso)
- [x] **Detección Dinámica de Capacidades de Hardware:**
  - Consulta en tiempo de ejecución de `FEATURE_VULKAN_HARDWARE_VERSION` (detección de versión >= 1.1 `0x401000`) y `FEATURE_VULKAN_HARDWARE_LEVEL` mediante `VulkanCapabilities.kt`.
  - Doble verificación nativa en C++ a través de JNI (`nativeQueryVulkanDriver`): consulta dinámica de `vkEnumerateInstanceVersion`, creación de instancia temporal y extracción de propiedades del dispositivo físico (`vkGetPhysicalDeviceProperties`: nombre de GPU, versión del controlador y tipo de hardware).
- [x] **Preparación del Entorno NDK y Enlace CMake:**
  - Enlace de la biblioteca nativa `vulkan` en `app/src/main/cpp/CMakeLists.txt` con compatibilidad cruzada estricta para 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).
- [x] **Módulo de Diagnóstico Gráfico en Telemetría (`TelemetrySubScreen`):**
  - Tarjeta interactiva en vivo con distintivo de compatibilidad (Vulkan 1.1+ Listo / Vulkan 1.0 Básico / Incompatible), versión de API, nivel de hardware, GPU detectada y versión del controlador.
- [x] **Integración en Selección de Motor Gráfico (`GraphicsEngineType`):**
  - Enum `OPENGL_ES` y `VULKAN` persistido en disco y configurable desde el asistente inicial.
- [ ] **Arquitectura con Degradación Elegante (*Graceful Fallback*):**
  - Dispositivos con Vulkan 1.1+: Canal de renderizado nativo C++ con extensión `VK_ANDROID_external_memory_android_hardware_buffer` para menor sobrecarga de CPU y consumo de batería.
  - Dispositivos sin soporte o con versiones previas (Vulkan 1.0): Renderizado automático y transparente con el pipeline probado de OpenGL ES 2.0 / 3.0.
- [ ] **Compilación de Shaders a SPIR-V (`glslc`):**
  - Pipeline de compilación de sombreadores GLSL hacia bytecode binario SPIR-V para los efectos de color, nitidez y reconstrucción.
- [ ] **Canal de Renderizado Nativo C++ (`VulkanVideoEngine`):**
  - Implementación de `VkInstance`, `VkSurfaceKHR`, `VkSwapchainKHR`, colas de comandos y sincronización de tramas.
- [ ] Perfiles automáticos de uso de memoria RAM (límite estricto de búferes en dispositivos de 1GB/2GB y Android Go).
- [ ] Estrategia de reducción de resolución de texturas intermedias si la GPU reporta sobrecarga.
- [ ] Desactivación selectiva de shaders pesados en dispositivos con procesadores ARMv7 de 32 bits.

---

### ⏳ Fase 8: Transmisión y Pantalla Compartida a TV (Casting / Mirroring con Fidelidad Total)
- [ ] **Soporte de Pantallas Secundarias (`DisplayManager` / `Presentation`):**
  - Salida directa por cable USB-C a HDMI / DisplayPort (Modo Escritorio / Samsung DeX / Display externo): renderiza el pipeline completo de OpenGL ES y audio Oboe nativo con calibración idéntica a la pantalla del móvil.
- [ ] **Protocolo de Pantalla Inalámbrica (Miracast / Wi-Fi Display):**
  - Duplicación de pantalla a nivel del framebuffer del sistema: transmite directamente el flujo postprocesado con shaders y DSP de audio.
- [ ] **Servidor de Streaming Local RTSP/HTTP (Sin dependencias de Google Cast ni Play Services):**
  - Arquitectura compatible con Smart TVs universales (LG webOS, Samsung Tizen, Android TV, Fire TV, Roku y navegadores DLNA/UPnP) transmitiendo el flujo con las correcciones de color y audio preservadas.

---

### ⏳ Fase 9: Empaquetado y Distribución Libre
- [x] **Pipeline de Integración Continua (CI/CD) con GitHub Actions (`build-debug.yml`):**
  - Descarga integral del repositorio y configuración automática de herramientas nativas (NDK r26d, CMake 3.22.1 y Rust stable con targets Android).
  - Compilación limpia forzada (**sin caché**) con flags `--no-build-cache` y `cache-disabled: true`.
  - Script automatizado de generación de firma debug (`scripts/generate_debug_keystore.sh`) que genera el keystore desde cero mediante `keytool` sin bloqueos interactivos.
  - Generación y publicación automática del APK de depuración como artefacto descargable directamente en teléfonos móviles.
- [ ] Optimización de ProGuard / R8 para reducir tamaño del binario y ofuscar código sensible.
- [ ] Pruebas exhaustivas de instalación directa de APK sin servicios de Google Play (GMS independiente).
- [ ] Generación de builds separados por arquitectura (`armeabi-v7a`, `arm64-v8a`) y build universal para Uptodown.
