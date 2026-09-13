# AI Context & Engineering Guidelines - Nova Video Player

Este archivo proporciona el contexto técnico, arquitectónico y operativo necesario para que cualquier modelo de Inteligencia Artificial entienda el proyecto y continúe su desarrollo de manera consistente.

---

## 🎯 Identidad y Objetivo del Proyecto
- **Nombre:** Nova Video Player.
- **Tipo de Aplicación:** Reproductor multimedia avanzado de alto rendimiento para Android.
- **Objetivo:** Ofrecer una experiencia de reproducción similar a los mejores reproductores de escritorio (controles precisos, soporte universal de formatos, alto rendimiento sin microcortes), maximizando la compatibilidad tanto en dispositivos modernos como en hardware de recursos reducidos.

---

## ⚡ Restricciones Críticas e Inviolables

1. **Versión Mínima de Android:**
   - `minSdk = 26` (Android 8.0 Oreo). No debe reducirse por debajo de 26, ya que Android 8 es la versión base que introduce soporte para la API nativa de **AAudio** requerida por Google Oboe.
   - Si se contemplan versiones futuras, evaluar siempre el impacto en la base de usuarios antes de modificar `minSdk`.

2. **Arquitecturas de Procesador:**
   - Debe soportar obligatoriamente **32 bits** (`armeabi-v7a`, `x86`) y **64 bits** (`arm64-v8a`, `x86_64`).
   - Las compilaciones nativas de C++ (CMake) y Rust (`Cargo`) deben mantener compatibilidad con ambas arquitecturas.

3. **Compatibilidad con Android Go y Hardware Ajustado:**
   - Evitar fugas de memoria o retención innecesaria de búferes PCM en memoria RAM.
   - Las estructuras nativas en C++ deben liberar inmediatamente la memoria al detener la reproducción (`OboeAudioEngine::stop()`).

4. **Distribución Fuera de Google Play (Uptodown / APK de Terceros):**
   - El proyecto **NO** debe depender de servicios propietarios de Google Play Services (GMS), Play Integrity, o bibliotecas cerradas de Google Play.
   - Debe funcionar de forma 100% autónoma en cualquier dispositivo Android estándar o ROM personalizada.

5. **Seguridad y Propiedad Intelectual:**
   - **NO** incluir nombres de marcas comerciales registradas en archivos o identificadores que puedan comprometer al usuario.
   - **NO** usar variables o comandos de sistema restringidos como `persist.sys.*`.
   - **NO** utilizar dependencias o librerías con licencias copyleft restrictivas (GPL/AGPL) que fuercen la apertura obligatoria del código o el reconocimiento forzado. Priorizar siempre licencias permisivas (Apache 2.0, MIT, BSD, zlib).

6. **Integración Real de Tecnologías:**
   - Todo módulo solicitado en C++, Rust o Kotlin debe ser funcional y estar debidamente enlazado en los scripts de compilación (`build.gradle.kts`, `CMakeLists.txt`, `Cargo.toml`).
   - Prohibido implementar funciones falsas o atajos sin soporte real de dependencias.

---

## 🧩 Patrones de Arquitectura Implementados

### 1. Motor de Audio Nativo (C++ & Oboe)
- **Google Oboe** está vinculado mediante la directiva `prefab` de Android Gradle Plugin y consumido en `CMakeLists.txt` (`find_package(oboe REQUIRED CONFIG)` y `target_link_libraries(native-lib oboe::oboe)`).
- La clase nativa `OboeAudioEngine` implementa el callback `oboe::AudioStreamDataCallback`.
- Para evitar contención y latencia, utiliza un vector dinámico protegido por `std::mutex`.
- El volumen se escala matemáticamente sobre las muestras PCM de 16 bits (`int16_t`) antes de ser transmitidas al hardware de sonido.

### 2. Procesamiento de Audio en Media3 (`OboeAudioProcessor`)
- Extiende de `BaseAudioProcessor` de Media3.
- Monitorea el formato de entrada (frecuencia de muestreo y número de canales).
- Cuando el motor seleccionado es `AudioEngineType.OBOE`, las tramas PCM son redirigidas a `OboeAudioEngine.writePcmData(...)` y el búfer de salida para el `AudioTrack` estándar se vacía (`replaceOutputBuffer(0)`), silenciando la salida tradicional sin detener el flujo de reloj de ExoPlayer.
- Cuando el motor es `AudioEngineType.MEDIA3`, las tramas pasan sin alteración hacia el `AudioTrack` habitual.

### 3. Interfaz de Usuario en Jetpack Compose
- Toda la interfaz sigue las especificaciones de **Material Design 3 (M3)** con tema oscuro optimizado para reproducción de video (ahorro de batería en pantallas OLED).
- Cada componente interactivo cuenta con su identificador `Modifier.testTag(...)` para garantizar verificabilidad automatizada.
- El ciclo de vida de la reproducción está sincronizado con `LocalLifecycleOwner.current` para pausar inmediatamente el audio y video al salir de la aplicación.

### 4. Navegación y Centro de Configuración Modular (Settings Hub)
- La arquitectura de navegación utiliza la máquina de estados `AppScreen` (`HOME`, `PLAYER`, `SETTINGS`) en `MainActivity.kt`.
- `SettingsScreen` está estructurado como un **Centro de Ajustes Modular (Hub-and-Spoke)** que organiza las configuraciones en subpantallas dedicadas y autónomas para máxima ergonomía:
  - `AUDIO_ENGINE`: Alternancia en caliente entre Oboe C++ y Media3.
  - `AUDIO_CHANNELS`: Enrutamiento estéreo nativo, mono centrado (L+R)/2 y pseudo-estéreo espacial (efecto Haas 3D en C++).
  - `AUDIO_TEST`: Prueba física de sonido senoidal de 440 Hz en tiempo real para verificar el canal de audio del hardware.
  - `TELEMETRY`: Métricas JNI y de hardware (tramas C++, backend AAudio/OpenSL ES, sample rate, buffers y memoria RAM).
  - `ABOUT`: Información de compatibilidad 32/64 bits, Android Go y distribución directa de APK.
- Al regresar (mediante el botón de navegación del TopAppBar o el `BackHandler` del sistema), se restaura el contexto previo y se reanuda la reproducción en la posición exacta (`currentPlaybackPositionMs`).

### 4.1. Canales de Audio en Tiempo Real y Algoritmo Haas 3D (C++ / DSP)
- Implementación en `OboeAudioEngine` y `OboeAudioProcessor` mediante el enum `AudioChannelMode`:
  - `STEREO`: Reproducción directa de canales L y R.
  - `MONO`: Mezcla aditiva de canales `(L + R) / 2` aplicada a ambos oídos.
  - `SPATIAL_HAAS`: Algoritmo psicoacústico de espacialización mediante búfer de retardo de 15 ms en el canal derecho en C++, transformando señales mono o estéreo estrechas en un escenario sonoro envolvente.

### 4.2. Control Inteligente de Orientación de Pantalla por Sensor de Hardware
- `OrientationEventListener` conectado directamente al acelerómetro/giroscopio en `VideoPlayerScreen`:
  - Detecta la postura física del dispositivo y fuerza la orientación (`SCREEN_ORIENTATION_LANDSCAPE`, `SCREEN_ORIENTATION_REVERSE_LANDSCAPE`, `SCREEN_ORIENTATION_PORTRAIT`), **incluso si el usuario tiene desactivada la rotación automática en el sistema operativo**.
  - Garantiza retorno inmediato a vertical (`SCREEN_ORIENTATION_PORTRAIT`) al finalizar el video (`STATE_ENDED`), al salir de la pantalla o al pulsar Atrás.

### 5. Persistencia Local y Biblioteca Multimedia (Room Database)
- Implementación de base de datos SQLite con **Room** (`AppDatabase`, `VideoDao`, `VideoEntity`).
- El repositorio `VideoRepository` encapsula la persistencia y emite de manera reactiva un `Flow<List<VideoEntity>>` ordenado por última interacción (`lastPlayedTimestamp DESC`).
- `MainViewModel` (hereda de `AndroidViewModel`) expone la lista mediante `StateFlow` y `SharingStarted.WhileSubscribed(5000)`.
- `VideoUtils.resolveVideoMetadata` y `VideoUtils.getVideoDurationMs` utilizan `MediaMetadataRetriever` para calcular la duración exacta del video en milisegundos y formatearla a `mm:ss` o `hh:mm:ss`.
- `VideoPlayerScreen` informa periódicamente el progreso (`currentPositionMs` y `totalDurationMs`), actualizando la base de datos para mostrar barras de avance y permitir reanudación instantánea con un toque desde la pantalla principal (`VideoImportScreen`).

### 6. Sistema de Gestos Táctiles, Avance Rápido a 2X y Modo Inmersivo
- **Modo Inmersivo Automático (Edge-to-Edge Sin Distracciones):**
  - Implementado mediante `WindowInsetsControllerCompat` y `WindowInsetsCompat.Type.systemBars()` con `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
  - Oculta de forma transparente la barra de estado (reloj, nivel de batería, notificaciones) y la barra de navegación del sistema durante la reproducción de video.
  - Se reactiva de forma persistente en `Lifecycle.Event.ON_RESUME` y se restaura limpia y automáticamente en el bloque `onDispose` al salir del reproductor.
- **Gesto de Avance Rápido a 2X (Press & Hold en Lateral Derecho):**
  - Al pulsar y mantener presionado el lateral derecho (`startX >= size.width / 2f`), una corrutina programa la aceleración a 2.0x tras 700 ms.
  - **Sensibilidad y Fluidez:** Este retardo de 700 ms evita falsos positivos por toques accidentales o el inicio de un arrastre vertical para regular el volumen físico.
  - Si el usuario inicia un desplazamiento vertical antes de cumplirse el retardo (`totalDy > touchSlop`), el avance rápido se cancela y se delega el control al volumen.
  - Al cumplirse el tiempo, se emite vibración táctil háptica (`HapticFeedbackConstants.LONG_PRESS`), se muestra un HUD flotante estilizado con distintivo `2X Avance Rápido` (`player_fast_forward_2x_badge`) y se eleva la velocidad mediante `PlaybackParameters(2.0f, 1.0f)`.
  - Al levantar el dedo (`changedToUp()`) o ante cualquier interrupción (`finally`), la velocidad se restaura fiel e instantáneamente a la configurada por el usuario (`playbackSpeed`).
- **Detección Directa y Desacoplada (`pointerInput` con `awaitEachGesture`):**
  - Se utiliza una capa interactiva sobre la vista de video nativa para discriminar toques simples de arrastres verticales sin colisiones de eventos.
  - **Mitad Izquierda del Canvas:** Ajusta progresivamente el brillo de pantalla de la ventana (`WindowManager.LayoutParams.screenBrightness`) en un rango de `0.01f` a `1.0f`. Al salir de la pantalla o cerrar el reproductor, se restaura automáticamente el valor predeterminado del sistema (`BRIGHTNESS_OVERRIDE_NONE`).
  - **Mitad Derecha del Canvas:** Modifica directamente el volumen físico multimedia del dispositivo (`AudioManager.STREAM_MUSIC`), sincronizando el estado con el reproductor y reactivando el audio si se encontraba silenciado.
  - **Indicador Flotante Minimalista (`MinimalistGestureIndicator`):** Cápsula elegante que aparece flotando en el lateral activo exclusivamente mientras se realiza el gesto (`AnimatedVisibility` con `fadeIn` y `scaleIn`). Incorpora icono dinámico contextual (según tramos de volumen/brillo), barra vertical graduada con gradiente y porcentaje numérico. Se desvanece suavemente 1 segundo después de finalizar el gesto.

### 6.1. Motor Oboe Optimizado con Búfer Circular y Decodificadores FFmpeg Puros
- **Búfer de Anillo Estático en C++:** Sustitución de asignaciones dinámicas `std::vector` por un búfer estático preasignado de 192.000 muestras (`kRingBufferSize`). Los punteros atómicos de lectura y escritura (`std::atomic<size_t>`) eliminan la contención de memoria en el hilo de audio en tiempo real de AAudio.
- **Vaciado Atómico Instantáneo (`nativeFlush` / `OboeAudioEngine::flush`):** Limpia las tramas pendientes en el búfer circular sin necesidad de detener y reiniciar el stream de hardware, eliminando el congelamiento de audio durante el avance, retroceso (seek) o repetición en bucle de videos.
- **Configuración de Flujo Oboe:** Uso de `oboe::Usage::Media` y `oboe::ContentType::Movie` para enrutamiento idéntico al subsistema multimedia de Android.
- **Compensación de Ganancia Perceptual:** Factor de amplificación limpia de 1.40x para nivelar la presión sonora de Oboe con el estándar de `AudioTrack`.
- **Integración de Decodificadores FFmpeg Puros:** Integración directa de `org.jellyfin.media3:media3-ffmpeg-decoder` configurado en `DefaultRenderersFactory` con `EXTENSION_RENDERER_MODE_PREFER` para soporte universal de codecs de audio (AC-3, E-AC-3, DTS, TrueHD, FLAC, Opus).

### 7. Pipeline de Video Acelerado por GPU con OpenGL ES y C++
- **Motor Nativo `VideoColorEngine` (C++17):**
  - Implementa sombreadores de vértices y fragmentos GLSL optimizados para hardware móvil.
  - Trabaja directamente con la textura de hardware `GL_TEXTURE_EXTERNAL_OES` alimentada por `SurfaceTexture`.
  - La matriz de transformación de textura `uSTMatrix` garantiza la orientación y aspect ratio correctos sin rotaciones erróneas.
  - Variables uniformes controladas en caliente mediante mutex de sincronización (`std::mutex`):
    - `uBrightness`: Desplazamiento perceptual lineal [-0.5f a 0.5f].
    - `uContrast`: Modulación de escala con pivote centrado [0.5f a 2.0f].
    - `uSaturation`: Ponderación de luminancia Rec. 709 (`dot(rgb, vec3(0.2126, 0.7152, 0.0722))`).
    - `uGamma`: Corrección de curva exponencial (`pow(rgb, vec3(1.0 / uGamma))`).
    - `uSharpness`: Filtro de convolución con kernel Laplaciano 3x3 adaptado a las dimensiones del video.
    - `uBlueLightFilter`: Factor continuo [0.0f a 1.0f] que atenúa selectivamente el canal azul (`color.b *= (1.0 - uBlueLightFilter * 0.45)`) y compensa con calidez ámbar suave (`color.r` y `color.g`) para descanso visual.
    - `uBlurRadius` y `uBackgroundDim`: Radio de muestreo y coeficiente de atenuación para el desenfoque de barras laterales (Pillarbox Blur).
- **Acoplamiento en Compose (`OpenGLVideoSurface.kt`):**
  - `GLSurfaceView` configurado con `EGLContext` versión 2.0/3.0 y modo `RENDERMODE_WHEN_DIRTY`.
  - `SurfaceTexture.OnFrameAvailableListener` solicita renderizado (`requestRender()`) únicamente al recibir una nueva trama decodificada, minimizando el consumo de batería en reposo.
  - El estado `@Volatile` en `OpenGLVideoRenderer` asegura visibilidad atómica instantánea de los cambios de ecualización entre el hilo de Compose y el hilo GL.
  - **Doble Paso de Renderizado para Videos Verticales:** Cuando un video vertical genera barras negras laterales (`hasPillarbox`), se ejecuta un primer pase con escalado `ZOOM/FILL`, radio de desenfoque Gaussiano 9-tap y atenuación de 45%, seguido inmediatamente por el renderizado frontal nítido con la relación de aspecto original (`FIT`).

### 8. Control de Velocidad de Reproducción y Preservación de Tono (Sonic)
- **Time-Stretching con Sonic:**
  - `ExoPlayer.setPlaybackParameters(PlaybackParameters(speed, 1.0f))` utiliza internamente la biblioteca Sonic en el pipeline de decodificación de audio.
  - Mantiene el tono constante (pitch = 1.0f) mientras se escala la velocidad de 0.25x a 2.0x, evitando alteraciones en las frecuencias vocales e instrumentales.
  - La interfaz (`PlaybackSpeedSheet`) expone opciones rápidas de 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x y 2.0x, junto con un control deslizante continuo.

### 9. Automatización CI/CD y Generación Autónoma de Firma Digital
- **Flujo en GitHub Actions (`.github/workflows/build-debug.yml`):**
  - Preparado para desarrolladores que operan desde dispositivos móviles sin PC.
  - Instala dependencias nativas: Android NDK r26d (`26.3.11579264`), CMake 3.22.1 y toolchain de Rust con targets para 32 y 64 bits (`aarch64`, `armv7`, `x86_64`, `i686`).
  - **Directiva Estricta Sin Caché:** Configurado con `cache-disabled: true` y `--no-build-cache` para evitar artefactos residuales o binarios corruptos.
- **Generación de Firma Debug (`scripts/generate_debug_keystore.sh`):**
  - Como los archivos `.keystore` se ignoran en el control de versiones por seguridad, el script genera el archivo `debug.keystore` de forma 100% no interactiva con `keytool` (formato PKCS12, alias `androiddebugkey`, password `android`).
  - Esto garantiza que Gradle encuentre siempre la clave esperada en `signingConfigs.debugConfig` sin detener la ejecución de GitHub Actions ni requerir interacción del usuario.
  - Publica el APK resultante en los artefactos de GitHub Actions (`NovaPlayer-Debug-APK`) con 30 días de retención.

### 10. Búfer de Memoria RAM Adaptativo y Protección Anti-OOM (`PlayerLoadControlHelper`)
- **Problema Abordado:** En terminales con especificaciones reducidas (Android Go, dispositivos con 1 GB o 2 GB de RAM y CPUs de 32 bits), el búfer ilimitado por defecto de ExoPlayer puede asignar más de 200 MB de memoria continua, activando el *Low Memory Killer* (LMK) y forzando el cierre de la aplicación en segundo plano o al decodificar pistas pesadas.
- **Estrategia Implementada:**
  - `PlayerLoadControlHelper` evalúa la memoria física del dispositivo (`ActivityManager.getMemoryInfo`) y la bandera `activityManager.isLowRamDevice`.
  - **Perfil Low RAM / Android Go (≤ 2.5 GB RAM):** Configura `DefaultLoadControl` con:
    - Búfer mínimo: 4.000 ms (4 segundos).
    - Búfer máximo: 10.000 ms (10 segundos).
    - Búfer previo a iniciar reproducción: 1.000 ms (1 segundo).
    - Búfer tras re-búfer: 2.500 ms (2.5 segundos).
    - `targetBufferBytes` y tamaño de bloque de `DefaultAllocator`: Restringido estrictamente a un tope de 16 MB (`16 * 1024 * 1024`).
    - `prioritizeTimeOverSizeThresholds = false` para garantizar que la memoria física prevalezca ante archivos con tasas de bits desmedidas.
  - **Perfil Estándar / Gama Alta (≥ 3 GB RAM):** Asigna un búfer holgado de 15.000 ms a 30.000 ms para saltos temporales ultra rápidos y resistencia frente a caídas de lectura.
- **Diagnóstico y Transparencia:** La pantalla `SettingsScreen` expone en tiempo real el perfil asignado, los segundos de retención y la RAM total del dispositivo.

### 11. Pipeline y Gestión de Subtítulos SRT (.srt) y WebVTT (.vtt)
- **Renderizado de Alta Eficiencia:** Implementado mediante `SubtitleView` de Media3 ui sobre la superficie de video nativa OpenGL. El listener `Player.Listener.onCues` transmite las señales de texto formateadas de forma inmediata al componente visual.
- **Detección Automática de Pistas Internas:** A través de `Player.Listener.onTracksChanged`, se discriminan los grupos de formato `C.TRACK_TYPE_TEXT` para identificar pistas de subtítulos embebidas en contenedores MKV o MP4, detectando etiquetas, códigos de idioma (ISO) y mime types (`text/x-ssa`, `application/x-subrip`, `text/vtt`).
- **Importación de Archivos Externos:** Mediante el Storage Access Framework (`ActivityResultContracts.OpenDocument`), el usuario puede importar archivos locales `.srt` y `.vtt` desde el almacenamiento interno o tarjeta MicroSD.
- **Inyección Dinámica de Subtítulos:** Se construye un `MediaItem.SubtitleConfiguration` con el `Uri` y tipo MIME correspondiente (`MimeTypes.APPLICATION_SUBRIP` o `MimeTypes.TEXT_VTT`), actualizando el `MediaItem` en caliente en ExoPlayer y preservando la posición exacta de reproducción (`currentPosition`).
- **Panel Inferior Modal (`SubtitlesBottomSheet`):**
  - Conmutador general de subtítulos (activa/desactiva la visualización de texto).
  - Lista de pistas disponibles (internas del contenedor y externa cargada).
  - Selector de tamaño tipográfico en 4 niveles (Pequeño 14sp, Normal 18sp, Grande 24sp, Extra Grande 32sp) con vista previa interactiva.
  - Botón de acceso directo `CC` con retroalimentación visual (`CC On` en color esmeralda) en la barra de controles inferior.
  - Estilizado de alto contraste: texto en color blanco puro con trazo y borde negro para máxima legibilidad sobre escenas claras y oscuras.

### 12. Desenfoque Pillarbox y Filtro de Luz Azul en GPU
- **Filtro de Luz Azul / Descanso Visual:** Modulación en el Fragment Shader GLSL que suprime de forma continua y suave las frecuencias azules sin distorsionar agresivamente la luminancia general, introduciendo una suave curvatura ámbar en los canales rojo y verde para ver videos en la oscuridad con confort visual garantizado.
- **Desenfoque Pillarbox (Pillarbox Blur):**
  - Cuando la relación de aspecto del video es menor a la de la superficie (`videoAspect < surfaceAspect`, típico de videos de TikTok, Shorts, reels o videos verticales en pantalla apaisada), en lugar de dejar barras negras opacas, el renderizador realiza una primera pasada con un quad escalado que cubre toda la pantalla.
  - Se aplica un kernel convolucional Gaussiano de 9 muestras con desplazamiento de texels (`uTexelStep * uBlurRadius`) y una atenuación de brillo del 45% (`uBackgroundDim`).
  - Acto seguido, se dibuja el video central nítido sobre el fondo desenfocado. Todo se calcula en hardware sin copias en memoria RAM ni decodificaciones duplicadas.

### 13. Super-Resolución AMD FidelityFX™ FSR 1.0 en OpenGL ES
- **Objetivo y Contexto:** Permite escalar videos de baja resolución (360p, 480p, 720p o videos antiguos/comprimidos) hacia la resolución nativa de la pantalla del dispositivo móvil con una nitidez superior, preservando bordes definidos y minimizando el desenfoque sin incurrir en modelos neuronales pesados ni depender de NPU.
- **Pipeline de Dos Etapas en GLSL:**
  1. **EASU (Edge-Adaptive Spatial Upsampling):**
     - Recolecta un patrón de 9 toques (centro, norte, sur, este, oeste y diagonales).
     - Calcula luminancias de estándar Rec. 709 (`0.2126 R + 0.7152 G + 0.0722 B`).
     - Evalúa un operador gradiente direccional para detectar la orientación del borde local (`gradX`, `gradY`).
     - Interpola muestras tangenciales a lo largo del borde para suavizar aristas sin crear artefactos de escalera (*jaggies*).
  2. **RCAS (Robust Contrast-Adaptive Sharpening):**
     - Evalúa la curva de contraste local del vecindario.
     - Determina la amplitud máxima y mínima local para definir una caja de tolerancia (*neighborhood clamping*).
     - Aplica un factor de afilado dependiente del contraste (`uFsrSharpness`), donde las zonas planas no sufren amplificación de ruido y las zonas con bordes adquieren un realce limpio.
     - El *clamping* estricto elimina los halos y el *ringing* característicos de los filtros de nitidez no adaptativos.
- **Integración UI y Presets:**
  - Control conmutador dinámico en `VideoEqualizerSheet` con distintivo `GPU`.
  - Deslizador de ajuste de nitidez RCAS (0% a 100%, 75% recomendado por AMD para video).
  - Preset instantáneo "Super-Resolución FSR" en `VideoEqualizerState`.

### 14. Arquitectura Modular de Pantallas Independientes de Herramientas
- **Motivación y Desacoplamiento:**
  - En lugar de concentrar todas las configuraciones en un único menú sobrecargado, cada herramienta cuenta con su propia pantalla o panel modular exclusivo e independiente.
  - Esto evita la fatiga visual en pantallas móviles, permite enfocar al usuario en una sola tarea (ecualización, escala, subtítulos, motor de audio) y deja la arquitectura preparada para admitir nuevas herramientas en el futuro de forma desacoplada.
- **Componentes Implementados:**
  - `PlayerToolsSideSheet`: Panel lateral derecho elegante con animación de deslizamiento (`slideInHorizontally`), accesible desde el botón de herramientas en las barras superior e inferior. Presenta accesos directos con iconos y descripciones contextuales para cada herramienta disponible.
  - `VideoEqualizerSheet`: Pantalla modal exclusiva para calibración de color (brillo, contraste, saturación, gamma), nitidez por hardware, modo descanso visual y presets cromáticos.
  - `PillarboxBlurSheet`: Pantalla modal exclusiva para habilitar y ajustar el desenfoque de fondo en videos verticales (radio Gaussiano y atenuación de brillo).
  - `FsrUpscaleSheet`: Pantalla modal exclusiva para Super-Resolución AMD FidelityFX™ FSR 1.0 (activación de EASU y ajuste fino de afilado RCAS).
  - `AspectRatioSheet`: Pantalla modal exclusiva para cambiar la escala geométrica (Ajustar / FIT, Zoom / ZOOM, Llenar / FILL) con indicadores visuales claros.
  - `AudioEngineSheet`: Pantalla modal exclusiva para alternar entre el motor nativo de ultra baja latencia Google Oboe en C++ y Android Media3 AudioTrack.
  - `PlaybackSpeedSheet`: Pantalla modal de control de velocidad (0.25x a 2.0x) con Sonic Pitch activo.
  - `SubtitlesBottomSheet`: Panel modal de gestión de subtítulos internos (MKV/MP4) y externos (.srt/.vtt) con ajuste de tamaño.
  - `SunModeSheet`: Pantalla modal exclusiva para Modo Sol Extremo y Accesibilidad de Alto Contraste en GPU.
  - `VoiceNightAudioSheet`: Pantalla modal exclusiva para Audio Inteligente DSP (Realce de Diálogos y Compresor Dinámico Nocturno en C++).
  - **Bloqueo de Controles (`isControlsLocked`):** Opción en el panel lateral para bloquear los gestos táctiles del reproductor evitando toques accidentales durante la visualización, mostrando un botón flotante semitransparente con animación de pulso para desbloquear rápidamente con un solo toque.

### 15. Modo Sol Extremo / Accesibilidad de Alto Contraste (OpenGL ES Shader)
- **Desafío:** Al reproducir videos al aire libre o bajo luz solar directa, las sombras y detalles oscuros se pierden por los reflejos, forzando al usuario a elevar el brillo al 100% (lo cual sobrecalienta el terminal y drena la batería rápidamente).
- **Solución Técnica:** Se implementa un shader de mapeo tonal adaptativo que eleva dinámicamente las luminancias oscuras mediante una curva de transferencia logarítmica sin quemar las altas luces (`color = mix(color, pow(color, vec3(0.55)) * 1.35 + 0.08, uSunMode)`).
- Ofrece perfiles dedicados para legibilidad extrema y soporte de accesibilidad para usuarios con baja agudeza visual.

### 16. Procesamiento de Audio DSP en Tiempo Real con Google Oboe (C++)
- **Filtro Peaking Vocal (1.5 kHz a 3.5 kHz):** Realce paramétrico en C++ con punto flotante sobre las frecuencias donde se concentra el formante del habla humana. Permite entender los diálogos con claridad sin tener que subir el volumen general de la película.
- **Compresor de Rango Dinámico (DRC / Night Mode):** Seguidor de envolvente en C++ que detecta incrementos súbitos de amplitud (explosiones, golpes, disparos) y aplica reducción de ganancia proporcional en microsegundos, al tiempo que eleva pasajes susurrados.
- Todo el procesamiento ocurre en el hilo de audio de Google Oboe con 0 ms de latencia agregada y protección concurrente con `std::mutex`.

### 17. Transmisión a TV (Casting y Pantalla Compartida) con Fidelidad de Calibración
- **Pregunta Técnica Clave:** ¿Cómo asegurar que los efectos de color (OpenGL ES) y audio (Oboe DSP) suenen y se vean exactamente iguales en un televisor?
- **Ruta de Implementación:**
  1. **Salida por Cable HDMI / DisplayPort (USB Type-C con DisplayManager / Presentation):** El teléfono renderiza la superficie EGL y emite el audio Oboe directamente sobre el `Display` secundario conectado, logrando paridad 100% exacta sin compresión.
  2. **Duplicación Inalámbrica (Miracast / Wi-Fi Direct Display):** El codificador por hardware del sistema captura el framebuffer compuesto de la GPU y el flujo de audio mezclado de Android, preservando los efectos de shader y compresión.
  3. **Streaming Universal Autónomo (Sin Google Cast dependiente de GMS):** Generación de un servidor local HTTP/RTSP en el dispositivo para que cualquier Smart TV (LG webOS, Samsung Tizen, Android TV, Fire TV, Roku) reproduzca el contenido manteniendo total independencia de Google Play Services.

### 18. Anime4K: Reconstrucción y Realce de Trazos para Animación en GPU (GLSL C++)
- **Fundamento y Algoritmo:** Adaptación móvil de los algoritmos de Anime4K (bloc97) integrados en el Fragment Shader nativo de `VideoColorEngine.cpp`:
  - **Detección de Bordes Sobel:** Ponderación con luminancia Rec. 709 para aislar con precisión matemática los contornos en estilos de animación tradicional y cel-shading.
  - **Modo Lite (Reconstrucción Adaptativa Bilateral):** Filtro bilateral direccional de alta velocidad con pesos ponderados por distancia euclidiana de color. Ideal para dispositivos móviles de gama de entrada o sesiones prolongadas con batería.
  - **Modo Pro (Line Darken & Line Thinning):** Realce direccional de trazos oscuros y adelgazamiento de líneas borrosas producidas por escalado bilineal convencional, restaurando el contraste nítido característico del dibujo animado original.
  - **Modo Restauración / Denoise:** Filtro de suavizado de planos 8-conectado con preservación de contornos para suprimir grano y artefactos de compresión en fondos lisos típicos de series clásicas.
- **Pipeline Zero-Copy en GPU:** Se ejecuta directamente sobre la textura `GL_TEXTURE_EXTERNAL_OES` en el espacio de coordenadas nativo sin pasos adicionales en memoria RAM ni conversiones CPU-GPU.
- **Arquitectura de Interfaz y Estado:**
  - `Anime4kMode` en `VideoEqualizerState`: enum con `OFF (0)`, `LITE (1)`, `PRO (2)`, `RESTORE (3)`.
  - `Anime4kStrength`: regulador de intensidad del 10% al 100%.
  - `Anime4KSheet.kt`: pantalla modal dedicada e independiente accesible desde `PlayerToolsSideSheet` (`PlayerToolItem.ANIME4K`).
  - Preset instantáneo `"Anime 4K (Pro)"` añadido a la biblioteca de presets rápidos del ecualizador.

### 19. Persistencia de Preferencias de Audio (`AppPreferences`) y Media3 por Defecto
- **Almacenamiento Local de Preferencias:** Implementado en `AppPreferences.kt` usando `SharedPreferences` privado (`nova_player_preferences`).
- **Motor Predeterminado (Media3):** Por defecto, la aplicación inicia con `AudioEngineType.MEDIA3` garantizando sincronización A/V inmediata, estabilidad en auriculares Bluetooth y compatibilidad universal.
- **Persistencia Reactiva:** `MainViewModel` expone `selectedAudioEngine: StateFlow<AudioEngineType>` inicializado desde `AppPreferences`. Cualquier cambio seleccionado por el usuario en `SettingsScreen` o `AudioEngineSheet` se persiste en disco y se propaga atómicamente a `MainActivity` y `OboeAudioProcessor`, manteniéndose intacto tras cerrar y reabrir la app.

### 20. Sistema de Gestos Táctiles: Salto Rápido por Doble Toque (+5s / -5s)
- **Discriminación Inteligente en `pointerInput` (`awaitEachGesture`):**
  - **Doble Toque en Lateral Izquierdo (`x < width / 2`):** Retrocede el video 5 segundos (-5s) limitando a un mínimo de 0 ms.
  - **Doble Toque en Lateral Derecho (`x >= width / 2`):** Adelanta el video 5 segundos (+5s) limitando a la duración máxima del video.
  - **Ventana Temporal Calibrada:** La detección requiere dos toques en el mismo lateral con un intervalo inferior a 350 ms.
  - **Desacoplamiento del Toque Simple:** Si no ocurre un segundo toque en 280 ms, se ejecuta la alternancia de visibilidad de los controles de pantalla (`showControls = !showControls`). Si se detecta el doble toque, el job del toque simple se cancela de inmediato, evitando parpadeos en los controles.
  - **Retroalimentación Háptica y Visual (HUD):** Emite `HapticFeedbackConstants.KEYBOARD_TAP` y proyecta una cápsula circular animada con icono (`FastRewind` / `FastForward`) y etiqueta numérica (`-5 seg` / `+5 seg`) que se desvanece suavemente a los 700 ms.

### 21. Confirmación de Seguridad Antes de Eliminar Videos de la Biblioteca
- **Prevención de Pérdida Accidental:** En `VideoImportScreen.kt`, al tocar el botón de eliminar de una tarjeta del historial, se activa el estado `videoPendingDelete`.
- **Diálogo Modal Informativo (`AlertDialog`):** Muestra el nombre exacto del archivo, una advertencia explícita aclarando que el archivo original no se borrará del almacenamiento del teléfono, botón destructivo "Eliminar" y botón "Cancelar".

### 22. Aislamiento de Escala de Fuente del Sistema Operativo (`fontScale = 1.0f`)
- **Propósito y Experiencia de Usuario:** Los reproductores multimedia avanzados requieren proporciones tipográficas rigurosas y predecibles en los controles flotantes, paneles laterales, contadores de tiempo y subtítulos para evitar que textos sobredimensionados rompan la diagramación o desborden la pantalla horizontal.
- **Implementación en `Theme.kt`:** 
  - Mediante `CompositionLocalProvider(LocalDensity provides Density(density = originalDensity.density, fontScale = 1.0f))`, toda la jerarquía de Jetpack Compose adopta una escala de fuente exacta `1.0f`.
  - La densidad de píxeles (`dp`) del dispositivo se mantiene intacta, pero el escalado de accesibilidad de texto del sistema (`sp`) se fija al tamaño óptimo y equilibrado del diseño de la aplicación.

### 23. Bloqueo Visual con Candado de Funciones Exclusivas de Google Oboe en Media3
- **Diferenciación Arquitectónica de Motores:**
  - **Google Oboe C++:** Es el único motor con pipeline DSP en tiempo real sobre tramas PCM (Filtro Peaking de Voces Claras y Compresor Dinámico Nocturno DRC).
  - **Android Media3:** Utiliza el pipeline estándar del sistema Android sin el módulo de efectos por hardware en C++.
- **Mecanismo de Candado en UI:**
  - En `PlayerToolsSideSheet.kt`: Al estar seleccionado Media3, la opción *Audio DSP Inteligente* exhibe un icono de candado (`Icons.Default.Lock`), un distintivo ámbar `Bloqueado con Media3` y una descripción indicando que requiere Google Oboe C++.
  - En `VoiceNightAudioSheet.kt`: Se despliega un banner superior de advertencia estética con candado, se bloquean los interruptores (`Switch(enabled = false)`) y presets rápidos, y se provee un botón directo de acción: `"Activar Google Oboe C++ (Desbloquear)"`, permitiendo alternar el motor y habilitar el procesamiento en tiempo real con un solo toque.

### 24. Aceleración con Núcleo Rust y Renderizado de Subtítulos Complejos SSA/ASS (Fase 5)
- **Módulo Nativo Rust (`rust_core` / `libnova_rust.so`):**
  - Compilación cruzada para las 4 arquitecturas soportadas (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) mediante `cargo-ndk`.
  - Carga segura y sin fallbacks falsos en Kotlin (`NovaRustCore.kt`).
  - Parser ultra-eficiente de scripts SSA/ASS:
    - Extrae la resolución de renderizado virtual (`PlayResX` y `PlayResY`) para un posicionamiento proporcional independiente de la pantalla física.
    - Decodifica la tabla de estilos `[V4+ Styles]` (fuente, tamaño, colores primario y contorno con conversión BGR/ABGR a ARGB estándar, bordes y márgenes).
    - Procesa los eventos `[Events] / Dialogue` calculando marcas de tiempo de inicio y fin en milisegundos y limpiando etiquetas de control complejas `{\tag}`.
    - Serialización binaria rápida vía JSON con `serde` / `serde_json`, deserializada en Kotlin mediante `kotlinx.serialization`.
- **Componente de Renderizado `AssSubtitleOverlay` (Jetpack Compose):**
  - Renderizado sincronizado al milisegundo sobre la superficie de video OpenGL.
  - Soporte de alineación geométrica de 9 puntos (estilo teclado numérico del estándar ASS: 1 a 9, ej. Bottom-Left, Bottom-Center, Top-Center, Middle).
  - Efecto de doble contorno (*stroke outline*) para contraste absoluto sobre escenas oscuras o brillantes.
  - Escala tipográfica dinámica gobernada por el ajuste de tamaño (`SubtitleSize`) seleccionado por el usuario.

### 25. Persistencia Granular de Configuraciones por Video en Room Database (SQLite v2)
- **Motivación y Experiencia de Usuario:** Cada video puede requerir ajustes únicos (ej. un video antiguo en baja resolución necesita FSR 1.0 y 1.25x de velocidad; un anime requiere realce Anime4K y subtítulo `.ass`; un video musical requiere Oboe y efecto Haas 3D). La aplicación ahora guarda y restaura de forma completamente individualizada las configuraciones para cada archivo.
- **Entidad `VideoEntity` (Room Database v2):**
  - Se añadieron columnas específicas para almacenar el estado completo:
    - `playbackSpeed`: Velocidad (0.25x a 2.0x).
    - `aspectRatioMode`: Ajuste geométrico (Fit, Zoom, Fill).
    - `audioEngine`: Motor de audio (Media3 u Oboe).
    - `audioChannelMode`: Enrutamiento estéreo, mono o Haas 3D.
    - `subtitlesEnabled` y `subtitleSize`: Estado de visualización y tamaño de subtítulos.
    - `externalSubtitleUri` y `externalSubtitleName`: Referencia al archivo de subtítulo externo cargado.
    - 12 parámetros de Shaders OpenGL ES: brillo, contraste, saturación, gamma, nitidez, luz azul, Pillarbox Blur, AMD FSR 1.0 (activo y nitidez RCAS), Modo Sol Extremo (activo e intensidad), y Anime4K (modo y fuerza).
- **Sincronización en Caliente:**
  - `VideoPlayerScreen` inicializa sus controles a partir de `initialVideoEntity`.
  - Cada vez que el usuario ajusta un parámetro o cierra un panel modal de herramientas (`onDismiss`), se dispara `saveSettings()` persistiendo las preferencias en la base de datos a través de `MainViewModel`.
  - La persistencia también se asegura al salir del reproductor (`BackHandler` y `onBack`).


