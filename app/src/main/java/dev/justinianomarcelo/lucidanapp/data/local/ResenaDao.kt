package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.Resena

@Dao
interface ResenaDao {

    @Insert
    suspend fun insertar(resena: Resena): Long

    @Query("SELECT * FROM resenas WHERE lugarId = :lugarId ORDER BY fecha DESC")
    suspend fun obtenerPorLugar(lugarId: Long): List<Resena>

    @Query("SELECT AVG(calificacion) FROM resenas WHERE lugarId = :lugarId")
    suspend fun promedioPorLugar(lugarId: Long): Double?
}
