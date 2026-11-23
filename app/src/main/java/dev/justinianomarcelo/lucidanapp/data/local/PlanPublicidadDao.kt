package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

@Dao
interface PlanPublicidadDao {

    @Insert
    suspend fun insertarTodos(planes: List<PlanPublicidad>)

    @Query("SELECT * FROM planes_publicidad")
    suspend fun obtenerTodos(): List<PlanPublicidad>

    @Query("SELECT * FROM planes_publicidad WHERE id = :id")
    suspend fun obtenerPorId(id: Long): PlanPublicidad?

    @Query("SELECT COUNT(*) FROM planes_publicidad")
    suspend fun contar(): Int
}
