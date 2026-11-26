package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

/** Lo que incluye un nivel de plan de publicidad (HU10). */
data class Beneficios(
    val maxFotos: Int,
    /** Insignia «Destacado», pin con estrella, primero en la lista y visible sin importar el radio. */
    val destacado: Boolean = false,
    val redesSociales: Boolean = false,
    val notificacionCercania: Boolean = false,
    val estadisticasPorPeriodo: Boolean = false,
    val comparacionRubro: Boolean = false,
    val carruselInicio: Boolean = false,
    val ofertaDelDia: Boolean = false,
    val selloVerificado: Boolean = false,
    val avisoVencimiento: Boolean = false
)

/** ÚNICO lugar donde se definen los niveles de plan, lo que da cada uno y sus constantes. */
object BeneficiosPlan {
    const val SIN_PLAN = 0
    const val SEMANAL = 1
    const val MENSUAL = 2
    const val TRIMESTRAL = 3

    /** true: los negocios de nivel 3 aparecen en el mapa aunque se filtre por otro rubro. */
    const val VISIBLE_SIN_FILTRO_RUBRO_NIVEL3 = false

    /** true: el sello «Negocio verificado» solo se ve con plan de nivel 3 vigente. */
    const val SELLO_SOLO_NIVEL3 = true

    /** Días antes del vencimiento en que se avisa al dueño. */
    const val DIAS_AVISO_VENCIMIENTO = 2

    const val MAX_CARACTERES_OFERTA = 60

    /** Fotos por nivel: sin plan, semanal, mensual, trimestral. */
    private val MAX_FOTOS = intArrayOf(2, 3, 5, 6)

    fun de(nivel: Int) = Beneficios(
        maxFotos = MAX_FOTOS[nivel.coerceIn(SIN_PLAN, TRIMESTRAL)],
        destacado = nivel >= SEMANAL,
        redesSociales = nivel >= MENSUAL,
        notificacionCercania = nivel >= MENSUAL,
        estadisticasPorPeriodo = nivel >= MENSUAL,
        comparacionRubro = nivel >= MENSUAL,
        carruselInicio = nivel >= TRIMESTRAL,
        ofertaDelDia = nivel >= TRIMESTRAL,
        selloVerificado = nivel >= TRIMESTRAL,
        avisoVencimiento = nivel >= TRIMESTRAL
    )

    /** Respaldo para planes o contrataciones sin nivel guardado (anteriores a Room 13). */
    fun nivelPorDuracion(dias: Int): Int = when {
        dias >= 90 -> TRIMESTRAL
        dias >= 30 -> MENSUAL
        dias > 0 -> SEMANAL
        else -> SIN_PLAN
    }

    /** Beneficios en texto claro, acumulados hasta [nivel]; es lo que se siembra y se muestra. */
    fun textos(nivel: Int): List<String> = buildList {
        val b = de(nivel)
        add("Hasta ${b.maxFotos} fotos en tu ficha")
        if (b.destacado) {
            add("Insignia «Destacado» y pin con estrella en el mapa")
            add("Primero en la lista de resultados")
            add("Visible en el mapa sin importar el radio de búsqueda")
        }
        if (b.redesSociales) add("Enlaces a Facebook, Instagram y TikTok")
        if (b.notificacionCercania) add("Aviso a los usuarios que pasan cerca")
        if (b.estadisticasPorPeriodo) add("Estadísticas por período (7, 30 y 90 días)")
        if (b.comparacionRubro) add("Comparación con tu rubro")
        if (b.carruselInicio) add("Carrusel «Destacados» en la pantalla de inicio")
        if (b.ofertaDelDia) add("«Oferta del día» en tu ficha y en las listas")
        if (b.selloVerificado) add("Sello «Negocio verificado» (lo otorga el administrador)")
        if (b.avisoVencimiento) add("Aviso antes de que venza tu plan")
    }

    /** Cuánto se ahorra con el trimestral frente a pagar tres meses del mensual; 0 si no hay ahorro. */
    fun ahorroTrimestral(planes: List<PlanPublicidad>): Double {
        val mensual = planes.firstOrNull { it.nivel == MENSUAL } ?: return 0.0
        val trimestral = planes.firstOrNull { it.nivel == TRIMESTRAL } ?: return 0.0
        return (mensual.precio * 3 - trimestral.precio).coerceAtLeast(0.0)
    }
}
