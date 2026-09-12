package com.example.model

import android.net.Uri

/**
 * Modelo de datos que representa un video seleccionado por el usuario.
 *
 * Contiene la URI del archivo (proporcionada por la Galería o el Gestor de Archivos de Android),
 * el nombre del archivo y el tamaño en bytes para mostrar información en pantalla similar a un reproductor de PC.
 */
data class VideoItem(
    val uri: Uri,
    val name: String,
    val size: Long = 0L,
    val formattedSize: String = "",
    val durationMs: Long = 0L,
    val formattedDuration: String = "00:00"
)
