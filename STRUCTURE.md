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
│   └── generate_debug_keystore.sh # Generador no interactivo de 'debug.keystore' para CI/CD
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
├── app/                         # Módulo principal de Android (Kotlin + C++)
│   ├── .gitignore               # Exclusiones locales de compilación y .cxx
│   ├── build.gradle.kts         # Configuración del módulo de aplicación (NDK, Prefab, CMake)
│   ├── proguard-rules.pro       # Reglas de ofuscación y conservación de JNI
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Manifiesto de permisos y actividades
│       │   │
│       │   ├── cpp/                  # Capa Nativa C++ (Audio Oboe y OpenGL ES Shaders)
│       │   │   ├── CMakeLists.txt    # Script de compilación CMake (enlaza Oboe, GLESv2, EGL, log)
│       │   │   ├── native-lib.cpp    # Puntos de entrada JNI (puente hacia Kotlin)
│       │   │   ├── OboeAudioEngine.h # Declaración de la clase del motor de audio Oboe
│       │   │   ├── OboeAudioEngine.cpp # Implementación nativa de flujos AAudio y OpenSL ES
│       │   │   ├── VideoColorEngine.h # Declaración del motor de sombreadores OpenGL ES
│       │   │   └── VideoColorEngine.cpp # Shaders GLSL, texturizado OES, matriz de color y nitidez
│       │   │
│       │   ├── java/com/example/     # Código fuente Kotlin (UI y Lógica)
│       │   │   ├── MainActivity.kt   # Actividad raíz y enrutador de pantallas
│       │   │   │
│       │   │   ├── audio/            # Capa de integración de audio nativo
│       │   │   │   ├── AudioEngineType.kt     # Enum: OBOE vs MEDIA3
│       │   │   │   ├── OboeAudioEngine.kt     # Wrapper JNI de control del motor C++
│       │   │   │   └── OboeAudioProcessor.kt  # Interceptor PCM de Media3 hacia Oboe
│       │   │   │
│       │   │   ├── opengl/           # Capa de renderizado acelerado por GPU
│       │   │   │   ├── NativeVideoFilter.kt   # Puente JNI con VideoColorEngine en C++
│       │   │   │   ├── VideoEqualizerState.kt # Modelo de parámetros de ecualización y presets
│       │   │   │   └── OpenGLVideoSurface.kt  # GLSurfaceView.Renderer y Composable OpenGLVideoPlayerView
│       │   │   │
│       │   │   ├── data/             # Persistencia local con Room (SQLite)
│       │   │   │   ├── AppDatabase.kt         # Base de datos Room singleton
│       │   │   │   ├── VideoDao.kt            # Operaciones reactivas DAO con Flow
│       │   │   │   ├── VideoEntity.kt         # Entidad persistente de video importado/visto
│       │   │   │   └── VideoRepository.kt     # Abstracción y operaciones asíncronas
│       │   │   │
│       │   │   ├── model/            # Modelos de datos
│       │   │   │   └── VideoItem.kt  # Modelo de metadatos de video (URI, nombre, tamaño, duración)
│       │   │   │
│       │   │   ├── ui/               # Componentes visuales Jetpack Compose
│       │   │   │   ├── AspectRatioMode.kt     # Modos de relación de aspecto geométrico (FIT, ZOOM, FILL)
│       │   │   │   ├── MainViewModel.kt       # ViewModel central de la biblioteca e importados
│       │   │   │   ├── PlaybackSpeedSheet.kt  # Panel inferior de velocidad de reproducción (hasta 2x)
│       │   │   │   ├── SettingsScreen.kt      # Pantalla independiente de configuración de motores, test de sonido y telemetría
│       │   │   │   ├── VideoEqualizerSheet.kt # Panel inferior del ecualizador de video con sliders y presets
│       │   │   │   ├── VideoImportScreen.kt   # Pantalla interactiva: biblioteca de importados, duración, progreso y selectores
│       │   │   │   ├── VideoPlayerScreen.kt   # Pantalla de reproducción multimedia con OpenGL, gestos y HUD
│       │   │   │   ├── VideoSourceDialog.kt   # Diálogo para alternar Galería / Gestor de archivos
│       │   │   │   └── theme/                 # Paleta de colores, tipografía y tema oscuro
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   │
│       │   │   └── utils/            # Utilidades auxiliares
│       │   │       └── VideoUtils.kt # Extracción y formateo seguro de metadatos y duración
│       │   │
│       │   └── res/                  # Recursos de la aplicación (strings, drawables)
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
 Contraste, Saturación,         │
 Gamma, Sharpening GPU)         │
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
2. **Procesador de Audio en Pipeline (`OboeAudioProcessor`):** Al insertarse como procesador de audio dentro de `DefaultAudioSink`, no se requiere re-instanciar el reproductor para alternar entre Oboe y AudioTrack.
3. **Pipeline de Video Acelerado por GPU (OpenGL ES & C++):** El decodificador por hardware escribe directamente en un `SurfaceTexture` conectado a `GL_TEXTURE_EXTERNAL_OES`. El renderizado pasa por un pipeline nativo en C++ (`VideoColorEngine`) que aplica correcciones de color y convolución en el fragment shader sin provocar pausas ni consumir ciclos de CPU.
4. **Preservación Acústica de Tono (Sonic Pitch Preservation):** El control de velocidad (hasta 2.0x) implementa el algoritmo Sonic integrado en Media3, permitiendo acelerar o ralentizar la reproducción conservando la afinación y timbre de voces e instrumentos.
5. **Soporte Arquitectural Universal:** La compilación de C++ y Rust está parametrizada para generar binarios tanto en 32 bits (`armeabi-v7a`, `x86`) como en 64 bits (`arm64-v8a`, `x86_64`), permitiendo que el mismo código fuente ejecute en cualquier dispositivo.
6. **Sin Dependencias de Servicios Propietarios:** No se utilizan APIs de Google Play Services ni bibliotecas cerradas con licencias restrictivas, facilitando la publicación en plataformas abiertas y tiendas alternativas.
7. **Pantalla Independiente de Configuración:** En sustitución de diálogos emergentes o tarjetas modales, la configuración se aloja en una pantalla propia (`SettingsScreen`) con navegación limpia (`AppScreen`), preservación de la posición del video en reproducción, verificación física de salida mediante síntesis senoidal en vivo y telemetría nativa JNI continua.
8. **Persistencia Local Reactiva con Room:** Registro estructurado de videos importados y vistos en SQLite local (`VideoEntity`, `VideoDao`, `AppDatabase`). La interfaz observa de forma reactiva un `Flow<List<VideoEntity>>` a través de `MainViewModel`, mostrando títulos, duraciones, tamaños, barras de progreso y reanudación instantánea desde la última posición guardada.
9. **Compilación en la Nube Autónoma (CI/CD sin Caché):** Flujo de GitHub Actions (`build-debug.yml`) que garantiza compilación limpia desde cero con generación no interactiva de firma digital (`generate_debug_keystore.sh`). Permite a desarrolladores sin PC compilar y descargar el APK de depuración directamente en su teléfono móvil.
