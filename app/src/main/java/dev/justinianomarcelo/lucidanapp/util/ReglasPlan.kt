package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import java.util.Calendar
import java.util.TimeZone

/*
 * Reglas puras de los beneficios de plan. Todas comprueban la vigencia AL LEER (ver Destacados.nivelVigente):
 * un plan vencido no muestra beneficios aunque nadie haya actualizado el negocio.
 */

object LimiteFotos {
    fun maximo(negocio: Negocio?, ahora: Long = System.currentTimeMillis()): Int =
        negocio?.let { Destacados.beneficios(it, ahora).maxFotos } ?: BeneficiosPlan.de(BeneficiosPlan.SIN_PLAN).maxFotos

    /** Las fotos que se muestran; las que sobran se conservan guardadas. */
    fun visibles(negocio: Negocio, ahora: Long = System.currentTimeMillis()): List<String> =
        negocio.fotos.take(maximo(negocio, ahora))

    fun mensaje(maximo: Int) = "Tu plan permite hasta $maximo fotos. Mejora tu plan para agregar más"
}

enum class VigenciaOferta(val etiqueta: String) {
    HOY("Hasta hoy 23:59"),
    VEINTICUATRO_HORAS("24 horas");

    fun hasta(ahora: Long, zona: TimeZone = TimeZone.getDefault()): Long = when (this) {
        VEINTICUATRO_HORAS -> ahora + Ofertas.DIA_MS
        HOY -> Calendar.getInstance(zona).apply {
            timeInMillis = ahora
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}

object Ofertas {
    const val DIA_MS = 24 * 60 * 60 * 1000L

    /** Texto de la oferta que se debe mostrar, o null (sin plan de nivel 3, vacía o vencida). */
    fun vigente(negocio: Negocio, ahora: Long = System.currentTimeMillis()): String? =
        negocio.ofertaTexto.takeIf {
            it.isNotBlank() && negocio.ofertaHasta > ahora && Destacados.beneficios(negocio, ahora).ofertaDelDia
        }
}

object Sello {
    fun visible(
        negocio: Negocio,
        ahora: Long = System.currentTimeMillis(),
        soloNivel3: Boolean = BeneficiosPlan.SELLO_SOLO_NIVEL3
    ): Boolean = negocio.verificado && (!soloNivel3 || Destacados.beneficios(negocio, ahora).selloVerificado)
}

object AvisoVencimiento {
    /**
     * Días que faltan (1 o 2) si hay que avisar ahora, o null. [avisadoHasta] es el vencimiento por el que
     * ya se avisó: cada período de plan genera un solo aviso.
     */
    fun diasParaAvisar(negocio: Negocio, ahora: Long, avisadoHasta: Long): Int? {
        if (!Destacados.beneficios(negocio, ahora).avisoVencimiento) return null
        val restante = negocio.destacadoHasta - ahora
        if (restante > BeneficiosPlan.DIAS_AVISO_VENCIMIENTO * Ofertas.DIA_MS) return null
        if (avisadoHasta == negocio.destacadoHasta) return null
        return ((restante + Ofertas.DIA_MS - 1) / Ofertas.DIA_MS).toInt()
    }

    fun texto(dias: Int) = "Tu plan vence en $dias ${if (dias == 1) "día" else "días"}, renueva"
}

object ComparacionRubro {
    /** Promedio por negocio de los otros del rubro; null si no hay otros con quién comparar. */
    fun promedio(total: Int, otrosNegocios: Int): Double? =
        if (otrosNegocios > 0) total.toDouble() / otrosNegocios else null
}
