package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.justinianomarcelo.lucidanapp.data.model.Resena

@Dao
interface ResenaDao {

    /** Upsert (UPDATE, no REPLACE): la misma reseña puede llegar de la nube varias veces. */
    @Upsert
    suspend fun upsertar(resenas: List<Resena>)

    @Upsert
    suspend fun upsertar(resena: Resena)

    @Query("SELECT * FROM resenas WHERE negocioId = :negocioId ORDER BY fecha DESC")
    suspend fun obtenerPorNegocio(negocioId: Long): List<Resena>

    @Query("SELECT * FROM resenas")
    suspend fun obtenerTodas(): List<Resena>

    @Query("SELECT AVG(calificacion) FROM resenas WHERE negocioId = :negocioId")
    suspend fun promedioPorNegocio(negocioId: Long): Double?

    @Query("SELECT id FROM negocios")
    suspend fun idsDeNegocios(): List<Long>
}
