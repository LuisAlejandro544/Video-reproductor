# Reglas y Directrices de Desarrollo para Agentes IA (AGENTS.md)

Este archivo contiene las directrices obligatorias y restricciones de ingeniería que deben regir todas las intervenciones de modelos y agentes IA en este repositorio.

---

## 📱 Contexto del Desarrollador y Plataforma
1. **Entorno del Usuario:** El usuario opera directamente desde un teléfono móvil (sin PC de escritorio). Las respuestas y el código deben ser precisos, auto-contenidos y listos para compilar directamente sin requerir pasos manuales complejos.
2. **Canal de Distribución:** La aplicación se distribuye a través de tiendas alternativas y APK de terceros (como Uptodown) y **NO** a través de Google Play Store. Por tanto, no se deben introducir dependencias vinculadas a Google Play Services ni servicios propietarios que restrinjan la portabilidad.
3. **Mensajes de Commit:** Si existe o se genera un archivo `commit_message.txt`, su contenido debe redactarse obligatoriamente en **español**, y no debe modificarse a menos que el usuario lo solicite explícitamente.

---

## 🧠 Metodología de Razonamiento y Modificación
1. **Razonamiento Previo:** Antes de aplicar cualquier modificación o ejecutar herramientas, razonar de manera metódica: identificar qué archivos intervenir, qué dependencias se necesitan y cómo encaja el cambio en la arquitectura global.
2. **Inspección Selectiva de Archivos:** No revisar ni leer archivos que no sean estrictamente necesarios para la solicitud en curso. Mantener las operaciones acotadas al alcance requerido.
3. **Claridad en el Código:** Todos los archivos de código fuente creados o modificados deben incluir comentarios y documentación clara explicando la lógica de su contenido para evitar confusiones.

---

## ⚙️ Directrices Técnicas de Compilación y Dependencias
1. **Integración Rigurosa de Lenguajes Compilados:**
   - Si el proyecto involucra **Kotlin, C++ o Rust**, todos deben estar correctamente integrados y configurados en los scripts de Gradle (`build.gradle.kts`, `CMakeLists.txt`, `Cargo.toml`).
   - Está terminantemente prohibido omitir la compilación nativa o sustituirla con funciones simuladas (*fallbacks*) en Kotlin si el usuario pidió implementarlo en C++ o Rust.
2. **Uso de Dependencias Reales:**
   - Al usuario no le preocupa el peso final del APK siempre y cuando las dependencias sean 100% funcionales y de calidad. Evitar crear soluciones precarias sin dependencias cuando existan bibliotecas consolidadas y probadas.
3. **Licencias de Software:**
   - No recomendar ni incluir dependencias con licencias copyleft restrictivas (como GPL o AGPL) que obliguen a abrir el código del proyecto o exigir menciones obligatorias de créditos. Usar siempre licencias permisivas (Apache 2.0, MIT, BSD, zlib).
4. **Soporte de Arquitecturas:**
   - Diseñar y compilar contemplando explícitamente arquitecturas de **32 bits** (`armeabi-v7a`, `x86`) y **64 bits** (`arm64-v8a`, `x86_64`), así como compatibilidad con dispositivos de recursos limitados (**Android Go**).
5. **Versión Mínima del Sistema (minSdk):**
   - Evaluar siempre el impacto de la versión mínima de Android antes de introducir nuevas APIs del sistema. La versión actual está fijada en **Android 8.0 (API 26)** para garantizar soporte nativo a **AAudio / Google Oboe**.

---

## 🛡️ Seguridad y Buenas Prácticas
1. **Marcas Registradas:** Evitar el uso de nombres comerciales protegidos por derechos de autor en paquetes, rutas o recursos que puedan comprometer al usuario.
2. **Restricción de Propiedades del Sistema:** Bajo ninguna circunstancia utilizar `persist.sys.*` o llamadas al sistema que alteren configuraciones internas no autorizadas.
