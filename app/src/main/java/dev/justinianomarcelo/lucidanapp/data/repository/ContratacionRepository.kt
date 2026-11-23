package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.ContratacionDao
import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import java.util.Calendar
import java.util.Date

/** DS09 — Contratación de un plan de publicidad (HU10). */
class ContratacionRepository(
    private val contratacionDao: ContratacionDao,
    private val negocioRepository: NegocioRepository
) {

    suspend fun obtenerActivaPorNegocio(negocioId: Long): ContratacionPublicidad? =
        contratacionDao.obtenerActivaPorNegocio(negocioId)

    suspend fun obtenerHistorialPorNegocio(negocioId: Long): List<ContratacionPublicidad> =
        contratacionDao.obtenerHistorialPorNegocio(negocioId)

    /** DS09: registra la contratación, calcula el vencimiento y marca el negocio como destacado. */
    suspend fun registrarContratacion(negocioId: Long, planId: Long, duracionDias: Int): ContratacionPublicidad {
        val fechaInicio = Date()
        val fechaVencimiento = Calendar.getInstance().apply {
            time = fechaInicio
            add(Calendar.DAY_OF_YEAR, duracionDias)
        }.time
        val contratacion = ContratacionPublicidad(
            negocioId = negocioId,
            planId = planId,
            fechaInicio = fechaInicio,
            fechaVencimiento = fechaVencimiento,
            activo = true
        )
        val id = contratacionDao.insertar(contratacion)
        negocioRepository.actualizarEstadoDestacado(negocioId, true)
        return contratacion.copy(id = id)
    }

    /**
     * Sustituye al "proceso programado" de DS09 (no hay backend con cron): se ejecuta cada
     * vez que se abre Home (DS01) y desactiva las contrataciones vencidas, quitando el
     * destacado a sus negocios.
     */
    suspend fun desactivarVencidas() {
        val vencidas = contratacionDao.obtenerVencidas(Date())
        vencidas.forEach { contratacion ->
            contratacionDao.desactivar(contratacion.id)
            negocioRepository.actualizarEstadoDestacado(contratacion.negocioId, false)
        }
    }
}
