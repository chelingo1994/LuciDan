package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.LugarDao
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

class LugarRepository(private val lugarDao: LugarDao) {

    /** DS01 / DS02: lugares dentro de [radioMetros] desde el punto dado. */
    suspend fun obtenerLugaresCercanos(
        lat: Double,
        lng: Double,
        radioMetros: Double,
        categoria: CategoriaLugar? = null
    ): List<Lugar> {
        return lugarDao.obtenerTodos()
            .filter { it.calcularDistancia(lat, lng) <= radioMetros }
            .filter { categoria == null || it.categoria == categoria }
            .sortedBy { it.calcularDistancia(lat, lng) }
    }

    suspend fun obtenerLugarPorId(id: Long): Lugar? = lugarDao.obtenerPorId(id)

    /** DS08: lugares destacados dentro del radio, para notificaciones de cercanía. */
    suspend fun buscarDestacadosCercanos(lat: Double, lng: Double, radioMetros: Double): List<Lugar> {
        return lugarDao.obtenerTodos()
            .filter { it.destacado && it.calcularDistancia(lat, lng) <= radioMetros }
    }

    suspend fun recalcularPromedio(lugarId: Long, promedio: Double) {
        lugarDao.actualizarPromedio(lugarId, promedio)
    }
}
