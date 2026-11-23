package dev.justinianomarcelo.lucidanapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoTest {
    private val ahora = 10_000_000_000L
    private fun hace(minutos: Long) = ahora - minutos * 60_000

    @Test
    fun haceCuanto_usaMinutosHorasYDias() {
        assertEquals("hace un momento", Tiempo.haceCuanto(hace(0), ahora))
        assertEquals("hace 5 min", Tiempo.haceCuanto(hace(5), ahora))
        assertEquals("hace 2 h", Tiempo.haceCuanto(hace(125), ahora))
        assertEquals("hace 1 día", Tiempo.haceCuanto(hace(24 * 60), ahora))
        assertEquals("hace 3 días", Tiempo.haceCuanto(hace(3 * 24 * 60), ahora))
    }

    @Test
    fun haceCuanto_futuroSeTrataComoAhora() {
        assertEquals("hace un momento", Tiempo.haceCuanto(ahora + 60_000, ahora))
    }
}
