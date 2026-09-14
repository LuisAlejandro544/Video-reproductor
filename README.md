# Nova Video Player

> Reproductor multimedia avanzado de alto rendimiento para Android con arquitectura híbrida (Kotlin Jetpack Compose, C++ nativo con Google Oboe y núcleo Rust).

---

## 📋 Descripción General

**Nova Video Player** es un reproductor de video y audio móvil diseñado con los estándares de control, fluidez y flexibilidad de los reproductores clásicos de escritorio. Su arquitectura está orientada al máximo rendimiento en una amplia variedad de dispositivos, desde terminales de gama alta hasta teléfonos con especificaciones ajustadas (Android Go) y procesadores tanto de **32 bits (ARMv7, x86)** como de **64 bits (ARM64, x86_64)**.

---

## ✨ Características Principales

- **Efectos de Sonido de Interfaz Táctil (UI Clicks con SoundPool de Ultra Baja Latencia y Doble Capa):**
  - **Retroalimentación Acústica Nítida y Universal:** Respuesta auditiva inmediata en botones de control, navegación de subpantallas de ajustes, panel lateral de herramientas, selección de videos e indicadores gestuales.
  - **Enrutamiento por Flujo Multimedia (`STREAM_MUSIC`):** Configurado con `AudioAttributes.USAGE_MEDIA` para garantizar audición nítida sin depender de los ajustes restrictivos de "sonidos del sistema" de Android ni silenciarse con el modo vibración.
  - **Muestra Acústica Calibrada (`ui_click.ogg` a 48 kHz mono):** Duración acústica ajustada a 42 ms con ataque suave anti-pop, chasquido a 2.4 kHz y cuerpo a 520 Hz, evitando ruidos espurios y garantizando decodificación Vorbis perfecta sin truncamiento por encendido de DAC.
  - **Degradación Elegante (Dual-Tier Fallback):** Respaldo automático mediante `AudioManager.playSoundEffect` para evitar pulsaciones mudas durante la precarga o saturación de canales.
  - **Ajuste y Control en Pantalla:** Interruptor en *Configuración > Apariencia* para activar o desactivar los efectos sonoros en cualquier momento, con persistencia en `AppPreferences`.
  - **Herramienta de Optimización de Audio (`scripts/convert_audio_asset.sh`):** Script automatizado para convertir cualquier muestra de audio (WAV/MP3/FLAC) a Ogg Vorbis mono optimizado para móviles de recursos limitados.
- **Indicadores Gestuales Dinámicos y Transiciones Cinemáticas entre Pantallas:**
  - **Indicadores HUD Reactivos (`PlayerHudIndicators`):** Interpolación suave con físicas de resorte (`spring`) en los medidores flotantes de volumen y brillo, expansión adaptativa de cápsula y resplandor cromático según el nivel.
  - **Animación Elástica de Doble Toque (+5s / -5s):** Pulso con rebote dinámico y resplandor para confirmar el salto temporal en reproducción.
  - **Insignia Pulsante de Avance Rápido 2X:** Latido continuo y brillo cíclico mientras se mantiene presionada la pantalla.
  - **Transición Fluida entre Pantallas (`AnimatedContent`):** Deslizamientos cinemáticos verticales para el reproductor y horizontales para la configuración con desvanecimientos combinados (`togetherWith`).
- **Asistente de Bienvenida y Configuración Guiada (`OnboardingScreen`):**
  - **Experiencia Inicial Paso a Paso:** Al abrir la aplicación por primera vez, un asistente visual con indicador de progreso guía al usuario para personalizar la experiencia según su hardware y preferencias:
    - **Paso 1 (Bienvenida y Permisos):** Solicitud interactiva y transparente de permisos de almacenamiento (`READ_MEDIA_VIDEO` en Android 13+ y `READ_EXTERNAL_STORAGE` en Android 8 a 12), con distintivo de estado en tiempo real.
    - **Paso 2 (Motor de Audio):** Comparativa detallada entre **Android Media3** (AudioTrack estándar con sincronización Bluetooth óptima) y **Google Oboe C++** (ultra baja latencia sin capas intermedias), desglosando ventajas y desventajas técnicas.
    - **Paso 3 (Motor Gráfico):** Detección en tiempo real de compatibilidad con **Vulkan 1.1+**. Si el teléfono lo soporta, permite elegir entre **OpenGL ES 3.0+** (probado, estable y con soporte universal de efectos) o **Vulkan 1.1+** (bajo nivel con menor consumo energético y sobrecarga de CPU, indicando explícitamente que se encuentra en desarrollo activo de funciones). Si el hardware carece de Vulkan 1.1+, selecciona automáticamente OpenGL ES explicando el motivo.
    - **Paso 4 (Apariencia y Colores):** Elección entre Modo Oscuro (OLED/Cine), Modo Claro (Exteriores) y Seguir al Sistema, con interruptor para activar o desactivar Material You (colores dinámicos extraídos del fondo de pantalla en Android 12+).
    - **Paso 5 (Modo de Descubrimiento):** Elección entre habilitar el escaneo de carpetas de mensajería (WhatsApp y Telegram) o el Modo Privado (solo importar archivos manualmente).
    - **Paso 6 (Resumen y Comienzo):** Tarjeta con el resumen de todas las opciones seleccionadas y botón directo de inicio a la biblioteca.
- **Descubrimiento y Escaneo Inteligente de Videos de Mensajería (WhatsApp y Telegram):**
  - **Escaneo Local Directo (`MessagingMediaScanner`):** Detecta e indexa automáticamente en la biblioteca local de Room los videos recibidos y descargados en las carpetas públicas de WhatsApp (`/WhatsApp/Media/WhatsApp Video/`) y Telegram (`/Telegram/Telegram Video/`).
  - **100% Privado y Autónomo:** Todo el escaneo se realiza de forma estrictamente local en el dispositivo; ningún dato ni archivo es enviado a la nube ni a servidores externos.
  - **Tarjeta de Control Dinámica:** En la pantalla principal (`VideoImportScreen`), una tarjeta dedicada permite visualizar el estado del escaneo y refrescar con un toque para detectar nuevos videos recibidos en cualquier momento.
- **Personalización y Apariencia Material You (Modo Oscuro, Claro y del Sistema con Color Dinámico):**
  - **Selector de Tema Completo:** Alternancia fluida entre **Modo Oscuro** (óptimo para salas de cine y ahorro de batería en pantallas OLED), **Modo Claro** (máxima visibilidad en exteriores bajo luz solar) y **Seguir al Sistema** (sincronizado automáticamente con las preferencias del sistema operativo).
  - **Color Dinámico Material You (Android 12+ / API 31+):** Adaptación cromática armoniosa que extrae y aplica automáticamente los tonos primarios y secundarios del fondo de pantalla (*wallpaper*) del usuario mediante `dynamicDarkColorScheme` y `dynamicLightColorScheme`.
  - **Interruptor de Control Manual:** Opción para activar o desactivar el color dinámico a voluntad, recurriendo a una cuidada paleta personalizada de alto contraste en caso de desactivarse o en dispositivos con versiones anteriores de Android.
  - **Subpantalla Dedicada (`AppearanceSubScreen`):** Módulo exclusivo dentro del Centro de Configuración con tarjeta de previsualización en vivo en tiempo real de los colores activos y persistencia inmediata en disco (`AppPreferences`).
  - **Aislamiento Tipográfico Permanente:** Mantiene fija la escala de texto en `fontScale = 1.0f` para prevenir desbordamientos o solapamientos visuales bajo cualquier combinación de tema.
- **Herramienta de Canales de Audio en Tiempo Real (Estéreo / Mono / Pseudo-Estéreo Haas 3D):**
  - **Conversión Mono a Estéreo en Tiempo Real:** Corrige videos grabados con un solo micrófono o pista mono duplicando y enrutando la señal a ambos auriculares o altavoces.
  - **Modo Mono Centrado:** Mezcla balanceada `(L + R) / 2` para balancear pistas desequilibradas o escuchar con un único auricular.
  - **Pseudo-Estéreo Espacial Haas (DSP C++):** Algoritmo psicoacústico basado en el efecto Haas que introduce un retardo interaural calibrado de 15 ms en el canal derecho mediante búfer circular en C++, creando una sensación de espacialidad tridimensional y profundidad envolvente a partir de pistas mono o estéreo planas, sin degradar la fase acústica.
  - Conmutación en caliente en tiempo real sin pausar el video a través de `StereoMonoSheet` y el panel de herramientas.
- **Control Inteligente de Orientación por Sensor de Hardware (Giro Forzado Independiente):**
  - Sensor de acelerómetro y giroscopio (`OrientationEventListener`) activo durante la reproducción:
    - Permite que la pantalla gire a horizontal o vertical según cómo se sostenga el teléfono, **incluso si el usuario tiene desactivada la opción de 'Giro Automático' en los ajustes de Android**.
    - Retorno automático y garantizado a orientación vertical (`SCREEN_ORIENTATION_PORTRAIT`) al terminar el video (`STATE_ENDED`), al pulsar Atrás o al regresar a la biblioteca principal.
- **Centro de Configuración Ergonómico Modular por Pantallas Independientes (`SettingsScreen`):**
  - Reemplazo de la vista monolítica por un menú de acceso categorizado con navegación a subpantallas dedicadas y exclusivas:
    - **Motor de Audio:** Selección entre Google Oboe C++ y Android Media3.
    - **Canales de Audio:** Enrutamiento estéreo, mono centrado y espacial Haas 3D.
    - **Prueba de Sonido:** Generador senoidal PCM de 440 Hz para validación física de salida de hardware.
    - **Telemetría y Rendimiento:** Monitoreo en tiempo real de tramas C++, buffers de memoria y perfil Android Go.
    - **Arquitectura y Distribución:** Información de compilación 32/64 bits, licencias permisivas y portabilidad APK para Uptodown.
- **Soporte y Gestión de Subtítulos Avanzados: SRT (.srt), WebVTT (.vtt) y SSA/ASS (.ass / .ssa con Rust Core):**
  - **Motor Nativo Rust de Subtítulos Complejos (`NovaRustCore`):**
    - Parsing sin copias en Rust (`libnova_rust.so`) de scripts SSA/ASS, extrayendo resolución virtual (`PlayResX`/`PlayResY`), estilos tipográficos completos y eventos de diálogo.
    - Componente Jetpack Compose dedicado `AssSubtitleOverlay`: posicionamiento proporcional virtual, alineación geométrica de 9 puntos (estilo teclado numérico ASS), doble contorno tipográfico de alto contraste y conversión de paletas cromáticas BGR/ABGR a RGB/ARGB.
  - Renderizado en tiempo real sincronizado mediante `SubtitleView` sobre la superficie de video OpenGL para pistas estándar (SRT/VTT).
  - Detección automática y selección de pistas de subtítulos internas integradas en contenedores MKV/MP4.
  - Carga e importación de archivos de subtítulos externos (`.srt`, `.vtt`, `.ass`, `.ssa`) desde el almacenamiento del dispositivo o tarjeta MicroSD mediante el Storage Access Framework.
  - Panel modal inferior de configuración rápida (`SubtitlesBottomSheet`):
    - Activación y desactivación instantánea de subtítulos.
    - Selector interactivo de pistas disponibles con indicación del idioma o archivo cargado y etiqueta de metadatos ASS (resolución y estilos).
    - Personalización de tamaño tipográfico en 4 niveles (Pequeño, Normal, Grande, Extra Grande).
    - Botón de acceso rápido `CC` en la barra de controles con etiqueta visual `CC On` cuando se encuentran activos.
- **Persistencia Avanzada de Configuraciones por Video (Room Database SQLite v2):**
  - Guardado y restauración individualizada para cada archivo de video:
    - **Velocidad de reproducción** (0.25x a 2.0x).
    - **Relación de aspecto de pantalla** (Ajustar, Zoom completo, Llenar).
    - **Motor de audio preferido** (Google Oboe C++ o Media3 AudioTrack).
    - **Enrutamiento de canales de audio** (Estéreo, Mono centrado, Pseudo-Estéreo Haas 3D).
    - **Subtítulo externo y tamaño tipográfico** asignados al archivo.
    - **Calibración completa de Ecualizador y Shaders OpenGL ES:** Brillo, contraste, saturación, gamma, nitidez, filtro de luz azul, Pillarbox Blur (desenfoque para videos verticales), AMD FidelityFX FSR 1.0 (activación e intensidad RCAS), Modo Sol Extremo y perfiles Anime4K.
  - Permite retomar cualquier video conservando exactamente los mismos retoques visuales y acústicos que se le configuraron previamente sin alterar los demás videos.
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
  - Gestión del historial: renombrado en base de datos, eliminación de videos individuales o vaciado total mediante confirmación.
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
  - **Compresor Dinámico (DRC) y Modo Voces Claras (Audio DSP Inteligente Universal para Oboe y Media3):**
    - **Filtro Peaking Vocal:** Ganancia selectiva sobre la banda formativa humana (1.5 kHz a 3.5 kHz) para maximizar la inteligibilidad de diálogos y susurros en películas y series.
    - **Compresor Dinámico Nocturno (Dynamic Range Compressor):** Atenuación automática con tiempo de respuesta de microsegundos sobre picos estridentes (explosiones, disparos) mientras eleva pasajes de bajo volumen para disfrutar del cine sin sobresaltos.
    - **Soporte Universal en Ambos Motores:** Procesamiento nativo en C++ con aceleración por hardware (NEON) al operar con Google Oboe, y procesamiento integrado de flujo PCM de alta velocidad en `OboeAudioProcessor` al operar con Media3, garantizando 0 ms de latencia perceptiva y sincronía A/V perfecta en ambos modos.
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
  - **Reconstrucción y Realce Anime4K (bloc97 en GPU):**
    - Algoritmo de restauración y perfilado de trazos especializado para series de animación, anime y dibujos animados en OpenGL ES:
      - **Modo Lite (Bilateral Rápido):** Reconstrucción bilateral de alta velocidad orientada a dispositivos de entrada o ahorro de batería.
      - **Modo Pro (Line Darken & Line Thinning):** Realce direccional de contornos, oscurecimiento de trazos y adelgazamiento de líneas borrosas producidas por escalado bilineal.
      - **Modo Restauración (Denoise & Despeckle):** Suavizado adaptativo de planos preservando aristas para limpiar artefactos de compresión y grano en fondos lisos.
    - Regulador de intensidad de realce (10% a 100%) y preset directo "Anime 4K (Pro)".
  - **Presets de Imagen Instantáneos:** Normal, Anime 4K (Pro), Super-Resolución FSR, Descanso Visual, Vívido, Cine, Nocturno, Alto Contraste y Blanco y Negro.
  - Panel inferior moderno e interactivo (`VideoEqualizerSheet`) con botón de restablecimiento rápido.
- **Control de Velocidad de Reproducción (Hasta 2.0x) con Corrección de Tono (Sonic):**
  - Selector de velocidad desde 0.25x hasta un máximo de **2.0x** tanto por presets rápidos como por ajuste fino continuo.
  - Algoritmo de estiramiento temporal *Sonic Pitch Preservation* activo: conserva intacta la tonalidad original de las voces y de los instrumentos musicales, evitando por completo distorsiones acústicas o el efecto "voz de ardilla".
  - Botón dedicado en la barra de controles con etiqueta de velocidad en vivo (`PlaybackSpeedSheet`).
- **Arquitectura Modular de Herramientas del Reproductor (Pantallas Independientes):**
  - Panel lateral interactivo (`PlayerToolsSideSheet`) accesible con un toque desde las barras superior e inferior.
  - Cada herramienta cuenta con su propia pantalla o panel modal independiente y exclusivo para una experiencia enfocada y limpia:
    - **Ecualizador de Video (`VideoEqualizerSheet`):** Calibración de color (brillo, contraste, saturación, gamma), nitidez por convolución y presets de imagen.
    - **Reconstrucción Anime4K (`Anime4KSheet`):** Pantalla dedicada para realce de animación con modos Lite, Pro y Restauración más ajuste de fuerza.
    - **Modo Sol Extremo (`SunModeSheet`):** Pantalla independiente de compensación para exteriores bajo luz solar directa y perfiles de alto contraste para accesibilidad.
    - **Relleno Desenfoque Vertical (`PillarboxBlurSheet`):** Pantalla exclusiva para configurar el desenfoque Gaussiano y atenuación de fondo cuando un video vertical se reproduce con bandas negras.
    - **Super-Resolución AMD FSR 1.0 (`FsrUpscaleSheet`):** Pantalla dedicada para activar el escalado espacial adaptativo (EASU) y regular la nitidez dependiente del contraste (RCAS).
    - **Compresor Dinámico y Voces Claras (`VoiceNightAudioSheet`):** Pantalla dedicada para realce de diálogos y compresión de rango dinámico para cine nocturno en DSP C++. Cuenta con candado visual y bloqueo preventivo si se utiliza Media3, ofreciendo un botón de desbloqueo instantáneo hacia Google Oboe C++.
    - **Relación de Aspecto (`AspectRatioSheet`):** Selector independiente de proporción geométrica (*Ajustar*, *Zoom*, *Llenar*).
    - **Zoom Táctil Continuo (`ZoomBottomSheet`):** Panel exclusivo para regular la ampliación continua de 1.0x a 10.0x con deslizador de precisión y presets rápidos (1.0x, 1.5x, 2.0x, 3.0x, 5.0x y 10.0x).
    - **Motor de Audio (`AudioEngineSheet`):** Selector independiente entre Google Oboe nativo en C++ y Android Media3.
    - **Velocidad de Reproducción (`PlaybackSpeedSheet`):** Panel dedicado de velocidad con *Sonic Pitch Preservation*.
    - **Gestor de Subtítulos (`SubtitlesBottomSheet`):** Configuración de subtítulos internos y externos con ajuste de escala tipográfica.
    - **Bloqueo de Controles (`Lock`):** Modo para inmovilizar gestos y toques accidentales con botón flotante animado de desbloqueo instantáneo.
- **Aislamiento de Escala de Fuente del Sistema Operativo (`fontScale = 1.0f`):**
  - La aplicación define su propia escala tipográfica óptima e independiente de la configuración global de tamaño de texto de Android.
  - Previene distorsiones visuales, desbordamientos de paneles y solapamientos en menús o subtítulos cuando el usuario tiene configurada una fuente gigante o reducida en su teléfono.
- **Audio DSP Inteligente Universal en Tiempo Real (Google Oboe C++ y Android Media3):**
  - Procesamiento acústico en tiempo real disponible tanto bajo Google Oboe C++ (vía SIMD NEON) como en Android Media3 (vía pipeline PCM optimizado en `OboeAudioProcessor`).
  - **Voces Claras (Voice Clarity):** Filtro Peaking en la banda formativa humana (1.5 kHz a 3.5 kHz) para maximizar la inteligibilidad de diálogos y susurros.
  - **Compresor Dinámico Nocturno (DRC):** Atenúa picos estridentes (explosiones, disparos) y realza pasajes de bajo volumen para disfrutar del cine sin sobresaltos nocturnos.
  - Disponibilidad universal sin restricciones ni candados: la subpantalla `VoiceNightAudioSheet` y el panel lateral operan de forma interactiva e inmediata en ambos motores de audio, manteniendo sincronía A/V perfecta de 0 ms.
- **Doble Motor de Audio Seleccionable con Persistencia:**
  - **Media3 (AudioTrack Estándar) [Predeterminado]:** Pipeline nativo estándar de Android con sincronización A/V automática, compensación de retardo para auriculares Bluetooth y compatibilidad universal con todos los dispositivos.
  - **Google Oboe (Nativo C++):** Motor de ultra baja latencia que interactúa directamente con **AAudio** en Android 8.0+ y realiza fallback automático a **OpenSL ES** en hardware heredado. Elimina microcortes y asegura procesamiento directo a nivel de muestra.
  - **Persistencia en Disco (`AppPreferences`):** La preferencia de motor de audio elegida se guarda automáticamente y se preserva de forma permanente entre reinicios de la aplicación.
- **Gestos Táctiles Avanzados y Control Rápido:**
  - **Pellizcar para Zoom Continuo (Pinch-to-Zoom hasta x10):**
    - **Ampliación Multitáctil Fluida (1.0x a 10.0x):** Gesto intuitivo con dos dedos para ampliar cualquier parte de la escena hasta diez veces su escala original de forma continua.
    - **Desplazamiento Panorámico (Pan):** Con el zoom activo (>1.0x), deslizar con uno o dos dedos permite recorrer toda la imagen con encuadre delimitado por los bordes reales.
    - **Restablecimiento Rápido con Doble Toque:** Si la imagen está ampliada, pulsar dos veces con un dedo restablece de inmediato la escala a 1.0x con respuesta háptica.
    - **Indicador HUD Reactivo (`ZoomHudIndicator`):** Píldora translúcida superior con animación elástica que indica el factor exacto de aumento y ofrece un botón de reinicio directo.
  - **Salto Rápido por Doble Toque (Doble Click a los Laterales):**
    - **Doble toque en el lado izquierdo:** Atrasa el video **5 segundos** (-5s).
    - **Doble toque en el lado derecho:** Adelanta el video **5 segundos** (+5s).
    - Incluye respuesta háptica precisa y un indicador visual flotante (HUD circular) con icono y etiqueta (+5 seg / -5 seg) que aparece instantáneamente y se desvanece de forma suave.
  - **Gesto de Avance Rápido a 2X (Pulsación Prolongada en Lateral Derecho):**
    - Mantener presionado el lado derecho de la pantalla activa instantáneamente la reproducción rápida a **2X** con respuesta háptica y un badge flotante HUD estilizado.
    - Al levantar el dedo, la reproducción vuelve de forma inmediata y suave a la velocidad configurada previamente.
  - **Gestos Táctiles con HUD Minimalista:**
    - **Lado Izquierdo (Deslizar vertical):** Control dinámico y directo del brillo de pantalla (1% a 100%).
    - **Lado Derecho (Deslizar vertical):** Control en tiempo real del volumen multimedia físico del dispositivo.
- **Biblioteca Interactiva de Videos Importados y Vistos (Persistencia Local con Room):**
  - Registro automático y persistente en SQLite (`VideoEntity`, `VideoDao`) de cada video cargado desde la Galería o Gestor de Archivos.
  - Extracción y muestra inmediata de metadatos: **título completo del archivo**, **tamaño** y **duración formateada** (ej. `04:32` o `01:20:15`).
  - Barra de progreso visual interactiva indicando el porcentaje visto y la marca de tiempo de pausa (ej. *En pausa en 02:15* o *Visto completo*).
  - Reproducción o reanudación instantánea con un solo toque directamente desde la posición guardada.
  - **Confirmación de Seguridad al Eliminar:** Diálogo modal que solicita confirmación antes de eliminar cualquier video individual de la biblioteca, indicando claramente que los archivos originales permanecen a salvo en el almacenamiento del teléfono. Opción adicional de vaciado completo mediante confirmación.
  - Barra de progreso con *scrubbing* en tiempo real.
  - Salto temporal rápido (-10s / +10s).
  - Modos de relación de aspecto instantáneos: *Ajustar (Fit)*, *Zoom (Rellenar)* y *Estirar (Fill)*.
  - Silenciado rápido y control dinámico de volumen.
  - Ocultamiento inteligente de controles tras inactividad táctil.
  - Persistencia de pantalla encendida (*Keep Screen On*) durante la reproducción y restauración automática del brillo al salir.
- **Motor de Audio Nativo Oboe Optimizado y Soporte FFmpeg Puro:**
  - **Búfer Circular Estático en C++:** Sustitución de asignaciones dinámicas por un búfer de anillo estático con punteros atómicos para erradicar microcortes y latencias al buscar (seek) o reanudar.
  - **Vaciado Instantáneo (`flush`):** Limpieza inmediata de colas de audio nativas sin detener ni recrear el flujo de hardware de AAudio / OpenSL ES.
  - **Decodificador FFmpeg Nativo Puro Integrado:** Soporte extendido para pistas de audio complejas de alta fidelidad (AC-3, E-AC-3, DTS, DTS-HD, TrueHD, FLAC, ALAC, Opus y Vorbis) sin wrappers obsoletos.
  - **Compensación de Ganancia Perceptual (1.40x):** Nivel de volumen equiparado al estándar de AudioTrack para una audición potente y sin pérdidas.
- **Pantalla Independiente de Configuración y Telemetría:**
  - Pantalla dedicada y desacoplada de diálogos o tarjetas flotantes emergentes.
  - Alternancia 100% real en caliente entre el motor C++ (Google Oboe con AAudio/OpenSL ES) y Android Media3 (AudioTrack).
  - **Prueba de Sonido Real:** Generador de tono senoidal estéreo PCM de 440 Hz integrado para audición física instantánea en el motor seleccionado.
  - **Telemetría Nativa en Tiempo Real:** Diagnóstico en vivo de arquitectura CPU (32/64 bits), backend nativo C++ activo, frecuencia de muestreo (Hz), canales de audio y tramas escritas en buffer.
- **Detección Dinámica de Vulkan 1.1+ y Enlace Nativo C++ (Fase 6):**
  - Módulo de verificación de hardware (`VulkanCapabilities`) que inspecciona `FEATURE_VULKAN_HARDWARE_VERSION` (>= 1.1 `0x401000`) y `FEATURE_VULKAN_HARDWARE_LEVEL` a nivel de sistema operativo.
  - Consulta nativa directa al driver de la GPU en C++ vía JNI (`nativeQueryVulkanDriver`) mediante `vkEnumerateInstanceVersion` y `vkGetPhysicalDeviceProperties`.
  - Enlace de la biblioteca nativa `libvulkan.so` en `CMakeLists.txt` con compatibilidad cruzada estricta para 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).
  - Tarjeta dedicada en `TelemetrySubScreen` que muestra en tiempo real la versión de API Vulkan, nivel de hardware, modelo de GPU física detectada y versión del controlador.

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
| **Interfaz de Usuario** | Kotlin + Jetpack Compose (Material 3 + Material You) | UI moderna y adaptativa con soporte de Material You (colores dinámicos en Android 12+) y selector de tema Oscuro, Claro y del Sistema. |
| **Canal de Video** | AndroidX Media3 (ExoPlayer 1.5.1) | Decodificación por hardware de codecs universales (H.264, HEVC, AV1, VP9). |
| **Motor de Audio Nativo** | C++17 + Google Oboe 1.9.3 | Procesamiento de audio de ultra baja latencia con AAudio y OpenSL ES. |
| **Motor Gráfico Principal** | C++17 + OpenGL ES 2.0 / 3.0 (GLSL) | Pipeline de shaders en GPU para ecualizador de video en tiempo real (Zero-Copy OES). |
| **Infraestructura Gráfica Avanzada** | C++17 + Vulkan 1.1+ (libvulkan.so NDK) | Detección de hardware y enlace nativo para renderizado adaptativo de baja sobrecarga de CPU (Fase 6). |
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
