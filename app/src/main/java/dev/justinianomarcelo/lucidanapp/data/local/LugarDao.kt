package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

@Dao
interface LugarDao {

    @Insert
    suspend fun insertarTodos(lugares: List<Lugar>)

    @Update
    suspend fun actualizar(lugar: Lugar)

    @Query("SELECT * FROM lugares")
    suspend fun obtenerTodos(): List<Lugar>

    @Query("SELECT * FROM lugares WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Lugar?

    @Query("SELECT COUNT(*) FROM lugares")
    suspend fun contar(): Int

    @Query("UPDATE lugares SET promedioCalificacion = :promedio WHERE id = :lugarId")
    suspend fun actualizarPromedio(lugarId: Long, promedio: Double)
}
