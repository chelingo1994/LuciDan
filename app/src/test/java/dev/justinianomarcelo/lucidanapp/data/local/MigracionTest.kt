package dev.justinianomarcelo.lucidanapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.DriverManager

/** Ejecuta sobre SQLite real el mismo SQL de la migración 13 -> 14 y comprueba que no se pierde nada. */
class MigracionTest {

    private val tablaVersion13 =
        "CREATE TABLE `negocios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, " +
            "`nombre` TEXT NOT NULL, `categoria` TEXT NOT NULL, `descripcion` TEXT NOT NULL, " +
            "`telefono` TEXT NOT NULL, `direccion` TEXT NOT NULL, `latitud` REAL NOT NULL, " +
            "`longitud` REAL NOT NULL, `horario` TEXT NOT NULL, `fotos` TEXT NOT NULL, " +
            "`promedioCalificacion` REAL NOT NULL, `estado` TEXT NOT NULL, `destacado` INTEGER NOT NULL, " +
            "`duenoId` TEXT NOT NULL, `planNivel` INTEGER NOT NULL DEFAULT 0, " +
            "`destacadoHasta` INTEGER NOT NULL DEFAULT 0)"

    @Test
    fun migracion13a14ConservaLosDatos() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conexion ->
            conexion.createStatement().use { sql ->
                sql.execute(tablaVersion13)
                sql.execute(
                    "INSERT INTO negocios VALUES (7, 1, 'Panadería', 'GASTRONOMIA', 'pan', '71234567', 'Centro', " +
                        "-22.7, -64.3, '8-18', 'a|b|c', 4.5, 'PUBLICADO', 1, 'abc', 3, 999)"
                )

                SQL_MIGRACION_13_14.forEach { sql.execute(it) }

                sql.executeQuery("SELECT * FROM negocios").use { fila ->
                    assertEquals(true, fila.next())
                    assertEquals(7L, fila.getLong("id"))
                    assertEquals("Panadería", fila.getString("nombre"))
                    assertEquals("a|b|c", fila.getString("fotos"))
                    assertEquals(4.5, fila.getDouble("promedioCalificacion"), 0.001)
                    assertEquals(3, fila.getInt("planNivel"))
                    assertEquals(999L, fila.getLong("destacadoHasta"))
                    listOf("facebook", "instagram", "tiktok", "ofertaTexto").forEach {
                        assertEquals(it, "", fila.getString(it))
                    }
                    assertEquals(0L, fila.getLong("ofertaHasta"))
                    assertEquals(0, fila.getInt("verificado"))
                    assertEquals(false, fila.next())
                }
            }
        }
    }
}
