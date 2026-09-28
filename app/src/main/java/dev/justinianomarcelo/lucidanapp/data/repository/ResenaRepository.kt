package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.ResenaDao
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import java.util.Date

class ResenaRepository(
    private val resenaDao: ResenaDao,
    private val lugarRepository: LugarRepository
) {

    suspend fun obtenerResenasDeLugar(lugarId: Long): List<Resena> = resenaDao.obtenerPorLugar(lugarId)

    /** DS05: guarda la reseña y recalcula el promedio del lugar. */
    suspend fun guardarResena(usuarioId: Long, lugarId: Long, calificacion: Int, comentario: String): Resena {
        val resena = Resena(
            usuarioId = usuarioId,
            lugarId = lugarId,
            calificacion = calificacion,
            comentario = comentario,
            fecha = Date()
        )
        val id = resenaDao.insertar(resena)
        val promedio = resenaDao.promedioPorLugar(lugarId) ?: calificacion.toDouble()
        lugarRepository.recalcularPromedio(lugarId, promedio)
        return resena.copy(id = id)
    }
}
