package dev.justinianomarcelo.lucidanapp.ui.planes

import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

data class PlanesUiState(
    val negocioId: Long? = null,
    val planes: List<PlanPublicidad> = emptyList(),
    val contratacionActiva: ContratacionPublicidad? = null,
    val cargando: Boolean = true,
    val contratando: Boolean = false,
    val mensaje: String? = null
)
