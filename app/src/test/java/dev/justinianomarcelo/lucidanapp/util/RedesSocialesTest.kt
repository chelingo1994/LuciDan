package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RedesSocialesTest {

    @Test
    fun usuariosYEnlacesSeNormalizan() {
        listOf("mitienda", "@mitienda", " instagram.com/mitienda ", "https://www.instagram.com/mitienda/?hl=es").forEach {
            assertEquals(it, "https://www.instagram.com/mitienda", RedSocial.INSTAGRAM.normalizar(it))
        }
        assertEquals("https://www.facebook.com/mi.tienda", RedSocial.FACEBOOK.normalizar("http://m.facebook.com/mi.tienda"))
        assertEquals(
            "https://www.facebook.com/profile.php?id=1000123",
            RedSocial.FACEBOOK.normalizar("https://www.facebook.com/profile.php?id=1000123&ref=x")
        )
        assertEquals("https://www.tiktok.com/@mi_tienda", RedSocial.TIKTOK.normalizar("tiktok.com/@mi_tienda"))
        assertEquals("", RedSocial.TIKTOK.normalizar("   "))
    }

    @Test
    fun loInvalidoSeRechaza() {
        listOf(
            "https://sitio-malo.com/mitienda", "instagram.com.malo.com/x", "mi tienda", "javascript:alert(1)",
            "facebook.com", "https://instagram.com/", "..."
        ).forEach { assertNull(it, RedSocial.INSTAGRAM.normalizar(it)) }
    }

    @Test
    fun soloSeMuestranConPlanMensualOSuperiorVigente() {
        val n = Negocio(
            id = 1, nombre = "N", categoria = Rubro.OTRO, descripcion = "", telefono = "", direccion = "",
            latitud = 0.0, longitud = 0.0, horario = "", planNivel = 2, destacadoHasta = 100,
            facebook = "https://www.facebook.com/tienda", instagram = "https://malo.com/x"
        )
        assertEquals(listOf(RedSocial.FACEBOOK to "https://www.facebook.com/tienda"), n.redesVisibles(50))
        assertEquals(0, n.redesVisibles(100).size)
        assertEquals(0, n.copy(planNivel = 1).redesVisibles(50).size)
    }
}
