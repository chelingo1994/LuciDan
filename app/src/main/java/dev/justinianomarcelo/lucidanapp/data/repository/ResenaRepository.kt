package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.ResenaDao
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.data.remote.NubeService
import java.util.Date
import kotlin.random.Random

/**
 * Las reseñas viajan a Firestore para que todos los usuarios vean las de cada negocio; Room es la caché
 * local (la app lee siempre de ahí y funciona sin internet). El promedio de cada negocio se calcula en
 * el teléfono a partir de las reseñas que tiene.
 */
class ResenaRepository(
    private val resenaDao: ResenaDao,
    private val negocioRepository: NegocioRepository,
    private val nube: NubeService = NubeService()
) {

    suspend fun obtenerResenasDeNegocio(negocioId: Long): List<Resena> = resenaDao.obtenerPorNegocio(negocioId)

    /** DS05: guarda la reseña (Room + Firestore) y recalcula el promedio del negocio. */
    suspend fun guardarResena(autorId: String, negocioId: Long, calificacion: Int, comentario: String): Resena {
        val resena = Resena(
            id = Random.nextLong(1, MAX_ID),
            autorId = autorId,
            negocioId = negocioId,
            calificacion = calificacion,
            comentario = comentario,
            fecha = Date()
        )
        resenaDao.upsertar(resena)
        nube.guardarResena(resena)
        recalcularPromedio(negocioId)
        return resena
    }

    /**
     * Baja las reseñas de Firestore a Room (todas, o solo las de [negocioId]) y recalcula los promedios de
     * los negocios afectados. Las reseñas no se editan ni se borran, así que lo que Room tenga y la nube no
     * (reseñas de antes de usar la nube, o subidas que fallaron sin red) se sube. Devuelve true si la
     * respuesta vino del servidor.
     */
    suspend fun sincronizar(negocioId: Long? = null): Boolean {
        val instantanea = nube.obtenerResenas(negocioId) ?: return false
        if (instantanea.desdeServidor) {
            val idsRemotos = instantanea.datos.map { it.id }.toSet()
            resenaDao.obtenerTodas()
                .filter { (negocioId == null || it.negocioId == negocioId) && it.id !in idsRemotos }
                .forEach { nube.guardarResena(it) }
        }
        // Una reseña cuyo negocio aún no se bajó (o ya no existe) rompería la FK: se toma en la próxima sincronización.
        val negociosLocales = resenaDao.idsDeNegocios().toSet()
        val validas = instantanea.datos.filter { it.negocioId in negociosLocales }
        resenaDao.upsertar(validas)
        validas.map { it.negocioId }.distinct().forEach { recalcularPromedio(it) }
        return instantanea.desdeServidor
    }

    private suspend fun recalcularPromedio(negocioId: Long) {
        resenaDao.promedioPorNegocio(negocioId)?.let { negocioRepository.recalcularPromedio(negocioId, it) }
    }

    private companion object {
        /** Tope de 2^52 para que el id viaje sin pérdida como número y como texto. */
        const val MAX_ID = 1L shl 52
    }
}
