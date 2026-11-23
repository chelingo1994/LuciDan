package dev.justinianomarcelo.lucidanapp.data.local

import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NegociosSemillaTest {

    @Test
    fun parseaUnaLineaCompleta() {
        val n = NegociosSemilla.parsear(
            "Farmacia San Martín | FARMACIA_SALUD | -22.7319 | -64.3418 | Medicamentos | 70000002 | Calle Tarija | 08:00 - 22:00 | SI"
        ).single()
        assertEquals(NegociosSemilla.ID_BASE + 1, n.id)
        assertEquals("Farmacia San Martín", n.nombre)
        assertEquals(Rubro.FARMACIA_SALUD, n.categoria)
        assertEquals(-22.7319, n.latitud, 0.0)
        assertEquals("70000002", n.telefono)
        assertEquals("Calle Tarija", n.direccion)
        assertTrue(n.destacado)
        assertEquals(EstadoNegocio.PUBLICADO, n.estado)
        assertEquals(IdDueno.SEMILLA, n.duenoId)
    }

    @Test
    fun ignoraComentariosYLineasInvalidasSinMoverIds() {
        val negocios = NegociosSemilla.parsear(
            """
            # comentario
            A | OTRO | -22.7 | -64.3 | d | | dir | h | NO

            linea rota sin campos
            B | OTRO | no-es-numero | -64.3 | d | | dir | h | NO
            C | GASTRONOMIA | -22.8 | -64.4 | d | 1 | dir | h | NO
            """.trimIndent()
        )
        assertEquals(listOf("A", "C"), negocios.map { it.nombre })
        assertEquals(listOf(NegociosSemilla.ID_BASE + 1, NegociosSemilla.ID_BASE + 2), negocios.map { it.id })
        assertEquals("", negocios[0].telefono)
        assertFalse(negocios[0].destacado)
    }

    @Test
    fun rubroDesconocidoCaeEnOtro() {
        val n = NegociosSemilla.parsear("X | PANADERIA | -22.7 | -64.3 | d | | dir | h | NO").single()
        assertEquals(Rubro.OTRO, n.categoria)
    }

    @Test
    fun archivoRealDeDocsSeParseaCompleto() {
        val archivo = listOf(File("../docs/negocios_bermejo.txt"), File("docs/negocios_bermejo.txt")).first { it.exists() }
        val lineasDeDatos = archivo.readLines().count { it.isNotBlank() && !it.trim().startsWith("#") }
        val negocios = NegociosSemilla.parsear(archivo.readText())
        assertEquals(lineasDeDatos, negocios.size)
        assertTrue(negocios.all { it.latitud in -23.0..-22.5 && it.longitud in -64.6..-64.1 })
    }

    @Test
    fun fotoOpcionalSeGuardaSinExtension() {
        val negocios = NegociosSemilla.parsear(
            """
            A | OTRO | -22.7 | -64.3 | d | | dir | h | NO | aurora_2.jpg
            B | OTRO | -22.7 | -64.3 | d | | dir | h | NO | Willy
            C | OTRO | -22.7 | -64.3 | d | | dir | h | NO |
            D | OTRO | -22.7 | -64.3 | d | | dir | h | NO
            """.trimIndent()
        )
        assertEquals(listOf(listOf("aurora_2"), listOf("willy"), emptyList<String>(), emptyList<String>()), negocios.map { it.fotos })
    }

    @Test
    fun cadaFotoDelArchivoRealExisteComoRecurso() {
        val raiz = listOf(File(".."), File(".")).first { File(it, "docs/negocios_bermejo.txt").exists() }
        val negocios = NegociosSemilla.parsear(File(raiz, "docs/negocios_bermejo.txt").readText())
        val carpeta = File(raiz, "app/src/main/res/drawable-nodpi")
        for (negocio in negocios) {
            for (foto in negocio.fotos) {
                assertTrue("Falta $foto.jpg para ${negocio.nombre}", File(carpeta, "$foto.jpg").exists())
                assertTrue("Nombre de recurso inválido: $foto", Regex("[a-z0-9_]+").matches(foto))
            }
        }
        assertEquals(12, negocios.count { it.fotos.isNotEmpty() })
    }
}
