package dev.justinianomarcelo.lucidanapp.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cálculos del tipo de cambio Bs ↔ ARS (HU16). Todas las tasas se expresan en **ARS por 1 Bs**:
 *  - `compra`: ARS que se reciben por cada Bs que se entrega (sentido Bs → ARS).
 *  - `venta`: ARS que cuesta cada Bs que se recibe (sentido ARS → Bs).
 * La casa de cambio gana en la diferencia, así que siempre `compra <= venta`.
 */
object Cambio {

    enum class Sentido { BS_A_ARS, ARS_A_BS }

    /** Convierte [monto] en el [sentido] dado. Bs→ARS multiplica por la compra; ARS→Bs divide por la venta. */
    fun convertir(monto: Double, sentido: Sentido, compra: Double, venta: Double): Double {
        if (monto <= 0.0) return 0.0
        val resultado = when (sentido) {
            Sentido.BS_A_ARS -> monto * compra
            Sentido.ARS_A_BS -> if (venta > 0.0) monto / venta else 0.0
        }
        return redondear(resultado)
    }

    /** Redondeo comercial a 2 decimales (0,005 sube). */
    fun redondear(valor: Double): Double =
        if (valor.isNaN() || valor.isInfinite()) 0.0 else BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** Acepta coma o punto decimal; null si no es un número finito. */
    fun parsearNumero(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }

    /**
     * Valida lo que escribe el administrador. Devuelve el mensaje de error, o null si es válido:
     * ambos números positivos y `compra <= venta`.
     */
    fun validarTasa(compra: String, venta: String): String? {
        val c = parsearNumero(compra) ?: return "La tasa de compra debe ser un número."
        val v = parsearNumero(venta) ?: return "La tasa de venta debe ser un número."
        if (c <= 0.0 || v <= 0.0) return "Las tasas deben ser mayores que cero."
        if (c > TASA_MAXIMA || v > TASA_MAXIMA) return "Las tasas no pueden superar $TASA_MAXIMA ARS por Bs."
        if (c > v) return "La tasa de compra no puede ser mayor que la de venta."
        return null
    }

    /**
     * Cruce ARS por 1 Bs a partir de las cotizaciones del USD (open.er-api.com): `ARS/USD ÷ BOB/USD`.
     * Null si falta algún dato o no es positivo.
     */
    fun cruceArsPorBs(arsPorUsd: Double?, bobPorUsd: Double?): Double? {
        if (arsPorUsd == null || bobPorUsd == null) return null
        if (arsPorUsd <= 0.0 || bobPorUsd <= 0.0 || arsPorUsd.isNaN() || bobPorUsd.isNaN()) return null
        return arsPorUsd / bobPorUsd
    }

    private const val TASA_MAXIMA = 100_000.0
}
