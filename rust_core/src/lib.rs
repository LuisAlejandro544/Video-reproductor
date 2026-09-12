//! ==============================================================================
//! novaplayer_rust - Motor nativo en Rust para Nova Video Player
//!
//! Propósito:
//! Provee una capa segura de memoria para procesamiento de flujos multimedia,
//! parsers de contenedores y futuras operaciones de ecualización digital / red.
//!
//! Compatible con arquitecturas de 32 bits (armv7, i686) y 64 bits (aarch64, x86_64).
//!
//! En esta fase inicial, se definen las dependencias base y la estructura inicial
//! sin implementar lógica de ejecución activa, manteniendo el entorno listo para
//! enlazar con C++ y Kotlin cuando se requiera.
//! ==============================================================================

use jni::sys::jint;
use jni::JavaVM;
use std::os::raw::c_void;


/// Constante que identifica la versión compatible de JNI (1.6)
const JNI_VERSION_1_6: jint = 0x00010006;

/// JNI_OnLoad en Rust:
/// Se ejecuta automáticamente cuando se carga la biblioteca nativa en Android.
#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    // Inicialización del runtime de Rust para Android
    JNI_VERSION_1_6
}
