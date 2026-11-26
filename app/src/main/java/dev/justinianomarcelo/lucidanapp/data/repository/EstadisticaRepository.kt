package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.EstadisticaDao
import dev.justinianomarcelo.lucidanapp.data.model.EstadisticaNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.TipoEvento
import dev.justinianomarcelo.lucidanapp.util.ComparacionRubro
import java.util.Calendar
import java.util.Date

data class ResumenEstadisticas(val vistas: Int, val clics: Int)

/** Promedio por negocio de los otros [otros] negocios publicados del rubro (null si no hay ninguno). */
data class PromedioRubro(val otros: Int, val vistas: Double?, val clics: Double?)

/** DS10 — Registro y consulta de estadísticas (HU11). */
class EstadisticaRepository(private val estadisticaDao: EstadisticaDao) {

    suspend fun registrarEvento(negocioId: Long, tipo: TipoEvento) {
        estadisticaDao.insertar(EstadisticaNegocio(negocioId = negocioId, tipo = tipo, fecha = Date()))
    }

    /** DS10: vistas y clics de "Cómo llegar" en los últimos [periodoDias] días; null = totales. */
    suspend fun obtenerConteo(negocioId: Long, periodoDias: Int?): ResumenEstadisticas {
        val hasta = Date()
        val desde = inicioDelPeriodo(hasta, periodoDias)
        val vistas = estadisticaDao.contarPorTipo(negocioId, TipoEvento.VISTA, desde, hasta)
        val clics = estadisticaDao.contarPorTipo(negocioId, TipoEvento.CLIC_COMO_LLEGAR, desde, hasta)
        return ResumenEstadisticas(vistas, clics)
    }

    /** Solo con los datos de este teléfono: las estadísticas no se sincronizan. */
    suspend fun obtenerPromedioRubro(negocioId: Long, categoria: Rubro, periodoDias: Int?): PromedioRubro {
        val hasta = Date()
        val desde = inicioDelPeriodo(hasta, periodoDias)
        val otros = estadisticaDao.contarOtrosDelRubro(negocioId, categoria)
        suspend fun promedio(tipo: TipoEvento) = ComparacionRubro.promedio(
            estadisticaDao.contarDeOtrosDelRubro(negocioId, categoria, tipo, desde, hasta), otros
        )
        return PromedioRubro(otros, promedio(TipoEvento.VISTA), promedio(TipoEvento.CLIC_COMO_LLEGAR))
    }

    private fun inicioDelPeriodo(hasta: Date, periodoDias: Int?): Date =
        if (periodoDias == null) Date(0) else Calendar.getInstance().apply {
            time = hasta
            add(Calendar.DAY_OF_YEAR, -periodoDias)
        }.time
}
