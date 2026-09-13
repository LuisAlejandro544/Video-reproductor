# Hoja de Ruta (Roadmap) - Nova Video Player

Este documento detalla las fases de desarrollo planificadas para convertir a **Nova Video Player** en un reproductor multimedia móvil avanzado, eficiente y adaptable a cualquier gama de hardware.

---

## 📍 Estado del Proyecto: En Desarrollo Activo

```text
[✅] Fase 1: Base de Importación y Reproducción de Video
[✅] Fase 2: Configuración Nativa C++, Google Oboe y Rust
[✅] Fase 3: Pipeline de Renderizado con OpenGL ES y Ecualizador de Video
[✅] Fase 4: Subtítulos SRT/VTT y Búfer de Memoria RAM Adaptativo (Android Go)
[🔄] Fase 5: Aceleración con Núcleo Rust y Formatos Complejos (SSA/ASS)
[⏳] Fase 6: Renderizado Gráfico Adaptativo Vulkan 1.1+
[⏳] Fase 7: Empaquetado y Distribución Externa (Uptodown / APK Autónomo)
```

---

## 🎯 Desglose por Fases

### ✅ Fase 1: Importación Flexible, Biblioteca y Reproducción Base (Completada)
- [x] Selector dual de entrada: Galería del sistema (`PickVisualMedia`) y Gestor de Archivos nativo (`OpenDocument` / SAF).
- [x] **Biblioteca Local Persistente con Room (SQLite):** Registro de archivos importados y vistos mostrando título completo, duración real formateada (`MediaMetadataRetriever`), tamaño en MB/GB y barra de progreso.
- [x] Reanudación con un solo toque directamente desde el último punto de visualización guardado en base de datos.
- [x] Interfaz de usuario inspirada en controles de PC (Seekbar, saltos de 10s, modos de relación de aspecto).
- [x] **Modo Inmersivo Completo:** Ocultamiento automático de la barra de estado (reloj, batería, notificaciones) y barra de navegación durante la reproducción.
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
- [x] Presets preconfigurados instantáneos: *Normal*, *Super-Resolución FSR*, *Descanso Visual*, *Vívido*, *Cine*, *Nocturno*, *Alto Contraste* y *Blanco y Negro*.
- [x] Control de velocidad de reproducción (hasta 2.0x) con *Sonic Pitch Preservation* activo sin distorsión de audio.
- [x] **Arquitectura Modular de Herramientas del Reproductor (Pantallas Independientes):**
  - Panel lateral derecho interactivo (`PlayerToolsSideSheet`) para navegación limpia y desacoplada entre herramientas.
  - Pantallas y hojas exclusivas e independientes para cada funcionalidad:
    - `VideoEqualizerSheet`: Ajuste dedicado de brillo, contraste, saturación, gamma, nitidez y presets de color.
    - `SunModeSheet`: Pantalla independiente de compensación para exteriores bajo luz solar intensa y perfiles de alto contraste para accesibilidad visual.
    - `PillarboxBlurSheet`: Control exclusivo para desenfoque y relleno de barras laterales en videos verticales.
    - `FsrUpscaleSheet`: Pantalla dedicada de Super Resolución AMD FidelityFX FSR 1.0 (EASU + RCAS).
    - `VoiceNightAudioSheet`: Pantalla dedicada de Audio Inteligente DSP (Modo Voces Claras y Compresor Dinámico Nocturno en C++).
    - `AspectRatioSheet`: Selección de modo de pantalla geométrico (Ajustar, Zoom, Llenar).
    - `AudioEngineSheet`: Conmutador de motor de audio (Oboe C++ vs Media3 AudioTrack).
    - `PlaybackSpeedSheet`: Selector de velocidad de reproducción (hasta 2.0x).
    - `SubtitlesBottomSheet`: Gestión de subtítulos internos y externos con selector tipográfico.
    - Modo de Bloqueo de Pantalla (`Lock`): Desactiva toques y gestos accidentales con botón flotante de desbloqueo.
- [x] **Procesamiento de Audio DSP en Tiempo Real con Google Oboe en C++:**
  - **Filtro Peaking Vocal (1.5 kHz a 3.5 kHz):** Realce inteligente de diálogos y frecuencias fonéticas clave.
  - **Compresor de Rango Dinámico (DRC):** Normalización de picos para cine nocturno (suaviza explosiones y levanta susurros).
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
- [x] Componentes modulares Jetpack Compose: `OpenGLVideoSurface`, `VideoEqualizerSheet`, `StereoMonoSheet`, `SunModeSheet`, `VoiceNightAudioSheet`, `PillarboxBlurSheet`, `FsrUpscaleSheet`, `AspectRatioSheet`, `AudioEngineSheet` y `PlaybackSpeedSheet`.

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

---

### 🔄 Fase 5: Aceleración con Núcleo Rust y Formatos Complejos (Próximo Hito)
- [ ] Enlace bidireccional JNI / FFI entre Rust y la capa de aplicación.
- [ ] Parser nativo de subtítulos con formateo avanzado (SSA/ASS y subtítulos vectoriales).
- [ ] Extracción rápida de metadatos de archivos de video sin bloquear la interfaz.
- [ ] Búfer circular en memoria para pre-carga de tramas multimedia.

---

### ⏳ Fase 5: Renderizado Gráfico Adaptativo Vulkan 1.1+ y Optimizaciones
- [ ] **Detección Dinámica de Capacidades de Hardware:** Consulta en tiempo de ejecución de `FEATURE_VULKAN_HARDWARE_VERSION` para detectar soporte de Vulkan 1.1+ (`0x401000`).
- [ ] **Arquitectura con Degradación Elegante (*Graceful Fallback*):**
  - Dispositivos con Vulkan 1.1+: Canal de renderizado nativo C++ con extensión `VK_ANDROID_external_memory_android_hardware_buffer` para menor sobrecarga de CPU y consumo de batería.
  - Dispositivos sin soporte o con versiones previas (Vulkan 1.0): Renderizado automático y transparente con el pipeline probado de OpenGL ES 2.0 / 3.0.
- [ ] **Selector Inteligente en Pantalla de Configuración (`SettingsScreen`):**
  - Opción interactiva para elegir entre motor Vulkan y OpenGL ES cuando el hardware lo soporte.
  - Bloqueo visual con mensaje informativo si el procesador no cuenta con Vulkan 1.1+.
- [ ] Perfiles automáticos de uso de memoria RAM (límite estricto de búferes en dispositivos de 1GB/2GB y Android Go).
- [ ] Estrategia de reducción de resolución de texturas intermedias si la GPU reporta sobrecarga.
- [ ] Desactivación selectiva de shaders pesados en dispositivos con procesadores ARMv7 de 32 bits.

---

### ⏳ Fase 6: Transmisión y Pantalla Compartida a TV (Casting / Mirroring con Fidelidad Total)
- [ ] **Soporte de Pantallas Secundarias (`DisplayManager` / `Presentation`):**
  - Salida directa por cable USB-C a HDMI / DisplayPort (Modo Escritorio / Samsung DeX / Display externo): renderiza el pipeline completo de OpenGL ES y audio Oboe nativo con calibración idéntica a la pantalla del móvil.
- [ ] **Protocolo de Pantalla Inalámbrica (Miracast / Wi-Fi Display):**
  - Duplicación de pantalla a nivel del framebuffer del sistema: transmite directamente el flujo postprocesado con shaders y DSP de audio.
- [ ] **Servidor de Streaming Local RTSP/HTTP (Sin dependencias de Google Cast ni Play Services):**
  - Arquitectura compatible con Smart TVs universales (LG webOS, Samsung Tizen, Android TV, Fire TV, Roku y navegadores DLNA/UPnP) transmitiendo el flujo con las correcciones de color y audio preservadas.

---

### ⏳ Fase 7: Empaquetado y Distribución Libre
- [x] **Pipeline de Integración Continua (CI/CD) con GitHub Actions (`build-debug.yml`):**
  - Descarga integral del repositorio y configuración automática de herramientas nativas (NDK r26d, CMake 3.22.1 y Rust stable con targets Android).
  - Compilación limpia forzada (**sin caché**) con flags `--no-build-cache` y `cache-disabled: true`.
  - Script automatizado de generación de firma debug (`scripts/generate_debug_keystore.sh`) que genera el keystore desde cero mediante `keytool` sin bloqueos interactivos.
  - Generación y publicación automática del APK de depuración como artefacto descargable directamente en teléfonos móviles.
- [ ] Optimización de ProGuard / R8 para reducir tamaño del binario y ofuscar código sensible.
- [ ] Pruebas exhaustivas de instalación directa de APK sin servicios de Google Play (GMS independiente).
- [ ] Generación de builds separados por arquitectura (`armeabi-v7a`, `arm64-v8a`) y build universal para Uptodown.
