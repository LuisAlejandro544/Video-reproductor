/**
 * VideoColorEngine.h - Motor de Renderizado y Postprocesamiento de Video en OpenGL ES
 *
 * Arquitectura Nativa en C++ para Nova Video Player:
 * Administra el pipeline gráfico de bajo nivel mediante OpenGL ES 2.0 / 3.0.
 *
 * Capacidades:
 * - Compilación y enlace de Vertex y Fragment Shaders en GPU.
 * - Texturizado Zero-Copy a partir de flujos de video con GL_TEXTURE_EXTERNAL_OES.
 * - Procesamiento en tiempo real de:
 *     1. Brillo (Brightness)
 *     2. Contraste (Contrast)
 *     3. Saturación de color (Saturation con luminancia Rec.709)
 *     4. Corrección Gamma (Gamma correction)
 *     5. Filtro de Nitidez / Realce de bordes (Kernel de convolución 3x3)
 *
 * Compatible con arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#ifndef NOVA_VIDEO_COLOR_ENGINE_H
#define NOVA_VIDEO_COLOR_ENGINE_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <mutex>
#include <string>

class VideoColorEngine {
public:
    VideoColorEngine();
    ~VideoColorEngine();

    // Inicializa shaders y compila el programa de postprocesado OpenGL ES
    bool init();

    // Renderiza el frame actual aplicando los parámetros de ecualización en tiempo real
    bool render(
        GLuint textureId,
        const float* stMatrix,
        const float* mvpMatrix,
        float brightness,
        float contrast,
        float saturation,
        float gamma,
        float sharpness,
        float texWidth,
        float texHeight
    );

    // Libera shaders y programa en GPU
    void release();

    // Estado del motor
    bool isInitialized() const { return mProgram != 0; }

private:
    GLuint loadShader(GLenum shaderType, const char* source);
    GLuint createProgram(const char* vertexSource, const char* fragmentSource);

    std::mutex mEngineMutex;
    GLuint mProgram;

    // Handles de atributos y matrices
    GLint maPositionHandle;
    GLint maTextureCoordHandle;
    GLint muMVPMatrixHandle;
    GLint muSTMatrixHandle;
    GLint msTextureHandle;

    // Handles de parámetros de ecualización en tiempo real
    GLint muBrightnessHandle;
    GLint muContrastHandle;
    GLint muSaturationHandle;
    GLint muGammaHandle;
    GLint muSharpnessHandle;
    GLint muTexelStepHandle;
};

#endif // NOVA_VIDEO_COLOR_ENGINE_H
