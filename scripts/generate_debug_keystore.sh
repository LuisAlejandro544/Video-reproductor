#!/usr/bin/env bash
# ==============================================================================
# Script de Generación de Firma Debug (debug.keystore) para CI/CD y Compilación Local
# ==============================================================================
# Propósito:
#   Garantiza que el archivo 'debug.keystore' exista en la raíz del proyecto antes
#   de ejecutar la tarea de compilación de Gradle ('assembleDebug').
#   Evita que el runner de GitHub Actions o el entorno de compilación se quede
#   bloqueado esperando una firma inexistente o solicitando contraseñas interactivas.
#
# Parámetros de la firma debug estándar de Android:
#   - Keystore: debug.keystore
#   - Alias: androiddebugkey
#   - Store Password: android
#   - Key Password: android
#   - Algoritmo: RSA (2048 bits)
#   - Validez: 10000 días
#   - DName: CN=Android Debug,O=Android,C=US
# ==============================================================================

set -euo pipefail

# 1. Determinar el directorio raíz del proyecto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_KEYSTORE="${1:-${ROOT_DIR}/debug.keystore}"

echo "========================================================"
echo " [Nova Video Player] Generador de Firma Debug Automático"
echo "========================================================"
echo "Ruta destino de la firma: ${TARGET_KEYSTORE}"

# 2. Localizar la herramienta 'keytool' del JDK
KEYTOOL_CMD="keytool"
if ! command -v keytool &> /dev/null; then
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/keytool" ]]; then
        KEYTOOL_CMD="${JAVA_HOME}/bin/keytool"
    else
        echo "❌ ERROR: No se encontró la herramienta 'keytool' en el PATH ni en JAVA_HOME."
        echo "Asegúrate de tener un JDK (versión 17+) instalado y configurado."
        exit 1
    fi
fi

# 3. Forzar eliminación de cualquier archivo residual o corrupto
if [[ -f "${TARGET_KEYSTORE}" ]]; then
    echo "⚠️ Se detectó un archivo previo en '${TARGET_KEYSTORE}'. Regenerando desde cero..."
    rm -f "${TARGET_KEYSTORE}"
fi

# 4. Generar el nuevo almacén de claves (keystore) debug de forma no interactiva
echo "🔑 Generando 'debug.keystore' con algoritmo RSA 2048 bits..."
"${KEYTOOL_CMD}" -genkeypair \
    -v \
    -keystore "${TARGET_KEYSTORE}" \
    -storetype PKCS12 \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"

# 5. Verificar la integridad y validez del archivo generado
echo "🔍 Validando integridad del almacén de claves recién generado..."
if "${KEYTOOL_CMD}" -list -keystore "${TARGET_KEYSTORE}" -storepass android -alias androiddebugkey &> /dev/null; then
    echo "✅ Firma debug generada y validada exitosamente en: ${TARGET_KEYSTORE}"
    echo "Tamaño del archivo: $(wc -c < "${TARGET_KEYSTORE}") bytes"
    echo "========================================================"
    exit 0
else
    echo "❌ ERROR: La verificación de la firma debug falló."
    exit 1
fi
