package dev.justinianomarcelo.lucidanapp.ui.estadisticas

data class EstadisticaUiState(
    val negocioId: Long? = null,
    val periodoDias: Int = PERIODOS_DIAS[1],
    val vistas: Int = 0,
    val clics: Int = 0,
    val cargando: Boolean = true
) {
    companion object {
        val PERIODOS_DIAS = listOf(7, 30, 90)
    }
}
