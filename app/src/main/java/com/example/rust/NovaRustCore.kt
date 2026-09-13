package com.example.rust

import android.util.Log
import org.json.JSONObject

/**
 * Representa los metadatos analizados de un archivo de subtítulos SSA/ASS procesado por el motor en Rust.
 *
 * @param title Título del script o subtítulo.
 * @param scriptType Versión del formato (ej. "v4.00+").
 * @param playResX Resolución horizontal de referencia para escalado de fuentes y posicionamiento.
 * @param playResY Resolución vertical de referencia.
 * @param styleCount Número total de estilos definidos ([V4+ Styles]).
 * @param dialogueCount Número total de líneas de diálogo ([Events]).
 * @param firstMs Marca de tiempo en ms del primer diálogo.
 * @param lastMs Marca de tiempo en ms del último diálogo.
 */
data class AssScriptInfo(
    val title: String = "",
    val scriptType: String = "v4.00+",
    val playResX: Int = 0,
    val playResY: Int = 0,
    val styleCount: Int = 0,
    val dialogueCount: Int = 0,
    val firstMs: Long = 0L,
    val lastMs: Long = 0L
)

/**
 * NovaRustCore - Puente JNI e interfaz con el motor nativo de Rust (novaplayer_rust).
 *
 * Responsabilidades:
 * 1. Procesamiento ultrarrápido y seguro de archivos de subtítulos SSA/ASS.
 * 2. Limpieza de etiquetas de override de ASS ({\pos}, {\c&H...}, \N).
 * 3. Sanitización de nombres de archivo de video para renombrado seguro sin errores de filesystem.
 */
object NovaRustCore {

    private const val TAG = "NovaRustCore"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("novaplayer_rust")
            isNativeLoaded = true
            Log.i(TAG, "Biblioteca nativa novaplayer_rust cargada con éxito.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "No se encontró libnovaplayer_rust.so en runtime: ${e.message}")
            isNativeLoaded = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error cargando libnovaplayer_rust: ${e.message}")
            isNativeLoaded = false
        }
    }

    // =========================================================================
    // Declaraciones de métodos nativos en Rust (JNI)
    // =========================================================================

    @JvmStatic
    private external fun nativeParseAssSubtitles(content: String): String

    @JvmStatic
    private external fun nativeStripAssTags(rawText: String): String

    @JvmStatic
    private external fun nativeSanitizeTitle(rawTitle: String): String

    // =========================================================================
    // Métodos públicos de alto nivel en Kotlin
    // =========================================================================

    /**
     * Parsea el contenido textual de un archivo de subtítulos SSA/ASS con el motor en Rust.
     */
    fun parseAssSubtitles(content: String): AssScriptInfo {
        if (isNativeLoaded) {
            try {
                val jsonStr = nativeParseAssSubtitles(content)
                if (jsonStr.isNotBlank()) {
                    val json = JSONObject(jsonStr)
                    return AssScriptInfo(
                        title = json.optString("title", ""),
                        scriptType = json.optString("scriptType", "v4.00+"),
                        playResX = json.optInt("playResX", 0),
                        playResY = json.optInt("playResY", 0),
                        styleCount = json.optInt("styles", 0),
                        dialogueCount = json.optInt("dialogues", 0),
                        firstMs = json.optLong("firstMs", 0L),
                        lastMs = json.optLong("lastMs", 0L)
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Fallo al procesar ASS mediante JNI Rust: ${e.message}")
            }
        }

        // Procesamiento en Kotlin en caso de que la biblioteca .so aún no esté empaquetada
        return fallbackParseAss(content)
    }

    /**
     * Limpia etiquetas complejas de SSA/ASS de forma segura con Rust.
     */
    fun stripAssTags(rawText: String): String {
        if (isNativeLoaded) {
            try {
                return nativeStripAssTags(rawText)
            } catch (e: Throwable) {
                Log.e(TAG, "Error en nativeStripAssTags: ${e.message}")
            }
        }
        return fallbackStripAssTags(rawText)
    }

    /**
     * Sanitiza y normaliza un título de video escrito por el usuario para su almacenamiento en Room y disco.
     */
    fun sanitizeTitle(rawTitle: String): String {
        if (isNativeLoaded) {
            try {
                val sanitized = nativeSanitizeTitle(rawTitle)
                if (sanitized.isNotBlank()) return sanitized
            } catch (e: Throwable) {
                Log.e(TAG, "Error en nativeSanitizeTitle: ${e.message}")
            }
        }
        return fallbackSanitizeTitle(rawTitle)
    }

    // =========================================================================
    // Lógica espejo de respaldo (garantiza ejecución continua)
    // =========================================================================

    private fun fallbackParseAss(content: String): AssScriptInfo {
        var title = ""
        var scriptType = "v4.00+"
        var playResX = 0
        var playResY = 0
        var styleCount = 0
        var dialogueCount = 0

        for (line in content.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Title:", ignoreCase = true)) {
                title = trimmed.substringAfter(":").trim()
            } else if (trimmed.startsWith("ScriptType:", ignoreCase = true)) {
                scriptType = trimmed.substringAfter(":").trim()
            } else if (trimmed.startsWith("PlayResX:", ignoreCase = true)) {
                playResX = trimmed.substringAfter(":").trim().toIntOrNull() ?: 0
            } else if (trimmed.startsWith("PlayResY:", ignoreCase = true)) {
                playResY = trimmed.substringAfter(":").trim().toIntOrNull() ?: 0
            } else if (trimmed.startsWith("Style:", ignoreCase = true)) {
                styleCount++
            } else if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                dialogueCount++
            }
        }

        return AssScriptInfo(
            title = title,
            scriptType = scriptType,
            playResX = playResX,
            playResY = playResY,
            styleCount = styleCount,
            dialogueCount = dialogueCount
        )
    }

    private fun fallbackStripAssTags(rawText: String): String {
        return rawText
            .replace(Regex("""\{[^}]*\}"""), "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")
            .trim()
    }

    private fun fallbackSanitizeTitle(rawTitle: String): String {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return "Video sin título"
        var clean = trimmed.replace(Regex("""[/\\:*?"<>|\x00]"""), " ")
        for (ext in listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".ts", ".flv")) {
            if (clean.endsWith(ext, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - ext.length)
                break
            }
        }
        val words = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val joined = words.joinToString(" ")
        return if (joined.length > 100) joined.substring(0, 100) else joined.ifEmpty { "Video sin título" }
    }
}
