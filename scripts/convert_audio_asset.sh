#!/usr/bin/env bash
# ==============================================================================
# convert_audio_asset.sh - Herramienta CLI para optimizar y convertir audios en Nova Player
# ==============================================================================
#
# Descripción:
#   Convierte y comprime efectos de sonido y muestras de audio para su uso eficiente
#   en la aplicación Android, reduciendo tamaño sin degradar la nitidez perceptual.
#   Por defecto convierte a formato Ogg Vorbis (.ogg) a 48 kHz mono con qscale de alta
#   fidelidad, ideal para SoundPool y reproducción con latencia ultra baja.
#
# Uso:
#   ./scripts/convert_audio_asset.sh <archivo_entrada> [archivo_salida] [calidad_o_bitrate]
#
# Ejemplos:
#   ./scripts/convert_audio_asset.sh click.wav app/src/main/res/raw/ui_click.ogg
#   ./scripts/convert_audio_asset.sh sample.mp3 output.ogg 64k
# ==============================================================================

set -euo pipefail

INPUT_FILE="${1:-}"
OUTPUT_FILE="${2:-}"
QUALITY_OR_BITRATE="${3:-5}"

if [ -z "$INPUT_FILE" ]; then
    echo "Uso: $0 <archivo_entrada> [archivo_salida] [calidad (0-10)]"
    echo "Ejemplo: $0 input.wav app/src/main/res/raw/ui_click.ogg 5"
    exit 1
fi

if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: El archivo de entrada '$INPUT_FILE' no existe."
    exit 1
fi

# Si no se especifica archivo de salida, usar el mismo nombre con extensión .ogg
if [ -z "$OUTPUT_FILE" ]; then
    BASE_NAME="${INPUT_FILE%.*}"
    OUTPUT_FILE="${BASE_NAME}.ogg"
fi

# Crear directorio de salida si no existe
OUTPUT_DIR=$(dirname "$OUTPUT_FILE")
mkdir -p "$OUTPUT_DIR"

echo "=========================================================="
echo " [Nova Player] Conversión y Optimización de Audio"
echo " Entrada:  $INPUT_FILE"
echo " Salida:   $OUTPUT_FILE"
echo " Calidad:  $QUALITY_OR_BITRATE"
echo "=========================================================="

# Comprobación de herramientas disponibles
if command -v ffmpeg >/dev/null 2>&1; then
    # Conversión optimizada con FFmpeg:
    # -vn: Sin stream de video
    # -c:a libvorbis: Códec Ogg Vorbis estándar y universal para Android
    # -qscale:a: Calidad VBR (5 = excelente balance peso/fidelidad)
    # -ar 48000: Frecuencia de muestreo estándar de audio digital Android (AAudio/Oboe)
    ffmpeg -y -hide_banner -loglevel warning \
        -i "$INPUT_FILE" \
        -vn \
        -c:a libvorbis \
        -qscale:a "$QUALITY_OR_BITRATE" \
        -ar 48000 \
        "$OUTPUT_FILE"
else
    echo "Error: ffmpeg no está instalado en el sistema."
    exit 1
fi

# Reporte de tamaño antes y después
SIZE_IN=$(wc -c < "$INPUT_FILE" | tr -d ' ')
SIZE_OUT=$(wc -c < "$OUTPUT_FILE" | tr -d ' ')

echo " Conversión finalizada con éxito."
echo " Tamaño original: ${SIZE_IN} bytes"
echo " Tamaño optimizado: ${SIZE_OUT} bytes"
echo " Guardado en: $OUTPUT_FILE"
echo "=========================================================="
