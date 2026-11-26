package dev.justinianomarcelo.lucidanapp.ui.planes

import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

data class PlanesUiState(
    val negocioId: Long? = null,
    val planes: List<PlanPublicidad> = emptyList(),
    /** Nivel vigente del negocio (0 = sin plan) y su vencimiento en epoch ms. */
    val nivelActivo: Int = 0,
    val activoHasta: Long = 0,
    val cargando: Boolean = true,
    val contratando: Boolean = false,
    val mensaje: String? = null
)
