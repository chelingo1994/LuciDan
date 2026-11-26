package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DestacadosTest {

    private val ahora = 1_000_000L

    private fun negocio(
        id: Long = 1,
        nivel: Int = 0,
        hasta: Long = ahora + 1,
        estado: EstadoNegocio = EstadoNegocio.PUBLICADO,
        rubro: Rubro = Rubro.GASTRONOMIA
    ) = Negocio(
        id = id, nombre = "N$id", categoria = rubro, descripcion = "", telefono = "", direccion = "",
        latitud = 0.0, longitud = 0.0, horario = "", estado = estado, planNivel = nivel, destacadoHasta = hasta
    )

    private fun visible(n: Negocio, distancia: Double, categoria: Rubro? = null, sinRubro: Boolean = false) =
        Destacados.esVisibleEnMapa(n, distancia, 1000.0, categoria, ahora, sinRubro)

    @Test
    fun vigenciaSegunElVencimiento() {
        assertEquals(2, Destacados.nivelVigente(negocio(nivel = 2), ahora))
        assertEquals(0, Destacados.nivelVigente(negocio(nivel = 2, hasta = ahora), ahora))
        assertEquals(0, Destacados.nivelVigente(negocio(nivel = 3, hasta = ahora - 1), ahora))
        // El booleano antiguo, sin nivel ni fecha, ya no destaca.
        assertEquals(0, Destacados.nivelVigente(negocio(hasta = 0).copy(destacado = true), ahora))
    }

    @Test
    fun sinPlanSeRespetaElRadio() {
        assertTrue(visible(negocio(), 900.0))
        assertFalse(visible(negocio(), 1500.0))
    }

    @Test
    fun destacadoVigenteIgnoraElRadioPeroNoElVencido() {
        assertTrue(visible(negocio(nivel = 1), 9000.0))
        assertFalse(visible(negocio(nivel = 1, hasta = ahora - 1), 9000.0))
    }

    @Test
    fun soloLosPublicadosSeVen() {
        assertFalse(visible(negocio(nivel = 3, estado = EstadoNegocio.PENDIENTE), 10.0))
        assertFalse(visible(negocio(nivel = 3, estado = EstadoNegocio.SUSPENDIDO), 10.0))
    }

    @Test
    fun elFiltroDeRubroSeRespetaSalvoQueLaConstanteLoAbraAlNivel3() {
        assertFalse(visible(negocio(nivel = 3), 10.0, Rubro.TRANSPORTE))
        assertTrue(visible(negocio(nivel = 3), 9000.0, Rubro.TRANSPORTE, sinRubro = true))
        assertFalse(visible(negocio(nivel = 2), 10.0, Rubro.TRANSPORTE, sinRubro = true))
        assertTrue(visible(negocio(), 10.0, Rubro.GASTRONOMIA))
    }

    @Test
    fun destacadosPrimeroYLuegoPorCercania() {
        val distancias = mapOf(1L to 100.0, 2L to 800.0, 3L to 5000.0, 4L to 50.0)
        val lista = listOf(
            negocio(1), negocio(2, nivel = 1), negocio(3, nivel = 3), negocio(4, nivel = 3, hasta = ahora - 1)
        )
        val orden = Destacados.ordenar(lista, ahora) { distancias.getValue(it.id) }.map { it.id }
        assertEquals(listOf(2L, 3L, 4L, 1L), orden)
    }

    @Test
    fun mejorPlanPrefiereNivelYLuegoVencimientoIgnorandoVencidos() {
        val planes = listOf(
            PlanVigente(1, ahora + 50), PlanVigente(2, ahora + 10), PlanVigente(2, ahora + 20), PlanVigente(3, ahora - 1)
        )
        assertEquals(PlanVigente(2, ahora + 20), Destacados.mejorPlan(planes, ahora))
        assertNull(Destacados.mejorPlan(listOf(PlanVigente(3, ahora)), ahora))
    }
}
