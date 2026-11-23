package dev.justinianomarcelo.lucidanapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimaCodigosTest {

    @Test
    fun codigosComunes_tienenDescripcionEnEspanol() {
        assertEquals("Despejado", ClimaCodigos.deCodigo(0).descripcion)
        assertEquals("Nublado", ClimaCodigos.deCodigo(3).descripcion)
        assertEquals("Niebla", ClimaCodigos.deCodigo(45).descripcion)
        assertEquals("Lluvia", ClimaCodigos.deCodigo(63).descripcion)
        assertEquals("Chubascos ligeros", ClimaCodigos.deCodigo(80).descripcion)
        assertEquals("Tormenta eléctrica", ClimaCodigos.deCodigo(95).descripcion)
    }

    @Test
    fun codigosAgrupados_compartenIcono() {
        assertEquals(IconoClima.SOL, ClimaCodigos.deCodigo(1).icono)
        assertEquals(IconoClima.PARCIAL, ClimaCodigos.deCodigo(2).icono)
        listOf(51, 61, 65, 80, 82).forEach { assertEquals(IconoClima.LLUVIA, ClimaCodigos.deCodigo(it).icono) }
        listOf(95, 96, 99).forEach { assertEquals(IconoClima.TORMENTA, ClimaCodigos.deCodigo(it).icono) }
        listOf(71, 75, 85).forEach { assertEquals(IconoClima.NIEVE, ClimaCodigos.deCodigo(it).icono) }
    }

    @Test
    fun codigoDesconocidoONulo_noFalla() {
        assertEquals(IconoClima.DESCONOCIDO, ClimaCodigos.deCodigo(null).icono)
        assertEquals(IconoClima.DESCONOCIDO, ClimaCodigos.deCodigo(1234).icono)
        assertEquals("Condición desconocida", ClimaCodigos.deCodigo(-1).descripcion)
    }
}
