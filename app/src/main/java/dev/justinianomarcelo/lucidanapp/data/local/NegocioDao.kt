package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

@Dao
interface NegocioDao {

    @Insert
    suspend fun insertar(negocio: Negocio): Long

    @Update
    suspend fun actualizar(negocio: Negocio)

    @Query("SELECT * FROM negocios WHERE usuarioId = :usuarioId LIMIT 1")
    suspend fun obtenerPorUsuario(usuarioId: Long): Negocio?
}
