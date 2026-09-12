# Hoja de Ruta (Roadmap) - Nova Video Player

Este documento detalla las fases de desarrollo planificadas para convertir a **Nova Video Player** en un reproductor multimedia móvil avanzado, eficiente y adaptable a cualquier gama de hardware.

---

## 📍 Estado del Proyecto: En Desarrollo Activo

```text
[✅] Fase 1: Base de Importación y Reproducción de Video
[✅] Fase 2: Configuración Nativa C++, Google Oboe y Rust
[✅] Fase 3: Pipeline de Renderizado con OpenGL ES y Ecualizador de Video
[🔄] Fase 4: Procesamiento de Subtítulos y Metadatos en Rust (Próximo hito)
[⏳] Fase 5: Optimizaciones Extremas para Android Go y 32 Bits
[⏳] Fase 6: Empaquetado y Distribución Externa (Uptodown / APK Autónomo)
```

---

## 🎯 Desglose por Fases

### ✅ Fase 1: Importación Flexible, Biblioteca y Reproducción Base (Completada)
- [x] Selector dual de entrada: Galería del sistema (`PickVisualMedia`) y Gestor de Archivos nativo (`OpenDocument` / SAF).
- [x] **Biblioteca Local Persistente con Room (SQLite):** Registro de archivos importados y vistos mostrando título completo, duración real formateada (`MediaMetadataRetriever`), tamaño en MB/GB y barra de progreso.
- [x] Reanudación con un solo toque directamente desde el último punto de visualización guardado en base de datos.
- [x] Interfaz de usuario inspirada en controles de PC (Seekbar, saltos de 10s, modos de relación de aspecto).
- [x] **Gestos Táctiles con HUD Minimalista:** Control de brillo deslizando en la mitad izquierda y control de volumen físico en la mitad derecha con indicador flotante no invasivo.
- [x] Gestión de pantalla encendida (`FLAG_KEEP_SCREEN_ON`), restauración de brillo y ciclo de vida de la actividad.
- [x] Compatibilidad con formatos populares (MP4, MKV, WebM, AVI, MOV).

---

### ✅ Fase 2: Motores de Audio y Capa Nativa (Completada)
- [x] Elevación de versión mínima de sistema a **Android 8.0 (API 26)** para garantizar acceso nativo a AAudio.
- [x] Integración de **Google Oboe 1.9.3** mediante Prefab y CMake en C++17.
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
- [x] Fragment shader dinámico GLSL ejecutado 100% en GPU para ecualización en tiempo real sin pausar la reproducción:
  - Brillo dinámico con desplazamiento perceptual [-0.5f a 0.5f].
  - Contraste con pivote en gris neutro [0.5f a 2.0f].
  - Saturación cromática Rec. 709 [0.0f a 2.0f].
  - Corrección de rango dinámico Gamma [0.5f a 2.0f].
  - Filtro de nitidez (*sharpening*) mediante convolución Laplaciano 3x3 en GPU.
- [x] Presets preconfigurados instantáneos: *Normal*, *Vívido*, *Cine*, *Nocturno*, *Alto Contraste* y *Blanco y Negro*.
- [x] Control de velocidad de reproducción (hasta 2.0x) con *Sonic Pitch Preservation* activo sin distorsión de audio.
- [x] Componentes modulares Jetpack Compose: `OpenGLVideoSurface`, `VideoEqualizerSheet` y `PlaybackSpeedSheet`.

---

### 🔄 Fase 4: Núcleo de Rendimiento en Rust (Parsing y Subtítulos - Próximo Hito)
- [ ] Enlace bidireccional JNI / FFI entre Rust y la capa de aplicación.
- [ ] Parser de subtítulos multiformato (SRT, VTT, SSA/ASS) con renderizado vectorial optimizado.
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

### ⏳ Fase 6: Empaquetado y Distribución Libre
- [x] **Pipeline de Integración Continua (CI/CD) con GitHub Actions (`build-debug.yml`):**
  - Descarga integral del repositorio y configuración automática de herramientas nativas (NDK r26d, CMake 3.22.1 y Rust stable con targets Android).
  - Compilación limpia forzada (**sin caché**) con flags `--no-build-cache` y `cache-disabled: true`.
  - Script automatizado de generación de firma debug (`scripts/generate_debug_keystore.sh`) que genera el keystore desde cero mediante `keytool` sin bloqueos interactivos.
  - Generación y publicación automática del APK de depuración como artefacto descargable directamente en teléfonos móviles.
- [ ] Optimización de ProGuard / R8 para reducir tamaño del binario y ofuscar código sensible.
- [ ] Pruebas exhaustivas de instalación directa de APK sin servicios de Google Play (GMS independiente).
- [ ] Generación de builds separados por arquitectura (`armeabi-v7a`, `arm64-v8a`) y build universal para Uptodown.
