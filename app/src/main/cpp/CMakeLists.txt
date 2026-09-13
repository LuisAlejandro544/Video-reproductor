# ==============================================================================
# CMakeLists.txt - Módulo Nativo C++ y Oboe para Nova Video Player
#
# Propósito:
# Configura la compilación de bibliotecas nativas C++ compatibles con arquitecturas
# de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
#
# Integra Google Oboe mediante el sistema Prefab de Android Gradle Plugin.
# Oboe cuenta con licencia permisiva Apache 2.0 (adecuada para distribución sin
# obligaciones de copyleft ni código abierto forzoso).
# ==============================================================================

cmake_minimum_required(VERSION 3.22.1)

project("novaplayer_native")

# Estándar C++17 moderno para alto rendimiento y optimizaciones de bajo nivel
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Localizar el paquete Oboe provisto por la dependencia AAR a través de Prefab
find_package(oboe REQUIRED CONFIG)

# Definir la biblioteca compartida nativa principal
add_library(
    novaplayer_native
    SHARED
    native-lib.cpp
    OboeAudioEngine.cpp
    VideoColorEngine.cpp
    vulkan/VulkanVideoEngine.cpp
)


# Enlazar las dependencias necesarias:
# - oboe::oboe : Motor de audio de ultra baja latencia de Google
# - android    : API nativa de Android (buffers, timers, eventos)
# - log        : Sistema nativo de logs (android/log.h)
# - GLESv2     : Biblioteca de sombreadores y GPU OpenGL ES 2.0 / 3.0
# - EGL        : Interfaz nativa de contexto gráfico de Android
# - vulkan     : API gráfica de bajo nivel Vulkan 1.1+ (Fase 6 del Roadmap)
target_link_libraries(
    novaplayer_native
    PRIVATE
    oboe::oboe
    android
    log
    GLESv2
    EGL
    vulkan
)
