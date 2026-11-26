package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro

/** Plan que corresponde a un negocio: nivel y vencimiento (epoch ms). */
data class PlanVigente(val nivel: Int, val hasta: Long)

/**
 * Reglas de los negocios destacados como funciones puras. La vigencia se calcula SIEMPRE al leer: un plan
 * vencido no destaca aunque nadie haya actualizado el negocio.
 */
object Destacados {

    fun nivelVigente(negocio: Negocio, ahora: Long = System.currentTimeMillis()): Int =
        if (negocio.planNivel >= BeneficiosPlan.SEMANAL && negocio.destacadoHasta > ahora) negocio.planNivel
        else BeneficiosPlan.SIN_PLAN

    fun beneficios(negocio: Negocio, ahora: Long = System.currentTimeMillis()): Beneficios =
        BeneficiosPlan.de(nivelVigente(negocio, ahora))

    /** El mejor plan sin vencer: mayor nivel y, a igual nivel, el que vence más tarde. */
    fun mejorPlan(planes: List<PlanVigente>, ahora: Long): PlanVigente? =
        planes.filter { it.nivel >= BeneficiosPlan.SEMANAL && it.hasta > ahora }
            .maxWithOrNull(compareBy<PlanVigente> { it.nivel }.thenBy { it.hasta })

    /**
     * Visibilidad en el mapa (HU01/HU02): solo PUBLICADO; el rubro se respeta (salvo nivel 3 con
     * [nivel3SinFiltroRubro]) y el radio no se aplica a los destacados vigentes.
     */
    fun esVisibleEnMapa(
        negocio: Negocio,
        distanciaMetros: Double,
        radioMetros: Double,
        categoria: Rubro?,
        ahora: Long,
        nivel3SinFiltroRubro: Boolean = BeneficiosPlan.VISIBLE_SIN_FILTRO_RUBRO_NIVEL3
    ): Boolean {
        if (negocio.estado != EstadoNegocio.PUBLICADO) return false
        val nivel = nivelVigente(negocio, ahora)
        val rubroOk = categoria == null || negocio.categoria == categoria ||
            (nivel3SinFiltroRubro && nivel >= BeneficiosPlan.TRIMESTRAL)
        return rubroOk && (BeneficiosPlan.de(nivel).destacado || distanciaMetros <= radioMetros)
    }

    /** Destacados vigentes primero; dentro de cada grupo, por cercanía. */
    fun ordenar(negocios: List<Negocio>, ahora: Long, distancia: (Negocio) -> Double): List<Negocio> =
        negocios.sortedWith(
            compareByDescending<Negocio> { beneficios(it, ahora).destacado }.thenBy { distancia(it) }
        )
}
