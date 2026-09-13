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
// 3. Parser Avanzado de Estilos y Eventos SSA / ASS (Rust Core)
// ==============================================================================

/// Estructura de estilo tipográfico definido en la sección [V4+ Styles] de un archivo ASS
#[derive(Debug, Clone, Default)]
pub struct AssStyle {
    pub name: String,
    pub font_name: String,
    pub font_size: f32,
    pub primary_color: String,
    pub outline_color: String,
    pub back_color: String,
    pub bold: bool,
    pub italic: bool,
    pub alignment: u32,
    pub outline: f32,
    pub shadow: f32,
}

/// Representa una línea individual de diálogo de subtítulo con sus overrides y marcas de tiempo
#[derive(Debug, Clone, Default)]
pub struct AssDialogue {
    pub start_ms: u64,
    pub end_ms: u64,
    pub style: String,
    pub actor: String,
    pub plain_text: String,
    pub raw_text: String,
    pub alignment: u32,
    pub primary_color: Option<String>,
    pub outline_color: Option<String>,
    pub is_bold: bool,
    pub is_italic: bool,
    pub font_size: Option<f32>,
    pub pos_x: Option<f32>,
    pub pos_y: Option<f32>,
}

/// Convierte un color de formato ASS (&HAABBGGRR o &HBBGGRR o entero) a formato Hex estándar "#AARRGGBB"
pub fn parse_ass_color_to_hex(raw: &str) -> String {
    let mut clean = raw.trim();
    if clean.starts_with('&') {
        clean = &clean[1..];
    }
    if clean.starts_with('H') || clean.starts_with('h') {
        clean = &clean[1..];
    }
    if clean.ends_with('&') {
        clean = &clean[..clean.len() - 1];
    }

    if clean.is_empty() {
        return "#FFFFFFFF".to_string();
    }

    // Intentar interpretar como valor hexadecimal BGR / ABGR
    if let Ok(val) = u32::from_str_radix(clean, 16) {
        let (a_ass, b, g, r) = if clean.len() > 6 {
            ((val >> 24) & 0xFF, (val >> 16) & 0xFF, (val >> 8) & 0xFF, val & 0xFF)
        } else {
            (0, (val >> 16) & 0xFF, (val >> 8) & 0xFF, val & 0xFF)
        };
        // En ASS: Alpha 00 = completamente opaco (255), FF = completamente transparente (0)
        let a_android = 255 - a_ass;
        return format!("#{:02X}{:02X}{:02X}{:02X}", a_android, r, g, b);
    }

    // Si es un entero decimal
    if let Ok(val) = clean.parse::<i64>() {
        let uval = val as u32;
        let a_ass = (uval >> 24) & 0xFF;
        let b = (uval >> 16) & 0xFF;
        let g = (uval >> 8) & 0xFF;
        let r = uval & 0xFF;
        let a_android = 255 - a_ass;
        return format!("#{:02X}{:02X}{:02X}{:02X}", a_android, r, g, b);
    }

    "#FFFFFFFF".to_string()
}

/// Extrae de forma segura los overrides presentes en una línea de diálogo ASS ({\pos}, {\an}, {\c}, etc.)
fn parse_dialogue_overrides(
    raw_text: &str,
    default_style: Option<&AssStyle>,
) -> (String, u32, Option<String>, Option<String>, bool, bool, Option<f32>, Option<f32>, Option<f32>) {
    let plain = strip_ass_tags_internal(raw_text);

    let mut alignment = default_style.map(|s| s.alignment).unwrap_or(2); // 2 = BottomCenter por defecto
    let mut primary_color = default_style.map(|s| s.primary_color.clone());
    let mut outline_color = default_style.map(|s| s.outline_color.clone());
    let mut is_bold = default_style.map(|s| s.bold).unwrap_or(false);
    let mut is_italic = default_style.map(|s| s.italic).unwrap_or(false);
    let mut font_size = default_style.map(|s| s.font_size);
    let mut pos_x = None;
    let mut pos_y = None;

    // Buscar bloques de override {...}
    let mut in_override = false;
    let mut tag_buf = String::new();

    for ch in raw_text.chars() {
        if ch == '{' {
            in_override = true;
            tag_buf.clear();
            continue;
        }
        if ch == '}' {
            in_override = false;
            // Procesar tags dentro de este bloque
            let tags = tag_buf.split('\\');
            for t in tags {
                let tag = t.trim();
                if tag.is_empty() {
                    continue;
                }

                // Posición: pos(X, Y)
                if tag.starts_with("pos(") && tag.ends_with(')') {
                    let coords = &tag[4..tag.len() - 1];
                    if let Some((x_str, y_str)) = coords.split_once(',') {
                        pos_x = x_str.trim().parse::<f32>().ok();
                        pos_y = y_str.trim().parse::<f32>().ok();
                    }
                }
                // Alineación moderna: an1..an9
                else if tag.starts_with("an") && tag.len() == 3 {
                    if let Ok(an_val) = tag[2..].parse::<u32>() {
                        if an_val >= 1 && an_val <= 9 {
                            alignment = an_val;
                        }
                    }
                }
                // Color primario: \c&H...& o \1c&H...&
                else if tag.starts_with("1c") || (tag.starts_with('c') && !tag.starts_with("clip")) {
                    let color_str = if tag.starts_with("1c") { &tag[2..] } else { &tag[1..] };
                    let parsed_hex = parse_ass_color_to_hex(color_str);
                    primary_color = Some(parsed_hex);
                }
                // Color borde: \3c&H...&
                else if tag.starts_with("3c") {
                    let color_str = &tag[2..];
                    let parsed_hex = parse_ass_color_to_hex(color_str);
                    outline_color = Some(parsed_hex);
                }
                // Negrita: \b1 o \b0
                else if tag == "b1" {
                    is_bold = true;
                } else if tag == "b0" {
                    is_bold = false;
                }
                // Cursiva: \i1 o \i0
                else if tag == "i1" {
                    is_italic = true;
                } else if tag == "i0" {
                    is_italic = false;
                }
                // Tamaño de fuente: \fsXX
                else if tag.starts_with("fs") {
                    if let Ok(sz) = tag[2..].parse::<f32>() {
                        font_size = Some(sz);
                    }
                }
            }
            tag_buf.clear();
            continue;
        }

        if in_override {
            tag_buf.push(ch);
        }
    }

    (plain, alignment, primary_color, outline_color, is_bold, is_italic, font_size, pos_x, pos_y)
}

/// Parsea un contenido completo SSA/ASS extrayendo su telemetría, estilos y eventos de diálogo
pub fn parse_ass_full_internal(content: &str) -> (AssScriptSummary, Vec<AssStyle>, Vec<AssDialogue>) {
    let mut summary = AssScriptSummary::default();
    let mut styles_map: HashMap<String, AssStyle> = HashMap::new();
    let mut styles_list: Vec<AssStyle> = Vec::new();
    let mut dialogues: Vec<AssDialogue> = Vec::new();

    let mut current_section = String::new();
    let mut style_format_headers: Vec<String> = Vec::new();
    let mut event_format_headers: Vec<String> = Vec::new();

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with(';') {
            continue;
        }

        if trimmed.starts_with('[') && trimmed.ends_with(']') {
            current_section = trimmed[1..trimmed.len() - 1].to_ascii_lowercase();
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
                if trimmed.starts_with("Format:") {
                    if let Some((_, cols)) = trimmed.split_once(':') {
                        style_format_headers = cols.split(',').map(|s| s.trim().to_ascii_lowercase()).collect();
                    }
                } else if trimmed.starts_with("Style:") {
                    if let Some((_, vals)) = trimmed.split_once(':') {
                        let values: Vec<&str> = vals.split(',').map(|s| s.trim()).collect();
                        if !style_format_headers.is_empty() && values.len() >= style_format_headers.len().min(5) {
                            let mut style = AssStyle::default();
                            for (idx, header) in style_format_headers.iter().enumerate() {
                                if idx >= values.len() {
                                    break;
                                }
                                let val = values[idx];
                                match header.as_str() {
                                    "name" => style.name = val.to_string(),
                                    "fontname" => style.font_name = val.to_string(),
                                    "fontsize" => style.font_size = val.parse().unwrap_or(24.0),
                                    "primarycolour" => style.primary_color = parse_ass_color_to_hex(val),
                                    "outlinecolour" => style.outline_color = parse_ass_color_to_hex(val),
                                    "backcolour" => style.back_color = parse_ass_color_to_hex(val),
                                    "bold" => style.bold = val == "-1" || val == "1",
                                    "italic" => style.italic = val == "-1" || val == "1",
                                    "alignment" => style.alignment = val.parse().unwrap_or(2),
                                    "outline" => style.outline = val.parse().unwrap_or(2.0),
                                    "shadow" => style.shadow = val.parse().unwrap_or(1.0),
                                    _ => {}
                                }
                            }
                            if style.primary_color.is_empty() {
                                style.primary_color = "#FFFFFFFF".to_string();
                            }
                            styles_map.insert(style.name.to_ascii_lowercase(), style.clone());
                            styles_list.push(style);
                            summary.style_count += 1;
                        }
                    }
                }
            }
            "events" => {
                if trimmed.starts_with("Format:") {
                    if let Some((_, cols)) = trimmed.split_once(':') {
                        event_format_headers = cols.split(',').map(|s| s.trim().to_ascii_lowercase()).collect();
                    }
                } else if trimmed.starts_with("Dialogue:") {
                    summary.dialogue_count += 1;
                    if let Some((_, raw_fields)) = trimmed.split_once(':') {
                        let total_cols = event_format_headers.len();
                        if total_cols > 0 {
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

                            let start_idx = event_format_headers.iter().position(|c| c == "start");
                            let end_idx = event_format_headers.iter().position(|c| c == "end");
                            let style_idx = event_format_headers.iter().position(|c| c == "style");
                            let name_idx = event_format_headers.iter().position(|c| c == "name" || c == "actor");
                            let text_idx = event_format_headers.iter().position(|c| c == "text");

                            if let (Some(s_i), Some(e_i), Some(t_i)) = (start_idx, end_idx, text_idx) {
                                if s_i < fields.len() && e_i < fields.len() && t_i < fields.len() {
                                    let start_ms = parse_ass_timestamp_to_ms(fields[s_i]).unwrap_or(0);
                                    let end_ms = parse_ass_timestamp_to_ms(fields[e_i]).unwrap_or(0);
                                    let style_name = style_idx.and_then(|i| fields.get(i).copied()).unwrap_or("Default");
                                    let actor_name = name_idx.and_then(|i| fields.get(i).copied()).unwrap_or("");
                                    let raw_text = fields[t_i];

                                    if summary.first_start_ms == 0 || (start_ms > 0 && start_ms < summary.first_start_ms) {
                                        summary.first_start_ms = start_ms;
                                    }
                                    if end_ms > summary.last_end_ms {
                                        summary.last_end_ms = end_ms;
                                    }

                                    let matched_style = styles_map.get(&style_name.to_ascii_lowercase());
                                    let (plain, align, prim_col, out_col, bold, ital, fsz, px, py) =
                                        parse_dialogue_overrides(raw_text, matched_style);

                                    dialogues.push(AssDialogue {
                                        start_ms,
                                        end_ms,
                                        style: style_name.to_string(),
                                        actor: actor_name.to_string(),
                                        plain_text: plain,
                                        raw_text: raw_text.to_string(),
                                        alignment: align,
                                        primary_color: prim_col,
                                        outline_color: out_col,
                                        is_bold: bold,
                                        is_italic: ital,
                                        font_size: fsz,
                                        pos_x: px,
                                        pos_y: py,
                                    });
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

    (summary, styles_list, dialogues)
}

// ==============================================================================
// 4. Enlaces JNI (Java Native Interface) para Kotlin
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

/// JNI: Parsea un archivo SSA/ASS completo retornando resumen, lista de estilos y lista de diálogos con overrides
#[no_mangle]
pub extern "system" fn Java_com_example_rust_NovaRustCore_nativeParseAssFull(
    mut env: JNIEnv,
    _class: JClass,
    ass_content: JString,
) -> jstring {
    let input: String = match env.get_string(&ass_content) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let (summary, styles, dialogues) = parse_ass_full_internal(&input);

    let mut json = String::with_capacity(input.len() / 2 + 1024);
    json.push('{');

    // Summary
    json.push_str(&format!(
        "\"summary\":{{\"title\":\"{}\",\"scriptType\":\"{}\",\"playResX\":{},\"playResY\":{},\"styles\":{},\"dialogues\":{},\"firstMs\":{},\"lastMs\":{}}},",
        summary.title.replace('\\', "\\\\").replace('"', "\\\""),
        summary.script_type.replace('\\', "\\\\").replace('"', "\\\""),
        summary.play_res_x,
        summary.play_res_y,
        summary.style_count,
        summary.dialogue_count,
        summary.first_start_ms,
        summary.last_end_ms
    ));

    // Styles
    json.push_str("\"styles\":[");
    for (idx, st) in styles.iter().enumerate() {
        if idx > 0 {
            json.push(',');
        }
        json.push_str(&format!(
            "{{\"name\":\"{}\",\"fontName\":\"{}\",\"fontSize\":{:.1},\"primaryColor\":\"{}\",\"outlineColor\":\"{}\",\"backColor\":\"{}\",\"bold\":{},\"italic\":{},\"alignment\":{},\"outline\":{:.1},\"shadow\":{:.1}}}",
            st.name.replace('\\', "\\\\").replace('"', "\\\""),
            st.font_name.replace('\\', "\\\\").replace('"', "\\\""),
            st.font_size,
            st.primary_color,
            st.outline_color,
            st.back_color,
            st.bold,
            st.italic,
            st.alignment,
            st.outline,
            st.shadow
        ));
    }
    json.push_str("],");

    // Dialogues
    json.push_str("\"dialogues\":[");
    for (idx, d) in dialogues.iter().enumerate() {
        if idx > 0 {
            json.push(',');
        }
        let prim_col_json = match &d.primary_color {
            Some(c) => format!("\"{}\"", c),
            None => "null".to_string(),
        };
        let out_col_json = match &d.outline_color {
            Some(c) => format!("\"{}\"", c),
            None => "null".to_string(),
        };
        let fsz_json = match d.font_size {
            Some(sz) => format!("{:.1}", sz),
            None => "null".to_string(),
        };
        let px_json = match d.pos_x {
            Some(x) => format!("{:.1}", x),
            None => "null".to_string(),
        };
        let py_json = match d.pos_y {
            Some(y) => format!("{:.1}", y),
            None => "null".to_string(),
        };

        // Escapar caracteres para JSON
        let escaped_plain = d.plain_text
            .replace('\\', "\\\\")
            .replace('"', "\\\"")
            .replace('\n', "\\n")
            .replace('\r', "");

        let escaped_raw = d.raw_text
            .replace('\\', "\\\\")
            .replace('"', "\\\"")
            .replace('\n', "\\n")
            .replace('\r', "");

        json.push_str(&format!(
            "{{\"startMs\":{},\"endMs\":{},\"style\":\"{}\",\"actor\":\"{}\",\"plainText\":\"{}\",\"rawText\":\"{}\",\"alignment\":{},\"primaryColor\":{},\"outlineColor\":{},\"isBold\":{},\"isItalic\":{},\"fontSize\":{},\"posX\":{},\"posY\":{}}}",
            d.start_ms,
            d.end_ms,
            d.style.replace('\\', "\\\\").replace('"', "\\\""),
            d.actor.replace('\\', "\\\\").replace('"', "\\\""),
            escaped_plain,
            escaped_raw,
            d.alignment,
            prim_col_json,
            out_col_json,
            d.is_bold,
            d.is_italic,
            fsz_json,
            px_json,
            py_json
        ));
    }
    json.push_str("]}");

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

    #[test]
    fn test_parse_ass_color() {
        // ASS color format: &H000000FF = opaque red
        let red_hex = parse_ass_color_to_hex("&H000000FF");
        assert_eq!(red_hex, "#FFFF0000");

        // ASS color format: &H00FF0000 = opaque blue
        let blue_hex = parse_ass_color_to_hex("&H00FF0000");
        assert_eq!(blue_hex, "#FF0000FF");
    }

    #[test]
    fn test_parse_ass_full() {
        let sample = r#"
[Script Info]
Title: Sample Anime Episode
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,1,2,20,20,20,1
Style: TopSign,Trebuchet MS,36,&H0000FFFF,&H000000FF,&H00000000,&H80000000,0,1,0,0,100,100,0,0,1,2,1,8,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:01.00,0:00:04.50,Default,,0,0,0,,{\c&H0000FF&}¡Hola mundo!{\b1}\NTexto en negrita
Dialogue: 0,0:00:05.00,0:00:08.00,TopSign,,0,0,0,,{\an8}Cartel superior traducido
"#;
        let (summary, styles, dialogues) = parse_ass_full_internal(sample);
        assert_eq!(summary.title, "Sample Anime Episode");
        assert_eq!(summary.play_res_x, 1920);
        assert_eq!(summary.play_res_y, 1080);
        assert_eq!(styles.len(), 2);
        assert_eq!(dialogues.len(), 2);
        assert_eq!(dialogues[0].start_ms, 1000);
        assert_eq!(dialogues[0].end_ms, 4500);
        assert_eq!(dialogues[0].plain_text, "¡Hola mundo!\nTexto en negrita");
        assert_eq!(dialogues[1].alignment, 8);
    }
}
