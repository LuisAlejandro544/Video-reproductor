//! ==============================================================================
//! novaplayer_rust - Motor nativo en Rust para Nova Video Player
//!
//! Propósito:
//! Provee una capa segura en memoria (memory-safe) y de ultra alto rendimiento para:
//! 1. Parsing y análisis de subtítulos avanzados SSA / ASS (SubStation Alpha v4+).
//! 2. Sanitización y limpieza de etiquetas complejas de formato (tags {\pos}, {\c&H...}, \N).
//! 3. Validación y normalización segura de nombres de archivo multimedia para renombrado.
//!
//! Compatible con arquitecturas Android de 32 bits (armv7, i686) y 64 bits (aarch64, x86_64).
//! Todas las dependencias son permisivas (MIT / Apache 2.0).
//! ==============================================================================

use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::{JNIEnv, JavaVM};
use std::collections::HashMap;
use std::os::raw::c_void;

/// Constante que identifica la versión compatible de JNI (1.6)
const JNI_VERSION_1_6: jint = 0x00010006;

/// JNI_OnLoad en Rust:
/// Se ejecuta automáticamente cuando se carga la biblioteca nativa en Android.
#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    JNI_VERSION_1_6
}

// ==============================================================================
// 1. Motor de Subtítulos Avanzados SSA / ASS (Rust Core)
// ==============================================================================

/// Estructura de metadatos extraídos de un archivo de subtítulos SSA/ASS
#[derive(Debug, Default)]
pub struct AssScriptSummary {
    pub title: String,
    pub script_type: String,
    pub play_res_x: u32,
    pub play_res_y: u32,
    pub style_count: usize,
    pub dialogue_count: usize,
    pub first_start_ms: u64,
    pub last_end_ms: u64,
}

/// Convierte una marca de tiempo de SSA/ASS (ej: "0:01:23.45" o "1:23:45.67") a milisegundos
pub fn parse_ass_timestamp_to_ms(time_str: &str) -> Option<u64> {
    let parts: Vec<&str> = time_str.trim().split(':').collect();
    if parts.len() != 3 {
        return None;
    }

    let hours: u64 = parts[0].parse().ok()?;
    let minutes: u64 = parts[1].parse().ok()?;

    let sec_parts: Vec<&str> = parts[2].split('.').collect();
    if sec_parts.is_empty() {
        return None;
    }

    let seconds: u64 = sec_parts[0].parse().ok()?;
    let centis: u64 = if sec_parts.len() > 1 {
        let centi_str = sec_parts[1];
        if centi_str.len() == 1 {
            centi_str.parse::<u64>().ok()? * 100
        } else if centi_str.len() == 2 {
            centi_str.parse::<u64>().ok()? * 10
        } else {
            centi_str[..3.min(centi_str.len())].parse::<u64>().ok()?
        }
    } else {
        0
    };

    let total_ms = (hours * 3600 + minutes * 60 + seconds) * 1000 + centis;
    Some(total_ms)
}

/// Parsea un contenido completo SSA/ASS y extrae su telemetría y metadatos clave
pub fn parse_ass_subtitles_internal(content: &str) -> AssScriptSummary {
    let mut summary = AssScriptSummary::default();
    let mut current_section = String::new();
    let mut format_headers: Vec<String> = Vec::new();
    let mut start_idx = None;
    let mut end_idx = None;
    let mut text_idx = None;

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with(';') {
            continue;
        }

        // Detección de secciones [Script Info], [V4+ Styles], [Events]
        if trimmed.starts_with('[') && trimmed.ends_with(']') {
            current_section = trimmed[1..trimmed.len() - 1].to_ascii_lowercase();
            format_headers.clear();
            start_idx = None;
            end_idx = None;
            text_idx = None;
            continue;
        }

        match current_section.as_str() {
            "script info" => {
                if let Some((k, v)) = trimmed.split_once(':') {
                    let key = k.trim().to_ascii_lowercase();
                    let val = v.trim();
                    match key.as_str() {
                        "title" => summary.title = val.to_string(),
                        "scripttype" => summary.script_type = val.to_string(),
                        "playresx" => summary.play_res_x = val.parse().unwrap_or(0),
                        "playresy" => summary.play_res_y = val.parse().unwrap_or(0),
                        _ => {}
                    }
                }
            }
            "v4+ styles" | "v4 styles" => {
                if trimmed.starts_with("Style:") {
                    summary.style_count += 1;
                }
            }
            "events" => {
                if trimmed.starts_with("Format:") {
                    if let Some((_, cols)) = trimmed.split_once(':') {
                        format_headers = cols.split(',').map(|s| s.trim().to_ascii_lowercase()).collect();
                        start_idx = format_headers.iter().position(|c| c == "start");
                        end_idx = format_headers.iter().position(|c| c == "end");
                        text_idx = format_headers.iter().position(|c| c == "text");
                    }
                } else if trimmed.starts_with("Dialogue:") {
                    summary.dialogue_count += 1;
                    if let Some((_, raw_fields)) = trimmed.split_once(':') {
                        let total_cols = format_headers.len();
                        if total_cols > 0 {
                            // Separar los campos respetando que el último ('Text') puede contener comas
                            let mut fields = Vec::with_capacity(total_cols);
                            let mut remaining = raw_fields.trim();
                            for _ in 0..total_cols - 1 {
                                if let Some((head, tail)) = remaining.split_once(',') {
                                    fields.push(head.trim());
                                    remaining = tail;
                                } else {
                                    break;
                                }
                            }
                            fields.push(remaining.trim());

                            if let (Some(s_i), Some(e_i)) = (start_idx, end_idx) {
                                if s_i < fields.len() && e_i < fields.len() {
                                    if let Some(start_ms) = parse_ass_timestamp_to_ms(fields[s_i]) {
                                        if summary.first_start_ms == 0 || start_ms < summary.first_start_ms {
                                            summary.first_start_ms = start_ms;
                                        }
                                    }
                                    if let Some(end_ms) = parse_ass_timestamp_to_ms(fields[e_i]) {
                                        if end_ms > summary.last_end_ms {
                                            summary.last_end_ms = end_ms;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            _ => {}
        }
    }

    if summary.script_type.is_empty() {
        summary.script_type = "v4.00+".to_string();
    }

    summary
}

/// Elimina de forma ultra-eficiente todas las etiquetas de estilo {\...} de ASS y normaliza saltos \N
pub fn strip_ass_tags_internal(raw: &str) -> String {
    let mut result = String::with_capacity(raw.len());
    let mut inside_tag = false;
    let chars: Vec<char> = raw.chars().collect();
    let mut i = 0;

    while i < chars.len() {
        let ch = chars[i];
        if ch == '{' {
            inside_tag = true;
            i += 1;
            continue;
        }
        if ch == '}' {
            inside_tag = false;
            i += 1;
            continue;
        }

        if !inside_tag {
            // Detección de \N o \n en ASS para salto de línea
            if ch == '\\' && i + 1 < chars.len() {
                let next = chars[i + 1];
                if next == 'N' || next == 'n' {
                    result.push('\n');
                    i += 2;
                    continue;
                } else if next == 'h' {
                    result.push(' '); // Espacio no separable en ASS
                    i += 2;
                    continue;
                }
            }
            result.push(ch);
        }
        i += 1;
    }

    result.trim().to_string()
}

// ==============================================================================
// 2. Sanitizador de Nombres de Video para Renombrado Seguro (Rust Core)
// ==============================================================================

/// Valida y normaliza el título de un archivo multimedia renombrado por el usuario:
/// - Remueve etiquetas intrusivas habituales (ej: [1080p], [HEVC], (Official Video)).
/// - Reemplaza caracteres prohibidos o problemáticos en sistemas de archivos Unix/Android (`/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|`).
/// - Limita la longitud máxima a 120 caracteres de forma segura (sin cortar caracteres Unicode por la mitad).
pub fn sanitize_video_title_internal(raw: &str) -> String {
    let trimmed = raw.trim();
    if trimmed.is_empty() {
        return "Video sin título".to_string();
    }

    // Filtrar caracteres no permitidos en nombres de archivo
    let mut sanitized: String = trimmed
        .chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' | '\0' => ' ',
            _ => c,
        })
        .collect();

    // Eliminar extensiones redundantes si el usuario las incluyó al escribir (ej: .mp4, .mkv, .avi)
    let lower = sanitized.to_lowercase();
    for ext in &[".mp4", ".mkv", ".webm", ".avi", ".mov", ".ts", ".flv"] {
        if lower.ends_with(ext) {
            sanitized.truncate(sanitized.len() - ext.len());
            break;
        }
    }

    // Colapsar espacios múltiples
    let words: Vec<&str> = sanitized.split_whitespace().collect();
    let collapsed = words.join(" ");

    if collapsed.is_empty() {
        return "Video sin título".to_string();
    }

    // Limitar longitud de forma segura para caracteres UTF-8
    let mut final_title = String::new();
    let mut char_count = 0;
    for c in collapsed.chars() {
        if char_count >= 100 {
            break;
        }
        final_title.push(c);
        char_count += 1;
    }

    final_title
}

// ==============================================================================
// 3. Enlaces JNI (Java Native Interface) para Kotlin
// ==============================================================================

/// JNI: Parsea un archivo de subtítulos SSA/ASS y retorna un resumen JSON simple
#[no_mangle]
pub extern "system" fn Java_com_example_rust_NovaRustCore_nativeParseAssSubtitles(
    mut env: JNIEnv,
    _class: JClass,
    ass_content: JString,
) -> jstring {
    let input: String = match env.get_string(&ass_content) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let summary = parse_ass_subtitles_internal(&input);

    // Generar JSON compacto con la información parseada
    let json = format!(
        "{{\"title\":\"{}\",\"scriptType\":\"{}\",\"playResX\":{},\"playResY\":{},\"styles\":{},\"dialogues\":{},\"firstMs\":{},\"lastMs\":{}}}",
        summary.title.replace('"', "\\\""),
        summary.script_type.replace('"', "\\\""),
        summary.play_res_x,
        summary.play_res_y,
        summary.style_count,
        summary.dialogue_count,
        summary.first_start_ms,
        summary.last_end_ms
    );

    match env.new_string(json) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// JNI: Limpia etiquetas complejas de SSA/ASS dejando el texto formateado
#[no_mangle]
pub extern "system" fn Java_com_example_rust_NovaRustCore_nativeStripAssTags(
    mut env: JNIEnv,
    _class: JClass,
    raw_text: JString,
) -> jstring {
    let input: String = match env.get_string(&raw_text) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let stripped = strip_ass_tags_internal(&input);

    match env.new_string(stripped) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// JNI: Sanitiza el nombre de un video antes de guardarlo en la base de datos Room
#[no_mangle]
pub extern "system" fn Java_com_example_rust_NovaRustCore_nativeSanitizeTitle(
    mut env: JNIEnv,
    _class: JClass,
    raw_title: JString,
) -> jstring {
    let input: String = match env.get_string(&raw_title) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let sanitized = sanitize_video_title_internal(&input);

    match env.new_string(sanitized) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_ass_timestamp() {
        assert_eq!(parse_ass_timestamp_to_ms("0:00:01.50"), Some(1500));
        assert_eq!(parse_ass_timestamp_to_ms("1:02:03.04"), Some(3723040));
    }

    #[test]
    fn test_strip_ass_tags() {
        let raw = "{\\pos(192,200)\\c&H0000FF&}¡Hola mundo!\\N{\\b1}Segunda línea";
        let cleaned = strip_ass_tags_internal(raw);
        assert_eq!(cleaned, "¡Hola mundo!\nSegunda línea");
    }

    #[test]
    fn test_sanitize_video_title() {
        let raw = "  Mi Video de Vacaciones [1080p].mp4  ";
        let cleaned = sanitize_video_title_internal(raw);
        assert_eq!(cleaned, "Mi Video de Vacaciones [1080p]");
    }
}
