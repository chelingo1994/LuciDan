package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.local.PlanesSemilla
import org.junit.Assert.assertEquals
import org.junit.Test

class BeneficiosPlanTest {

    @Test
    fun cadaNivelSumaBeneficios() {
        val mensual = Beneficios(
            maxFotos = 5, destacado = true, redesSociales = true, notificacionCercania = true,
            estadisticasPorPeriodo = true, comparacionRubro = true
        )
        assertEquals(Beneficios(maxFotos = 2), BeneficiosPlan.de(0))
        assertEquals(Beneficios(maxFotos = 3, destacado = true), BeneficiosPlan.de(1))
        assertEquals(mensual, BeneficiosPlan.de(2))
        assertEquals(
            mensual.copy(
                maxFotos = 6, carruselInicio = true, ofertaDelDia = true, selloVerificado = true, avisoVencimiento = true
            ),
            BeneficiosPlan.de(3)
        )
    }

    @Test
    fun laSemillaTieneUnPlanPorNivel() {
        assertEquals(listOf(1, 2, 3), PlanesSemilla.lista.map { it.nivel })
        assertEquals(listOf(4, 8, 12), PlanesSemilla.lista.map { it.beneficios.lines().size })
        PlanesSemilla.lista.forEach { assertEquals(it.nivel, BeneficiosPlan.nivelPorDuracion(it.duracionDias)) }
    }

    @Test
    fun ahorroDelTrimestralFrenteATresMensuales() {
        assertEquals(50.0, BeneficiosPlan.ahorroTrimestral(PlanesSemilla.lista), 0.001)
        assertEquals(0.0, BeneficiosPlan.ahorroTrimestral(emptyList()), 0.001)
    }
}
