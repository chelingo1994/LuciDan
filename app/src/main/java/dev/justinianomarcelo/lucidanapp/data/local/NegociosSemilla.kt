package dev.justinianomarcelo.lucidanapp.data.local

import android.content.Context
import android.util.Log
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.util.IdDueno

/**
 * Negocios de ejemplo de la primera carga. La fuente editable es docs/negocios_bermejo.txt: Gradle la
 * copia a assets en cada build (tarea `copiarSemilla`). Formato, una línea por negocio:
 * `nombre | rubro | latitud | longitud | descripcion | telefono | direccion | horario | destacado [| foto]`.
 * La foto es opcional: nombre de un recurso de drawable-nodpi (con o sin extensión), que se guarda en `fotos`.
 *
 * El orden de las líneas define el id ([ID_BASE] + posición): así todos los teléfonos comparten los mismos
 * ids y Firestore no recibe duplicados. Para no mover ids, agrega los negocios nuevos al final del archivo.
 * [ID_BASE] queda por encima de los ids chicos del autoincremento antiguo (1, 2, 3...) y por debajo del
 * tope de 2^52 que usan los negocios registrados desde la app (ver NegocioRepository).
 */
object NegociosSemilla {
    const val ARCHIVO = "negocios_bermejo.txt"
    const val ID_BASE = 4_100_000_000_000_000L
    private const val CAMPOS_MINIMOS = 9
    private const val CAMPOS_MAXIMOS = 10
    private const val TAG = "NegociosSemilla"

    fun cargar(context: Context): List<Negocio> =
        try {
            context.assets.open(ARCHIVO).bufferedReader(Charsets.UTF_8).use { parsear(it.readText()) }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer $ARCHIVO", e)
            emptyList()
        }

    /** Las líneas inválidas se omiten (y se avisan en el log) sin cambiar el id de las demás. */
    fun parsear(texto: String): List<Negocio> {
        val negocios = mutableListOf<Negocio>()
        texto.lineSequence()
            .map { it.trim().trimStart('﻿') }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { linea ->
                val negocio = parsearLinea(linea, ID_BASE + negocios.size + 1L)
                if (negocio != null) negocios.add(negocio) else Log.w(TAG, "Línea omitida: $linea")
            }
        return negocios
    }

    /** `aurora_2.jpg` -> `aurora_2`: así se busca el recurso. Vacío si no hay foto. */
    private fun nombreDeRecurso(foto: String): String? =
        foto.substringBeforeLast('.').trim().lowercase().takeIf { it.isNotEmpty() }

    private fun parsearLinea(linea: String, id: Long): Negocio? {
        val c = linea.split("|").map { it.trim() }
        if (c.size !in CAMPOS_MINIMOS..CAMPOS_MAXIMOS || c[0].isEmpty()) return null
        val latitud = c[2].toDoubleOrNull() ?: return null
        val longitud = c[3].toDoubleOrNull() ?: return null
        val rubro = Rubro.deNombre(c[1]) ?: Rubro.OTRO.also { Log.w(TAG, "Rubro desconocido '${c[1]}' en '${c[0]}': se usa OTRO") }
        return Negocio(
            id = id,
            nombre = c[0],
            categoria = rubro,
            descripcion = c[4],
            telefono = c[5],
            direccion = c[6],
            latitud = latitud,
            longitud = longitud,
            horario = c[7],
            fotos = listOfNotNull(c.getOrNull(9)?.let(::nombreDeRecurso)),
            estado = EstadoNegocio.PUBLICADO,
            destacado = c[8].equals("SI", ignoreCase = true),
            duenoId = IdDueno.SEMILLA
        )
    }
}
