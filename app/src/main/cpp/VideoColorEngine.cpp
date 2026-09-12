/**
 * VideoColorEngine.cpp - Implementación del Motor de Renderizado OpenGL ES
 *
 * Ejecuta el postprocesamiento de cada frame decodificado por hardware sin copia de memoria
 * (Zero-Copy) mediante la extensión GL_TEXTURE_EXTERNAL_OES.
 *
 * Los cálculos de brillo, contraste, saturación, gamma y nitidez se ejecutan en los
 * núcleos de sombreado de la GPU (Shaders GLSL), asegurando 60 FPS sin sobrecargar la CPU.
 */

#include "VideoColorEngine.h"
#include <android/log.h>
#include <vector>

#define LOG_TAG "NovaVideoColorEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Vértices del cuadrilátero (Quad) para la superficie de video
static const GLfloat sQuadVertices[] = {
    -1.0f, -1.0f, 0.0f, // Inferior Izquierda
     1.0f, -1.0f, 0.0f, // Inferior Derecha
    -1.0f,  1.0f, 0.0f, // Superior Izquierda
     1.0f,  1.0f, 0.0f  // Superior Derecha
};

// Coordenadas UV de textura estándar
static const GLfloat sQuadTexCoords[] = {
    0.0f, 0.0f, // Inferior Izquierda
    1.0f, 0.0f, // Inferior Derecha
    0.0f, 1.0f, // Superior Izquierda
    1.0f, 1.0f  // Superior Derecha
};

// Vertex Shader: Aplica transformaciones de proyección y la matriz de textura de SurfaceTexture
static const char* sVertexShaderSource = R"glsl(
    attribute vec4 aPosition;
    attribute vec4 aTextureCoord;
    uniform mat4 uMVPMatrix;
    uniform mat4 uSTMatrix;
    varying vec2 vTextureCoord;

    void main() {
        gl_Position = uMVPMatrix * aPosition;
        vTextureCoord = (uSTMatrix * aTextureCoord).xy;
    }
)glsl";

// Fragment Shader: Postprocesamiento dinámico en tiempo real (Color EQ + Sharpening)
static const char* sFragmentShaderSource =
"#extension GL_OES_EGL_image_external : require\n"
"precision mediump float;\n"
R"glsl(
    varying vec2 vTextureCoord;
    uniform samplerExternalOES sTexture;

    // Uniforms de ajuste en tiempo real
    uniform float uBrightness;  // Rango: [-0.5, 0.5] (0.0 = neutro)
    uniform float uContrast;    // Rango: [0.5, 2.0]  (1.0 = neutro)
    uniform float uSaturation;  // Rango: [0.0, 2.0]  (1.0 = neutro)
    uniform float uGamma;       // Rango: [0.5, 2.0]  (1.0 = neutro)
    uniform float uSharpness;   // Rango: [0.0, 1.5]  (0.0 = desactivado)
    uniform vec2 uTexelStep;    // Tamaño inverso del frame (1.0/ancho, 1.0/alto)

    void main() {
        vec4 color = texture2D(sTexture, vTextureCoord);

        // 1. Filtro de Nitidez (Kernel de convolución Laplaciano 3x3)
        if (uSharpness > 0.01) {
            vec4 north = texture2D(sTexture, vTextureCoord + vec2(0.0, uTexelStep.y));
            vec4 south = texture2D(sTexture, vTextureCoord - vec2(0.0, uTexelStep.y));
            vec4 east  = texture2D(sTexture, vTextureCoord + vec2(uTexelStep.x, 0.0));
            vec4 west  = texture2D(sTexture, vTextureCoord - vec2(uTexelStep.x, 0.0));
            vec4 neighbors = north + south + east + west;
            color = clamp(color * (1.0 + 4.0 * uSharpness) - neighbors * uSharpness, 0.0, 1.0);
        }

        // 2. Ajuste de Brillo
        color.rgb += uBrightness;

        // 3. Ajuste de Contraste con punto pivote en 0.5
        color.rgb = (color.rgb - 0.5) * uContrast + 0.5;

        // 4. Ajuste de Saturación de color (Luminancia Rec. 709)
        float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = mix(vec3(luma), color.rgb, uSaturation);

        // 5. Corrección Gamma para rango dinámico perceptual
        color.rgb = clamp(color.rgb, 0.0, 1.0);
        if (uGamma > 0.01 && abs(uGamma - 1.0) > 0.01) {
            color.rgb = pow(color.rgb, vec3(1.0 / max(uGamma, 0.01)));
        }

        // Asegurar opacidad total para evitar que frames con alfa nulo decodificados por hardware se vean negros
        gl_FragColor = vec4(color.rgb, 1.0);
    }
)glsl";

VideoColorEngine::VideoColorEngine()
    : mProgram(0),
      maPositionHandle(-1),
      maTextureCoordHandle(-1),
      muMVPMatrixHandle(-1),
      muSTMatrixHandle(-1),
      msTextureHandle(-1),
      muBrightnessHandle(-1),
      muContrastHandle(-1),
      muSaturationHandle(-1),
      muGammaHandle(-1),
      muSharpnessHandle(-1),
      muTexelStepHandle(-1) {
}

VideoColorEngine::~VideoColorEngine() {
    release();
}

GLuint VideoColorEngine::loadShader(GLenum shaderType, const char* source) {
    GLuint shader = glCreateShader(shaderType);
    if (!shader) {
        LOGE("No fue posible crear el shader de tipo %d", shaderType);
        return 0;
    }

    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> infoLog(infoLen);
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog.data());
            LOGE("Error compilando shader (%d): %s", shaderType, infoLog.data());
        }
        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

GLuint VideoColorEngine::createProgram(const char* vertexSource, const char* fragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!fragmentShader) {
        glDeleteShader(vertexShader);
        return 0;
    }

    GLuint program = glCreateProgram();
    if (!program) {
        LOGE("No fue posible crear el programa OpenGL ES");
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return 0;
    }

    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    GLint linkStatus = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
    if (!linkStatus) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> infoLog(infoLen);
            glGetProgramInfoLog(program, infoLen, nullptr, infoLog.data());
            LOGE("Error enlazando programa OpenGL ES: %s", infoLog.data());
        }
        glDeleteProgram(program);
        program = 0;
    }

    // Los shaders individuales ya están enlazados en el binario del programa
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return program;
}

bool VideoColorEngine::init() {
    std::lock_guard<std::mutex> lock(mEngineMutex);
    if (mProgram != 0) {
        return true;
    }

    mProgram = createProgram(sVertexShaderSource, sFragmentShaderSource);
    if (mProgram == 0) {
        LOGE("Fallo en la inicialización del programa OpenGL ES");
        return false;
    }

    // Obtener ubicaciones de atributos y uniforms
    maPositionHandle     = glGetAttribLocation(mProgram, "aPosition");
    maTextureCoordHandle = glGetAttribLocation(mProgram, "aTextureCoord");
    muMVPMatrixHandle    = glGetUniformLocation(mProgram, "uMVPMatrix");
    muSTMatrixHandle     = glGetUniformLocation(mProgram, "uSTMatrix");
    msTextureHandle      = glGetUniformLocation(mProgram, "sTexture");

    muBrightnessHandle   = glGetUniformLocation(mProgram, "uBrightness");
    muContrastHandle     = glGetUniformLocation(mProgram, "uContrast");
    muSaturationHandle   = glGetUniformLocation(mProgram, "uSaturation");
    muGammaHandle        = glGetUniformLocation(mProgram, "uGamma");
    muSharpnessHandle    = glGetUniformLocation(mProgram, "uSharpness");
    muTexelStepHandle    = glGetUniformLocation(mProgram, "uTexelStep");

    LOGI("VideoColorEngine inicializado exitosamente en OpenGL ES.");
    return true;
}

bool VideoColorEngine::render(
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
) {
    std::lock_guard<std::mutex> lock(mEngineMutex);
    if (mProgram == 0) {
        return false;
    }

    glUseProgram(mProgram);

    // Configurar matrices de proyección y de textura
    if (muMVPMatrixHandle >= 0 && mvpMatrix != nullptr) {
        glUniformMatrix4fv(muMVPMatrixHandle, 1, GL_FALSE, mvpMatrix);
    }
    if (muSTMatrixHandle >= 0 && stMatrix != nullptr) {
        glUniformMatrix4fv(muSTMatrixHandle, 1, GL_FALSE, stMatrix);
    }

    // Configurar parámetros de ecualización de video
    if (muBrightnessHandle >= 0) glUniform1f(muBrightnessHandle, brightness);
    if (muContrastHandle >= 0)   glUniform1f(muContrastHandle, contrast);
    if (muSaturationHandle >= 0) glUniform1f(muSaturationHandle, saturation);
    if (muGammaHandle >= 0)      glUniform1f(muGammaHandle, gamma);
    if (muSharpnessHandle >= 0)  glUniform1f(muSharpnessHandle, sharpness);

    // Configurar texel step para filtro de nitidez
    if (muTexelStepHandle >= 0) {
        float stepX = (texWidth > 0.0f) ? (1.0f / texWidth) : 0.0f;
        float stepY = (texHeight > 0.0f) ? (1.0f / texHeight) : 0.0f;
        glUniform2f(muTexelStepHandle, stepX, stepY);
    }

    // Activar textura externa OES
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
    if (msTextureHandle >= 0) {
        glUniform1i(msTextureHandle, 0);
    }

    // Cargar coordenadas de vértices y textura
    if (maPositionHandle >= 0) {
        glEnableVertexAttribArray(maPositionHandle);
        glVertexAttribPointer(maPositionHandle, 3, GL_FLOAT, GL_FALSE, 0, sQuadVertices);
    }
    if (maTextureCoordHandle >= 0) {
        glEnableVertexAttribArray(maTextureCoordHandle);
        glVertexAttribPointer(maTextureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, sQuadTexCoords);
    }

    // Dibujar el cuadrilátero con el frame procesado
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    if (maPositionHandle >= 0) {
        glDisableVertexAttribArray(maPositionHandle);
    }
    if (maTextureCoordHandle >= 0) {
        glDisableVertexAttribArray(maTextureCoordHandle);
    }

    return true;
}

void VideoColorEngine::release() {
    std::lock_guard<std::mutex> lock(mEngineMutex);
    if (mProgram != 0) {
        glDeleteProgram(mProgram);
        mProgram = 0;
        LOGI("VideoColorEngine liberado.");
    }
}
