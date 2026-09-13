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
#include "shaders/VideoShaders.h"
#include <android/log.h>
#include <vector>

#define LOG_TAG "NovaVideoColorEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
      muTexelStepHandle(-1),
      muBlueLightFilterHandle(-1),
      muBlurRadiusHandle(-1),
      muBackgroundDimHandle(-1),
      muFsrEnabledHandle(-1),
      muFsrSharpnessHandle(-1),
      muSunModeHandle(-1),
      muAnime4kModeHandle(-1),
      muAnime4kStrengthHandle(-1) {
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
    // Validar si el programa actual sigue existiendo en el contexto EGL activo
    if (mProgram != 0 && glIsProgram(mProgram)) {
        return true;
    }

    // Si el programa no es válido en el contexto actual (o es 0), resetear handles
    mProgram = 0;
    maPositionHandle     = -1;
    maTextureCoordHandle = -1;
    muMVPMatrixHandle    = -1;
    muSTMatrixHandle     = -1;
    msTextureHandle      = -1;
    muBrightnessHandle   = -1;
    muContrastHandle     = -1;
    muSaturationHandle   = -1;
    muGammaHandle        = -1;
    muSharpnessHandle    = -1;
    muTexelStepHandle    = -1;
    muBlueLightFilterHandle = -1;
    muBlurRadiusHandle   = -1;
    muBackgroundDimHandle= -1;
    muFsrEnabledHandle   = -1;
    muFsrSharpnessHandle = -1;
    muSunModeHandle      = -1;
    muAnime4kModeHandle  = -1;
    muAnime4kStrengthHandle = -1;

    mProgram = createProgram(VideoShaders::VertexShaderSource, VideoShaders::FragmentShaderSource);
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
    muBlueLightFilterHandle = glGetUniformLocation(mProgram, "uBlueLightFilter");
    muBlurRadiusHandle   = glGetUniformLocation(mProgram, "uBlurRadius");
    muBackgroundDimHandle= glGetUniformLocation(mProgram, "uBackgroundDim");
    muFsrEnabledHandle   = glGetUniformLocation(mProgram, "uFsrEnabled");
    muFsrSharpnessHandle = glGetUniformLocation(mProgram, "uFsrSharpness");
    muSunModeHandle      = glGetUniformLocation(mProgram, "uSunMode");
    muAnime4kModeHandle  = glGetUniformLocation(mProgram, "uAnime4kMode");
    muAnime4kStrengthHandle = glGetUniformLocation(mProgram, "uAnime4kStrength");

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
    float texHeight,
    float blueLightFilter,
    float blurRadius,
    float backgroundDim,
    float fsrEnabled,
    float fsrSharpness,
    float sunMode,
    float anime4kMode,
    float anime4kStrength
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
    if (muBlueLightFilterHandle >= 0) glUniform1f(muBlueLightFilterHandle, blueLightFilter);
    if (muBlurRadiusHandle >= 0) glUniform1f(muBlurRadiusHandle, blurRadius);
    if (muBackgroundDimHandle >= 0) glUniform1f(muBackgroundDimHandle, backgroundDim);
    if (muFsrEnabledHandle >= 0) glUniform1f(muFsrEnabledHandle, fsrEnabled);
    if (muFsrSharpnessHandle >= 0) glUniform1f(muFsrSharpnessHandle, fsrSharpness);
    if (muSunModeHandle >= 0)    glUniform1f(muSunModeHandle, sunMode);
    if (muAnime4kModeHandle >= 0) glUniform1f(muAnime4kModeHandle, anime4kMode);
    if (muAnime4kStrengthHandle >= 0) glUniform1f(muAnime4kStrengthHandle, anime4kStrength);

    // Configurar texel step para filtro de nitidez y desenfoque
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
        glVertexAttribPointer(maPositionHandle, 3, GL_FLOAT, GL_FALSE, 0, VideoShaders::QuadVertices);
    }
    if (maTextureCoordHandle >= 0) {
        glEnableVertexAttribArray(maTextureCoordHandle);
        glVertexAttribPointer(maTextureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, VideoShaders::QuadTexCoords);
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
        if (glIsProgram(mProgram)) {
            glDeleteProgram(mProgram);
        }
        mProgram = 0;
        maPositionHandle     = -1;
        maTextureCoordHandle = -1;
        muMVPMatrixHandle    = -1;
        muSTMatrixHandle     = -1;
        msTextureHandle      = -1;
        LOGI("VideoColorEngine liberado correctamente.");
    }
}
