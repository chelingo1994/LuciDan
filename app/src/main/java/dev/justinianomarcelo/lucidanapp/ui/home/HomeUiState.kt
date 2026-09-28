package dev.justinianomarcelo.lucidanapp.ui.home

import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.Lugar
import dev.justinianomarcelo.lucidanapp.location.Coordenada
import org.osmdroid.util.GeoPoint

data class HomeUiState(
    val ubicacionActual: Coordenada? = null,
    val radioSeleccionado: Double = RADIOS_METROS[1],
    val categoriaSeleccionada: CategoriaLugar? = null,
    val lugares: List<Lugar> = emptyList(),
    val puntoSeleccionadoManual: GeoPoint? = null,
    val mensaje: String? = null,
    val cargando: Boolean = false
) {
    companion object {
        val RADIOS_METROS = listOf(500.0, 1000.0, 2000.0, 5000.0)
    }
}
