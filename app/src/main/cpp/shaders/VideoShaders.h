// ==============================================================================
// VideoShaders.h - Definiciones de Sombreadores GLSL para VideoColorEngine
//
// Propósito:
// Centraliza los sombreadores Vertex y Fragment (OpenGL ES 2.0 / 3.0) utilizados
// por la GPU para el post-procesado y ecualización de video en tiempo real.
//
// Características de post-procesamiento implementadas en GLSL:
// 1. Desenfocado Gaussiano en Pillarbox (relleno dinámico para relaciones de aspecto)
// 2. Modo Anime4K (bloc97): Realce adaptativo de líneas de animación y reducción de halos
// 3. AMD FidelityFX Super Resolution (FSR 1.0): Reconstrucción espacial y nitidez RCAS
// 4. Filtro Laplaciano de Nitidez Unsharp Mask
// 5. Ajustes cromáticos: Brillo, Contraste, Saturación perceptual (Rec. 709)
// 6. Corrección Gamma para curva de rango dinámico
// 7. Filtro de Luz Azul (Eye Comfort)
// 8. Modo Sol Extremo (elevación dinámica de sombras y contraste exterior)
// ==============================================================================

#pragma once
#include <GLES2/gl2.h>

namespace VideoShaders {

// Vértices del cuadrilátero (Quad) para la superficie de video
inline const GLfloat QuadVertices[] = {
    -1.0f, -1.0f, 0.0f, // Inferior Izquierda
     1.0f, -1.0f, 0.0f, // Inferior Derecha
    -1.0f,  1.0f, 0.0f, // Superior Izquierda
     1.0f,  1.0f, 0.0f  // Superior Derecha
};

// Coordenadas UV de textura estándar
inline const GLfloat QuadTexCoords[] = {
    0.0f, 0.0f, // Inferior Izquierda
    1.0f, 0.0f, // Inferior Derecha
    0.0f, 1.0f, // Superior Izquierda
    1.0f, 1.0f  // Superior Derecha
};

// Vertex Shader: Aplica transformaciones de proyección y la matriz de textura de SurfaceTexture
inline const char* const VertexShaderSource = R"glsl(
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

// Fragment Shader con ecualizador de video, desenfoque de fondo y Super Resolución FSR en tiempo real
inline const char* const FragmentShaderSource =
"#extension GL_OES_EGL_image_external : require\n"
"precision mediump float;\n"
R"glsl(
    varying vec2 vTextureCoord;
    uniform samplerExternalOES sTexture;

    // Uniforms de ajuste en tiempo real
    uniform float uBrightness;       // Rango: [-0.5, 0.5] (0.0 = neutro)
    uniform float uContrast;         // Rango: [0.5, 2.0]  (1.0 = neutro)
    uniform float uSaturation;       // Rango: [0.0, 2.0]  (1.0 = neutro)
    uniform float uGamma;            // Rango: [0.5, 2.0]  (1.0 = neutro)
    uniform float uSharpness;        // Rango: [0.0, 1.5]  (0.0 = desactivado)
    uniform vec2 uTexelStep;         // Tamaño inverso del frame (1.0/ancho, 1.0/alto)
    uniform float uBlueLightFilter;  // Rango: [0.0, 1.0]  (0.0 = desactivado, 1.0 = descanso visual máximo)
    uniform float uBlurRadius;       // Rango: [0.0, 20.0] (radio de desenfoque para pillarbox)
    uniform float uBackgroundDim;    // Rango: [0.0, 1.0]  (atenuación de luminosidad para fondo)
    uniform float uFsrEnabled;       // 0.0 = desactivado, 1.0 = AMD FSR 1.0 activado
    uniform float uFsrSharpness;     // Rango: [0.0, 1.0]  (Afilado adaptativo al contraste RCAS)
    uniform float uSunMode;          // Rango: [0.0, 1.0]  (0.0 = desactivado, 1.0 = Modo Sol Extremo / Alto Contraste)
    uniform float uAnime4kMode;      // 0.0 = off, 1.0 = Lite, 2.0 = Pro, 3.0 = Restore
    uniform float uAnime4kStrength;  // Rango: [0.0, 1.0]  (Intensidad del realce Anime4K)

    void main() {
        vec4 color = texture2D(sTexture, vTextureCoord);

        // 1. Desenfoque de fondo Gaussiano de 9 toques (Pillarbox Blur)
        if (uBlurRadius > 0.5) {
            vec2 step = uTexelStep * uBlurRadius;
            vec4 blurSum = color * 0.227027;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(-step.x, -step.y)) * 0.070270;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(0.0,     -step.y)) * 0.121621;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(step.x,  -step.y)) * 0.070270;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(-step.x,  0.0))    * 0.121621;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(step.x,   0.0))    * 0.121621;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(-step.x,  step.y)) * 0.070270;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(0.0,      step.y)) * 0.121621;
            blurSum += texture2D(sTexture, vTextureCoord + vec2(step.x,   step.y)) * 0.070270;
            color = blurSum;

            if (uBackgroundDim > 0.01) {
                color.rgb *= (1.0 - uBackgroundDim);
            }
        }
        // 2. Anime4K: Reescalado y Reconstrucción de Líneas en GPU para Animación (Algoritmo bloc97)
        else if (uAnime4kMode > 0.5) {
            vec2 st = uTexelStep;
            vec4 c  = color;

            vec4 n  = texture2D(sTexture, vTextureCoord + vec2(0.0,  -st.y));
            vec4 s  = texture2D(sTexture, vTextureCoord + vec2(0.0,   st.y));
            vec4 e  = texture2D(sTexture, vTextureCoord + vec2( st.x, 0.0));
            vec4 w  = texture2D(sTexture, vTextureCoord + vec2(-st.x, 0.0));
            vec4 nw = texture2D(sTexture, vTextureCoord + vec2(-st.x, -st.y));
            vec4 ne = texture2D(sTexture, vTextureCoord + vec2( st.x, -st.y));
            vec4 sw = texture2D(sTexture, vTextureCoord + vec2(-st.x,  st.y));
            vec4 se = texture2D(sTexture, vTextureCoord + vec2( st.x,  st.y));

            // Luminancia Rec. 709 para estimación precisa de bordes en animación cel
            float lumaC  = dot(c.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaN  = dot(n.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaS  = dot(s.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaE  = dot(e.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaW  = dot(w.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaNW = dot(nw.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaNE = dot(ne.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaSW = dot(sw.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaSE = dot(se.rgb, vec3(0.2126, 0.7152, 0.0722));

            // Gradiente Sobel direccional
            float gx = (lumaNE + 2.0 * lumaE + lumaSE) - (lumaNW + 2.0 * lumaW + lumaSW);
            float gy = (lumaSW + 2.0 * lumaS + lumaSE) - (lumaNW + 2.0 * lumaN + lumaNE);
            float edgeMag = length(vec2(gx, gy));
            float strength = clamp(uAnime4kStrength, 0.0, 1.0);

            // MODO 1: Anime4K Lite (Reconstrucción Adaptativa Bilateral Rápida)
            if (uAnime4kMode < 1.5) {
                float wN = exp(-distance(n.rgb, c.rgb) * 5.0);
                float wS = exp(-distance(s.rgb, c.rgb) * 5.0);
                float wE = exp(-distance(e.rgb, c.rgb) * 5.0);
                float wW = exp(-distance(w.rgb, c.rgb) * 5.0);
                float totalW = 1.0 + wN + wS + wE + wW;
                vec4 bilateralColor = (c + n * wN + s * wS + e * wE + w * wW) / totalW;

                vec4 minNeighbor = min(c, min(min(n, s), min(e, w)));
                vec4 edgeEnhanced = mix(bilateralColor, minNeighbor, clamp(edgeMag * 1.2 * strength, 0.0, 0.45));
                color = mix(c, edgeEnhanced, strength);
            }
            // MODO 2: Anime4K Pro (Line Darken + Line Thinning + Bilateral Edge Reconstruction)
            else if (uAnime4kMode < 2.5) {
                // Realce de trazo oscuro (Line Darken)
                float lineWeight = clamp((edgeMag - 0.035) * 3.5, 0.0, 1.0);
                vec4 minNeighbor = min(c, min(min(n, s), min(e, w)));
                vec4 darkened = mix(c, minNeighbor, lineWeight * strength * 0.70);

                // Adelgazamiento de líneas borrosas (Line Thin)
                vec4 thinned = darkened;
                if (edgeMag > 0.04) {
                    vec2 gradDir = normalize(vec2(gx, gy));
                    vec4 posSample = texture2D(sTexture, vTextureCoord + gradDir * st);
                    vec4 negSample = texture2D(sTexture, vTextureCoord - gradDir * st);
                    vec4 maxSample = max(posSample, negSample);
                    thinned = mix(darkened, maxSample, (1.0 - lineWeight) * 0.40 * strength);
                }

                vec4 minRing = min(c, min(min(n, s), min(e, w)));
                vec4 maxRing = max(c, max(max(n, s), max(e, w)));
                color = clamp(thinned, minRing, maxRing);
            }
            // MODO 3: Anime4K Restauración / Denoise (Limpieza de artefactos en planos y preservación de líneas)
            else {
                float flatWeight = clamp(1.0 - (edgeMag * 4.0), 0.0, 1.0);
                float wN  = exp(-distance(n.rgb,  c.rgb) * 3.5);
                float wS  = exp(-distance(s.rgb,  c.rgb) * 3.5);
                float wE  = exp(-distance(e.rgb,  c.rgb) * 3.5);
                float wW  = exp(-distance(w.rgb,  c.rgb) * 3.5);
                float wNW = exp(-distance(nw.rgb, c.rgb) * 3.5) * 0.707;
                float wNE = exp(-distance(ne.rgb, c.rgb) * 3.5) * 0.707;
                float wSW = exp(-distance(sw.rgb, c.rgb) * 3.5) * 0.707;
                float wSE = exp(-distance(se.rgb, c.rgb) * 3.5) * 0.707;

                float totalW = 1.0 + wN + wS + wE + wW + wNW + wNE + wSW + wSE;
                vec4 denoised = (c + n*wN + s*wS + e*wE + w*wW + nw*wNW + ne*wNE + sw*wSW + se*wSE) / totalW;

                vec4 minNeighbor = min(c, min(min(n, s), min(e, w)));
                vec4 preserved = mix(denoised, minNeighbor, clamp(edgeMag * 1.5, 0.0, 0.5));
                color = mix(c, preserved, flatWeight * strength * 0.85);
            }
        }
        // 3. AMD FidelityFX Super Resolution 1.0 (EASU + RCAS)
        else if (uFsrEnabled > 0.5) {
            vec2 st = uTexelStep;
            vec4 c  = color;
            vec4 n  = texture2D(sTexture, vTextureCoord + vec2(0.0,  -st.y));
            vec4 s  = texture2D(sTexture, vTextureCoord + vec2(0.0,   st.y));
            vec4 e  = texture2D(sTexture, vTextureCoord + vec2( st.x, 0.0));
            vec4 w  = texture2D(sTexture, vTextureCoord + vec2(-st.x, 0.0));

            vec4 nw = texture2D(sTexture, vTextureCoord + vec2(-st.x, -st.y));
            vec4 ne = texture2D(sTexture, vTextureCoord + vec2( st.x, -st.y));
            vec4 sw = texture2D(sTexture, vTextureCoord + vec2(-st.x,  st.y));
            vec4 se = texture2D(sTexture, vTextureCoord + vec2( st.x,  st.y));

            // FSR Fase 1: EASU (Edge-Adaptive Spatial Upsampling)
            // Análisis de gradiente direccional con luminancia Rec. 709
            float lumaC  = dot(c.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaN  = dot(n.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaS  = dot(s.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaE  = dot(e.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaW  = dot(w.rgb,  vec3(0.2126, 0.7152, 0.0722));
            float lumaNW = dot(nw.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaNE = dot(ne.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaSW = dot(sw.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaSE = dot(se.rgb, vec3(0.2126, 0.7152, 0.0722));

            // Operador direccional de borde
            float gradX = (lumaNE + 2.0 * lumaE + lumaSE) - (lumaNW + 2.0 * lumaW + lumaSW);
            float gradY = (lumaSW + 2.0 * lumaS + lumaSE) - (lumaNW + 2.0 * lumaN + lumaNE);
            float edgeMag = length(vec2(gradX, gradY));

            vec4 easuColor = c;
            if (edgeMag > 0.015) {
                vec2 dir = normalize(vec2(gradX, gradY));
                vec2 tangentStep = vec2(-dir.y, dir.x) * st;
                vec4 sPos = texture2D(sTexture, vTextureCoord + tangentStep);
                vec4 sNeg = texture2D(sTexture, vTextureCoord - tangentStep);
                easuColor = mix(c, 0.5 * (sPos + sNeg), clamp(edgeMag * 1.6, 0.0, 0.85));
            }

            // FSR Fase 2: RCAS (Robust Contrast-Adaptive Sharpening)
            // Acotamiento estricto a los valores del vecindario para evitar ringing y artefactos
            vec4 minRing = min(c, min(min(n, s), min(e, w)));
            vec4 maxRing = max(c, max(max(n, s), max(e, w)));

            float sharpnessFactor = clamp(uFsrSharpness, 0.0, 1.0);
            float peak = -1.0 / mix(8.0, 3.5, sharpnessFactor);
            vec4 rcasColor = (n + s + e + w) * peak + easuColor;
            rcasColor /= (4.0 * peak + 1.0);

            color = clamp(rcasColor, minRing, maxRing);
        }
        // 3. Filtro de Nitidez Tradicional (Kernel Laplaciano 3x3)
        else if (uSharpness > 0.01) {
            vec4 north = texture2D(sTexture, vTextureCoord + vec2(0.0, uTexelStep.y));
            vec4 south = texture2D(sTexture, vTextureCoord - vec2(0.0, uTexelStep.y));
            vec4 east  = texture2D(sTexture, vTextureCoord + vec2(uTexelStep.x, 0.0));
            vec4 west  = texture2D(sTexture, vTextureCoord - vec2(uTexelStep.x, 0.0));
            vec4 neighbors = north + south + east + west;
            color = clamp(color * (1.0 + 4.0 * uSharpness) - neighbors * uSharpness, 0.0, 1.0);
        }

        // 3. Ajuste de Brillo
        color.rgb += uBrightness;

        // 4. Ajuste de Contraste con punto pivote en 0.5
        color.rgb = (color.rgb - 0.5) * uContrast + 0.5;

        // 5. Ajuste de Saturación de color (Luminancia Rec. 709)
        float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = mix(vec3(luma), color.rgb, uSaturation);

        // 6. Corrección Gamma para rango dinámico perceptual
        color.rgb = clamp(color.rgb, 0.0, 1.0);
        if (uGamma > 0.01 && abs(uGamma - 1.0) > 0.01) {
            color.rgb = pow(color.rgb, vec3(1.0 / max(uGamma, 0.01)));
        }

        // 7. Filtro de Luz Azul / Modo Descanso Visual (Eye Comfort)
        if (uBlueLightFilter > 0.01) {
            color.b *= (1.0 - uBlueLightFilter * 0.45);
            color.r *= (1.0 + uBlueLightFilter * 0.10);
            color.g *= (1.0 + uBlueLightFilter * 0.03);
            color.rgb = clamp(color.rgb, 0.0, 1.0);
        }

        // 8. Modo Sol Extremo / Accesibilidad de Alto Contraste para Exteriores
        if (uSunMode > 0.01) {
            float sunFactor = clamp(uSunMode, 0.0, 1.0);
            // Elevación dinámica de sombras para combatir el deslumbramiento solar
            vec3 lifted = color.rgb / (color.rgb + vec3(0.35 * (1.0 - color.rgb)));
            color.rgb = mix(color.rgb, lifted, 0.50 * sunFactor);
            // Expansión de luminancia y realce de bordes de alto contraste
            float lum = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            color.rgb = mix(color.rgb, color.rgb + (color.rgb - vec3(lum)) * 0.35, 0.50 * sunFactor);
            color.rgb = mix(vec3(lum), color.rgb, 1.0 + 0.30 * sunFactor);
            color.rgb = clamp(color.rgb, 0.0, 1.0);
        }

        // Asegurar opacidad total para evitar que frames con alfa nulo decodificados por hardware se vean negros
        gl_FragColor = vec4(color.rgb, 1.0);
    }
)glsl";

} // namespace VideoShaders
