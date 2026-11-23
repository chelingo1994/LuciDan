package dev.justinianomarcelo.lucidanapp.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusquedaTest {

    @Test
    fun consultaVaciaCoincideConTodo() {
        assertTrue(Busqueda.coincide("", "Plaza Principal"))
        assertTrue(Busqueda.coincide("   ", "Plaza Principal"))
    }

    @Test
    fun ignoraMayusculasYTildes() {
        assertTrue(Busqueda.coincide("cafe", "Café del Puente"))
        assertTrue(Busqueda.coincide("GASTRONOMIA", "Gastronomía"))
    }

    @Test
    fun todasLasPalabrasDebenAparecerEnAlgunCampo() {
        assertTrue(Busqueda.coincide("pan bermejo", "Panadería", "Vende pan fresco en Bermejo"))
        assertFalse(Busqueda.coincide("pan tarija", "Panadería", "Vende pan fresco en Bermejo"))
    }

    @Test
    fun buscaEnLaDescripcion() {
        assertTrue(Busqueda.coincide("repuestos", "Casa Pérez", "Venta de repuestos para auto"))
        assertFalse(Busqueda.coincide("farmacia", "Casa Pérez", "Venta de repuestos para auto"))
    }
}
