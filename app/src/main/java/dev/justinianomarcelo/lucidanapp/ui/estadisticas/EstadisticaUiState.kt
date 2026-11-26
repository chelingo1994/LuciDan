package dev.justinianomarcelo.lucidanapp.ui.estadisticas

import dev.justinianomarcelo.lucidanapp.data.repository.PromedioRubro

data class EstadisticaUiState(
    val negocioId: Long? = null,
    val periodoDias: Int = PERIODOS_DIAS[1],
    /** false: el plan del negocio no incluye el filtro por período y se muestran los totales. */
    val porPeriodo: Boolean = false,
    val vistas: Int = 0,
    val clics: Int = 0,
    /** null si el plan no incluye la comparación. */
    val promedioRubro: PromedioRubro? = null,
    val rubro: String = "",
    val cargando: Boolean = true
) {
    companion object {
        val PERIODOS_DIAS = listOf(7, 30, 90)
    }
}
