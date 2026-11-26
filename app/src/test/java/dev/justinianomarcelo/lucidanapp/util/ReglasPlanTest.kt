package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReglasPlanTest {

    private val dia = Ofertas.DIA_MS
    private val ahora = 100 * dia

    private fun negocio(nivel: Int = 0, hasta: Long = ahora + 10 * dia) = Negocio(
        id = 1, nombre = "N", categoria = Rubro.GASTRONOMIA, descripcion = "", telefono = "", direccion = "",
        latitud = 0.0, longitud = 0.0, horario = "", fotos = (1..6).map { "f$it" },
        planNivel = nivel, destacadoHasta = hasta
    )

    @Test
    fun limiteDeFotosPorNivelSinBorrarNinguna() {
        assertEquals(listOf(2, 3, 5, 6), (0..3).map { LimiteFotos.maximo(negocio(it), ahora) })
        assertEquals(2, LimiteFotos.maximo(null, ahora))
        val vencido = negocio(3, hasta = ahora - 1)
        assertEquals(listOf("f1", "f2"), LimiteFotos.visibles(vencido, ahora))
        assertEquals(6, vencido.fotos.size)
        assertEquals("Tu plan permite hasta 3 fotos. Mejora tu plan para agregar más", LimiteFotos.mensaje(3))
    }

    @Test
    fun ofertaVigenteSoloConNivel3YAntesDeVencer() {
        val n = negocio(3).copy(ofertaTexto = "2x1", ofertaHasta = ahora + 1)
        assertEquals("2x1", Ofertas.vigente(n, ahora))
        assertNull(Ofertas.vigente(n, ahora + 1))
        assertNull(Ofertas.vigente(n.copy(planNivel = 2), ahora))
        assertNull(Ofertas.vigente(n.copy(destacadoHasta = ahora), ahora))
        assertNull(Ofertas.vigente(n.copy(ofertaTexto = " "), ahora))
    }

    @Test
    fun vigenciaDeLaOferta() {
        val zona = TimeZone.getTimeZone("America/La_Paz")
        val tarde = Calendar.getInstance(zona).apply { clear(); set(2026, Calendar.NOVEMBER, 25, 18, 30) }.timeInMillis
        val finDelDia = Calendar.getInstance(zona).apply { clear(); set(2026, Calendar.NOVEMBER, 25, 23, 59, 59) }.timeInMillis
        assertEquals(finDelDia + 999, VigenciaOferta.HOY.hasta(tarde, zona))
        assertEquals(tarde + dia, VigenciaOferta.VEINTICUATRO_HORAS.hasta(tarde, zona))
    }

    @Test
    fun elSelloExigeVerificacionYNivel3SalvoQueLaConstanteCambie() {
        val verificado = negocio(3).copy(verificado = true)
        assertTrue(Sello.visible(verificado, ahora))
        assertFalse(Sello.visible(negocio(3), ahora))
        assertFalse(Sello.visible(verificado.copy(planNivel = 2), ahora))
        assertFalse(Sello.visible(verificado.copy(destacadoHasta = ahora - 1), ahora))
        assertTrue(Sello.visible(verificado.copy(planNivel = 0), ahora, soloNivel3 = false))
    }

    @Test
    fun avisoDeVencimientoUnaVezPorPeriodo() {
        val n = negocio(3, hasta = ahora + 2 * dia)
        assertEquals(2, AvisoVencimiento.diasParaAvisar(n, ahora, 0))
        assertEquals(1, AvisoVencimiento.diasParaAvisar(n, ahora + dia, 0))
        assertNull(AvisoVencimiento.diasParaAvisar(n, ahora, n.destacadoHasta))
        assertNull(AvisoVencimiento.diasParaAvisar(negocio(3, hasta = ahora + 2 * dia + 1), ahora, 0))
        assertNull(AvisoVencimiento.diasParaAvisar(n, n.destacadoHasta, 0))
        assertNull(AvisoVencimiento.diasParaAvisar(n.copy(planNivel = 2), ahora, 0))
        assertEquals("Tu plan vence en 2 días, renueva", AvisoVencimiento.texto(2))
    }

    @Test
    fun promedioDelRubro() {
        assertEquals(2.5, ComparacionRubro.promedio(5, 2)!!, 0.001)
        assertNull(ComparacionRubro.promedio(0, 0))
    }

    @Test
    fun whatsappConMensajeListo() {
        assertEquals(
            "https://wa.me/59171234567?text=Hola%2C%20vi%20su%20negocio%20en%20LuciDan",
            Contacto.enlaceWhatsapp("712 34567")
        )
        assertNull(Contacto.enlaceWhatsapp("4612345"))
    }

    @Test
    fun conVariasFotosElDocumentoSeReparte() {
        assertEquals(600_000, FotoNegocio.maximoTextoPorFoto(1))
        assertEquals(150_000, FotoNegocio.maximoTextoPorFoto(6))
        assertTrue((1..6).all { FotoNegocio.maximoTextoPorFoto(it) * it <= 900_000 })
    }
}
