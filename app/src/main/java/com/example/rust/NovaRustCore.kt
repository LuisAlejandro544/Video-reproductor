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
 * Representa un estilo tipográfico de subtítulo SSA/ASS analizado por Rust Core.
 */
data class AssStyleItem(
    val name: String = "Default",
    val fontName: String = "Arial",
    val fontSize: Float = 24.0f,
    val primaryColor: String = "#FFFFFFFF",
    val outlineColor: String = "#FF000000",
    val backColor: String = "#80000000",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val alignment: Int = 2, // 1..9 (Numpad)
    val outline: Float = 2.0f,
    val shadow: Float = 1.0f
)

/**
 * Representa una línea individual de diálogo de subtítulo SSA/ASS con marcas de tiempo,
 * alineación de pantalla, colores por tag y estilos tipográficos.
 */
data class AssDialogueItem(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val style: String = "Default",
    val actor: String = "",
    val plainText: String = "",
    val rawText: String = "",
    val alignment: Int = 2, // 1..9 (1=BottomLeft, 2=BottomCenter, 3=BottomRight, 4=MidLeft, 5=Center, 6=MidRight, 7=TopLeft, 8=TopCenter, 9=TopRight)
    val primaryColor: String? = null,
    val outlineColor: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val fontSize: Float? = null,
    val posX: Float? = null,
    val posY: Float? = null
)

/**
 * Resultado completo del análisis sintáctico de subtítulos complejos SSA/ASS en Rust Core.
 */
data class AssFullResult(
    val summary: AssScriptInfo = AssScriptInfo(),
    val styles: List<AssStyleItem> = emptyList(),
    val dialogues: List<AssDialogueItem> = emptyList()
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
    private external fun nativeParseAssFull(content: String): String

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
     * Parsea completamente un script SSA/ASS con Rust Core extrayendo metadata, estilos y diálogos con formato.
     */
    fun parseAssFull(content: String): AssFullResult {
        if (isNativeLoaded) {
            try {
                val jsonStr = nativeParseAssFull(content)
                if (jsonStr.isNotBlank()) {
                    val root = JSONObject(jsonStr)
                    val summaryObj = root.optJSONObject("summary")
                    val summary = if (summaryObj != null) {
                        AssScriptInfo(
                            title = summaryObj.optString("title", ""),
                            scriptType = summaryObj.optString("scriptType", "v4.00+"),
                            playResX = summaryObj.optInt("playResX", 0),
                            playResY = summaryObj.optInt("playResY", 0),
                            styleCount = summaryObj.optInt("styles", 0),
                            dialogueCount = summaryObj.optInt("dialogues", 0),
                            firstMs = summaryObj.optLong("firstMs", 0L),
                            lastMs = summaryObj.optLong("lastMs", 0L)
                        )
                    } else {
                        AssScriptInfo()
                    }

                    val stylesList = mutableListOf<AssStyleItem>()
                    val stylesArr = root.optJSONArray("styles")
                    if (stylesArr != null) {
                        for (i in 0 until stylesArr.length()) {
                            val st = stylesArr.getJSONObject(i)
                            stylesList.add(
                                AssStyleItem(
                                    name = st.optString("name", "Default"),
                                    fontName = st.optString("fontName", "Arial"),
                                    fontSize = st.optDouble("fontSize", 24.0).toFloat(),
                                    primaryColor = st.optString("primaryColor", "#FFFFFFFF"),
                                    outlineColor = st.optString("outlineColor", "#FF000000"),
                                    backColor = st.optString("backColor", "#80000000"),
                                    bold = st.optBoolean("bold", false),
                                    italic = st.optBoolean("italic", false),
                                    alignment = st.optInt("alignment", 2),
                                    outline = st.optDouble("outline", 2.0).toFloat(),
                                    shadow = st.optDouble("shadow", 1.0).toFloat()
                                )
                            )
                        }
                    }

                    val dialoguesList = mutableListOf<AssDialogueItem>()
                    val dialoguesArr = root.optJSONArray("dialogues")
                    if (dialoguesArr != null) {
                        for (i in 0 until dialoguesArr.length()) {
                            val dg = dialoguesArr.getJSONObject(i)
                            dialoguesList.add(
                                AssDialogueItem(
                                    startMs = dg.optLong("startMs", 0L),
                                    endMs = dg.optLong("endMs", 0L),
                                    style = dg.optString("style", "Default"),
                                    actor = dg.optString("actor", ""),
                                    plainText = dg.optString("plainText", ""),
                                    rawText = dg.optString("rawText", ""),
                                    alignment = dg.optInt("alignment", 2),
                                    primaryColor = if (dg.has("primaryColor") && !dg.isNull("primaryColor")) dg.optString("primaryColor") else null,
                                    outlineColor = if (dg.has("outlineColor") && !dg.isNull("outlineColor")) dg.optString("outlineColor") else null,
                                    isBold = dg.optBoolean("isBold", false),
                                    isItalic = dg.optBoolean("isItalic", false),
                                    fontSize = if (dg.has("fontSize") && !dg.isNull("fontSize")) dg.optDouble("fontSize").toFloat() else null,
                                    posX = if (dg.has("posX") && !dg.isNull("posX")) dg.optDouble("posX").toFloat() else null,
                                    posY = if (dg.has("posY") && !dg.isNull("posY")) dg.optDouble("posY").toFloat() else null
                                )
                            )
                        }
                    }

                    return AssFullResult(
                        summary = summary,
                        styles = stylesList,
                        dialogues = dialoguesList
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en nativeParseAssFull: ${e.message}")
            }
        }
        return fallbackParseAssFull(content)
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

    private fun fallbackParseAssFull(content: String): AssFullResult {
        var title = ""
        var scriptType = "v4.00+"
        var playResX = 0
        var playResY = 0
        val styles = mutableListOf<AssStyleItem>()
        val dialogues = mutableListOf<AssDialogueItem>()
        var minStart = Long.MAX_VALUE
        var maxEnd = 0L

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
                val parts = trimmed.substringAfter(":").split(",")
                if (parts.isNotEmpty()) {
                    val name = parts.getOrNull(0)?.trim() ?: "Default"
                    val fontName = parts.getOrNull(1)?.trim() ?: "Arial"
                    val fontSize = parts.getOrNull(2)?.trim()?.toFloatOrNull() ?: 24.0f
                    val bold = parts.getOrNull(7)?.trim() == "-1" || parts.getOrNull(7)?.trim() == "1"
                    val italic = parts.getOrNull(8)?.trim() == "-1" || parts.getOrNull(8)?.trim() == "1"
                    val alignment = parts.getOrNull(18)?.trim()?.toIntOrNull() ?: 2
                    styles.add(
                        AssStyleItem(
                            name = name,
                            fontName = fontName,
                            fontSize = fontSize,
                            bold = bold,
                            italic = italic,
                            alignment = alignment
                        )
                    )
                }
            } else if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val parts = trimmed.substringAfter(":").split(",", limit = 10)
                if (parts.size >= 10) {
                    val startStr = parts[1].trim()
                    val endStr = parts[2].trim()
                    val style = parts[3].trim()
                    val actor = parts[4].trim()
                    val text = parts[9].trim()

                    val startMs = parseAssTimestamp(startStr)
                    val endMs = parseAssTimestamp(endStr)
                    if (startMs < minStart) minStart = startMs
                    if (endMs > maxEnd) maxEnd = endMs

                    val plain = fallbackStripAssTags(text)
                    var alignment = 2
                    var isBold = false
                    var isItalic = false
                    var posX: Float? = null
                    var posY: Float? = null

                    val anMatch = Regex("""\\an(\d)""").find(text)
                    if (anMatch != null) {
                        alignment = anMatch.groupValues[1].toIntOrNull() ?: 2
                    }
                    if (text.contains("\\b1")) isBold = true
                    if (text.contains("\\i1")) isItalic = true

                    val posMatch = Regex("""\\pos\s*\(\s*([\d.]+)\s*,\s*([\d.]+)\s*\)""").find(text)
                    if (posMatch != null) {
                        posX = posMatch.groupValues[1].toFloatOrNull()
                        posY = posMatch.groupValues[2].toFloatOrNull()
                    }

                    dialogues.add(
                        AssDialogueItem(
                            startMs = startMs,
                            endMs = endMs,
                            style = style,
                            actor = actor,
                            plainText = plain,
                            rawText = text,
                            alignment = alignment,
                            isBold = isBold,
                            isItalic = isItalic,
                            posX = posX,
                            posY = posY
                        )
                    )
                }
            }
        }

        val summary = AssScriptInfo(
            title = title,
            scriptType = scriptType,
            playResX = playResX,
            playResY = playResY,
            styleCount = styles.size,
            dialogueCount = dialogues.size,
            firstMs = if (minStart != Long.MAX_VALUE) minStart else 0L,
            lastMs = maxEnd
        )

        return AssFullResult(
            summary = summary,
            styles = styles,
            dialogues = dialogues
        )
    }

    private fun parseAssTimestamp(ts: String): Long {
        val parts = ts.split(":")
        if (parts.size == 3) {
            val h = parts[0].toLongOrNull() ?: 0L
            val m = parts[1].toLongOrNull() ?: 0L
            val secParts = parts[2].split(".")
            val s = secParts[0].toLongOrNull() ?: 0L
            val cs = if (secParts.size > 1) {
                val sub = secParts[1].padEnd(3, '0').take(3)
                sub.toLongOrNull() ?: 0L
            } else 0L
            return h * 3600000L + m * 60000L + s * 1000L + cs
        }
        return 0L
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
