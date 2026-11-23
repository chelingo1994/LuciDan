package dev.justinianomarcelo.lucidanapp.ui.detalle

import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.ruta.ResultadoRuta

data class DetalleUiState(
    val negocio: Negocio? = null,
    val resenas: List<Resena> = emptyList(),
    val esFavorito: Boolean = false,
    val ruta: ResultadoRuta? = null,
    val calculandoRuta: Boolean = false,
    val calificacionSeleccionada: Int = 0,
    val comentario: String = "",
    val enviandoResena: Boolean = false,
    val mensaje: String? = null,
    val requiereSesion: Boolean = false,
    val cargando: Boolean = true
)
