package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.EstadisticaDao
import dev.justinianomarcelo.lucidanapp.data.model.EstadisticaNegocio
import dev.justinianomarcelo.lucidanapp.data.model.TipoEvento
import java.util.Calendar
import java.util.Date

data class ResumenEstadisticas(val vistas: Int, val clics: Int)

/** DS10 — Registro y consulta de estadísticas (HU11). */
class EstadisticaRepository(private val estadisticaDao: EstadisticaDao) {

    suspend fun registrarEvento(negocioId: Long, tipo: TipoEvento) {
        estadisticaDao.insertar(EstadisticaNegocio(negocioId = negocioId, tipo = tipo, fecha = Date()))
    }

    /** DS10: vistas y clics de "Cómo llegar" en los últimos [periodoDias] días. */
    suspend fun obtenerConteo(negocioId: Long, periodoDias: Int): ResumenEstadisticas {
        val hasta = Date()
        val desde = Calendar.getInstance().apply {
            time = hasta
            add(Calendar.DAY_OF_YEAR, -periodoDias)
        }.time
        val vistas = estadisticaDao.contarPorTipo(negocioId, TipoEvento.VISTA, desde, hasta)
        val clics = estadisticaDao.contarPorTipo(negocioId, TipoEvento.CLIC_COMO_LLEGAR, desde, hasta)
        return ResumenEstadisticas(vistas, clics)
    }
}
