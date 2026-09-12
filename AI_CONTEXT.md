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

### 4. Navegación y Pantalla de Configuración Independiente
- La arquitectura de navegación utiliza la máquina de estados `AppScreen` (`HOME`, `PLAYER`, `SETTINGS`) en `MainActivity.kt`.
- `SettingsScreen` es una pantalla completa e independiente (sin modales ni tarjetas emergentes) que permite alternar en caliente el motor de audio (`AudioEngineType`), ejecutar una prueba física de sonido senoidal de 440 Hz en tiempo real y supervisar telemetría JNI periódica (`getApiName`, `getSampleRate`, `getChannelCount`, `getFramesWritten`).
- Al regresar (mediante el botón de navegación del TopAppBar o el `BackHandler` del sistema), se restaura el contexto previo y se reanuda la reproducción en la posición exacta (`currentPlaybackPositionMs`).

### 5. Persistencia Local y Biblioteca Multimedia (Room Database)
- Implementación de base de datos SQLite con **Room** (`AppDatabase`, `VideoDao`, `VideoEntity`).
- El repositorio `VideoRepository` encapsula la persistencia y emite de manera reactiva un `Flow<List<VideoEntity>>` ordenado por última interacción (`lastPlayedTimestamp DESC`).
- `MainViewModel` (hereda de `AndroidViewModel`) expone la lista mediante `StateFlow` y `SharingStarted.WhileSubscribed(5000)`.
- `VideoUtils.resolveVideoMetadata` y `VideoUtils.getVideoDurationMs` utilizan `MediaMetadataRetriever` para calcular la duración exacta del video en milisegundos y formatearla a `mm:ss` o `hh:mm:ss`.
- `VideoPlayerScreen` informa periódicamente el progreso (`currentPositionMs` y `totalDurationMs`), actualizando la base de datos para mostrar barras de avance y permitir reanudación instantánea con un toque desde la pantalla principal (`VideoImportScreen`).

### 6. Sistema de Gestos Táctiles y HUD Minimalista
- **Detección Directa y Desacoplada (`pointerInput` con `awaitEachGesture`):**
  - Se utiliza una capa interactiva sobre la vista de video nativa para discriminar toques simples de arrastres verticales sin colisiones de eventos.
  - **Mitad Izquierda del Canvas:** Ajusta progresivamente el brillo de pantalla de la ventana (`WindowManager.LayoutParams.screenBrightness`) en un rango de `0.01f` a `1.0f`. Al salir de la pantalla o cerrar el reproductor, se restaura automáticamente el valor predeterminado del sistema (`BRIGHTNESS_OVERRIDE_NONE`).
  - **Mitad Derecha del Canvas:** Modifica directamente el volumen físico multimedia del dispositivo (`AudioManager.STREAM_MUSIC`), sincronizando el estado con el reproductor y reactivando el audio si se encontraba silenciado.
  - **Indicador Flotante Minimalista (`MinimalistGestureIndicator`):** Cápsula elegante que aparece flotando en el lateral activo exclusivamente mientras se realiza el gesto (`AnimatedVisibility` con `fadeIn` y `scaleIn`). Incorpora icono dinámico contextual (según tramos de volumen/brillo), barra vertical graduada con gradiente y porcentaje numérico. Se desvanece suavemente 1 segundo después de finalizar el gesto.

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




