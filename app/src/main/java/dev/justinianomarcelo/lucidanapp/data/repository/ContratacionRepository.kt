package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.ContratacionDao
import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import dev.justinianomarcelo.lucidanapp.util.BeneficiosPlan
import dev.justinianomarcelo.lucidanapp.util.Destacados
import dev.justinianomarcelo.lucidanapp.util.PlanVigente
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

    /** DS09: registra la contratación, calcula el vencimiento y aplica el plan efectivo al negocio. */
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
        aplicarPlanes()
        return contratacion.copy(id = id)
    }

    /**
     * Sustituye al "proceso programado" de DS09 (no hay backend con cron): se ejecuta cada
     * vez que se abre Home (DS01) y desactiva las contrataciones vencidas.
     */
    suspend fun desactivarVencidas() {
        val vencidas = contratacionDao.obtenerVencidas(Date())
        vencidas.forEach { contratacionDao.desactivar(it.id) }
        aplicarPlanes(vencidas.map { it.negocioId })
    }

    /**
     * Pone en cada negocio el mejor plan vigente contratado EN ESTE TELÉFONO. Nunca baja un plan vigente que
     * venga de la nube (pudo contratarse en otro teléfono); [recienVencidos] sin plan alguno se limpian.
     */
    suspend fun aplicarPlanes(recienVencidos: List<Long> = emptyList()) {
        val ahora = System.currentTimeMillis()
        val porNegocio = contratacionDao.obtenerActivasConNivel().groupBy { it.negocioId }
        (porNegocio.keys + recienVencidos).forEach { negocioId ->
            val negocio = negocioRepository.obtenerNegocioPorId(negocioId) ?: return@forEach
            val local = Destacados.mejorPlan(
                porNegocio[negocioId].orEmpty().map {
                    val nivel = if (it.nivel > 0) it.nivel else BeneficiosPlan.nivelPorDuracion(it.duracionDias)
                    PlanVigente(nivel, it.fechaVencimiento)
                },
                ahora
            )
            val nivelActual = Destacados.nivelVigente(negocio, ahora)
            val mejora = local != null &&
                (local.nivel > nivelActual || (local.nivel == nivelActual && local.hasta > negocio.destacadoHasta))
            if (local != null && mejora) {
                negocioRepository.actualizarPlan(negocioId, local.nivel, local.hasta)
            } else if (local == null && nivelActual == 0 && negocioId in recienVencidos) {
                negocioRepository.actualizarPlan(negocioId, 0, 0)
            }
        }
    }
}
