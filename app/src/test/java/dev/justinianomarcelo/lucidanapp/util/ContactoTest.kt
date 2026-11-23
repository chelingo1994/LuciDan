package dev.justinianomarcelo.lucidanapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactoTest {
    @Test
    fun celularBolivianoVaConCodigoDePais() {
        assertEquals("59174535332", Contacto.numeroWhatsapp("74535332"))
        assertEquals("59168823178", Contacto.numeroWhatsapp("688 23 178"))
        assertEquals("59174535332", Contacto.numeroWhatsapp("+591 74535332"))
    }

    @Test
    fun fijosYNumerosInvalidosNoUsanWhatsapp() {
        assertNull(Contacto.numeroWhatsapp("46112233"))
        assertNull(Contacto.numeroWhatsapp(""))
        assertNull(Contacto.numeroWhatsapp("7453"))
    }
}
