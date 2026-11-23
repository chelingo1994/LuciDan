package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.util.Cambio.Sentido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CambioTest {

    // Compra 130 ARS por Bs (lo que se recibe), venta 140 ARS por Bs (lo que cuesta).
    private val compra = 130.0
    private val venta = 140.0

    @Test
    fun bsAArs_multiplicaPorLaCompra() {
        assertEquals(1300.0, Cambio.convertir(10.0, Sentido.BS_A_ARS, compra, venta), 0.0)
    }

    @Test
    fun arsABs_dividePorLaVenta() {
        assertEquals(10.0, Cambio.convertir(1400.0, Sentido.ARS_A_BS, compra, venta), 0.0)
    }

    @Test
    fun ida_y_vuelta_nuncaDaMasQueElMontoOriginal() {
        // Con compra <= venta la casa gana: cambiar y volver a cambiar no puede dar más.
        val ars = Cambio.convertir(100.0, Sentido.BS_A_ARS, compra, venta)
        assertEquals(true, Cambio.convertir(ars, Sentido.ARS_A_BS, compra, venta) <= 100.0)
    }

    @Test
    fun redondeaA2Decimales_haciaArribaEnLaMitad() {
        assertEquals(0.01, Cambio.convertir(1.0, Sentido.ARS_A_BS, 1.0, 100.0 - 0.0), 0.0) // 1/100 = 0,01 exacto
        assertEquals(0.67, Cambio.convertir(2.0, Sentido.ARS_A_BS, 1.0, 3.0), 0.0) // 0,666... -> 0,67
        assertEquals(1.01, Cambio.redondear(1.005), 0.0) // 0,005 sube
        assertEquals(2.35, Cambio.convertir(1.0, Sentido.BS_A_ARS, 2.345, 3.0), 0.0)
    }

    @Test
    fun montoCeroNegativoOTasaCero_dan_cero() {
        assertEquals(0.0, Cambio.convertir(0.0, Sentido.BS_A_ARS, compra, venta), 0.0)
        assertEquals(0.0, Cambio.convertir(0.0, Sentido.ARS_A_BS, compra, venta), 0.0)
        assertEquals(0.0, Cambio.convertir(-5.0, Sentido.BS_A_ARS, compra, venta), 0.0)
        assertEquals(0.0, Cambio.convertir(10.0, Sentido.ARS_A_BS, compra, 0.0), 0.0)
    }

    @Test
    fun parsearNumero_aceptaComaYPunto_yRechazaBasura() {
        assertEquals(12.5, Cambio.parsearNumero("12,5")!!, 0.0)
        assertEquals(12.5, Cambio.parsearNumero(" 12.5 ")!!, 0.0)
        assertNull(Cambio.parsearNumero(""))
        assertNull(Cambio.parsearNumero("abc"))
        assertNull(Cambio.parsearNumero("NaN"))
        assertNull(Cambio.parsearNumero("Infinity"))
    }

    @Test
    fun validarTasa_aceptaCompraMenorOIgualQueVenta() {
        assertNull(Cambio.validarTasa("130", "140"))
        assertNull(Cambio.validarTasa("135,5", "135.5"))
    }

    @Test
    fun validarTasa_rechazaCompraMayorQueVenta() {
        assertNotNull(Cambio.validarTasa("141", "140"))
    }

    @Test
    fun validarTasa_rechazaVaciosCerosNegativosYTextos() {
        assertNotNull(Cambio.validarTasa("", "140"))
        assertNotNull(Cambio.validarTasa("130", ""))
        assertNotNull(Cambio.validarTasa("0", "140"))
        assertNotNull(Cambio.validarTasa("-1", "140"))
        assertNotNull(Cambio.validarTasa("130", "uno"))
        assertNotNull(Cambio.validarTasa("130", "1000000"))
    }

    @Test
    fun cruce_esArsPorUsdEntreBobPorUsd() {
        // Valores reales del 19/11/2026 en open.er-api.com.
        assertEquals(133.2229, Cambio.cruceArsPorBs(1558.2427, 11.696505)!!, 0.0001)
        assertEquals(2.0, Cambio.cruceArsPorBs(10.0, 5.0)!!, 0.0)
    }

    @Test
    fun cruce_sinDatosOInvalidos_esNulo() {
        assertNull(Cambio.cruceArsPorBs(null, 6.96))
        assertNull(Cambio.cruceArsPorBs(1000.0, null))
        assertNull(Cambio.cruceArsPorBs(1000.0, 0.0))
        assertNull(Cambio.cruceArsPorBs(-1.0, 6.96))
        assertNull(Cambio.cruceArsPorBs(Double.NaN, 6.96))
    }
}
