package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

@Dao
interface FavoritoDao {

    @Insert
    suspend fun insertar(favorito: Favorito)

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND negocioId = :negocioId")
    suspend fun eliminar(usuarioId: Long, negocioId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND negocioId = :negocioId)")
    suspend fun esFavorito(usuarioId: Long, negocioId: Long): Boolean

    @Query(
        """
        SELECT negocios.* FROM negocios
        INNER JOIN favoritos ON negocios.id = favoritos.negocioId
        WHERE favoritos.usuarioId = :usuarioId
        ORDER BY favoritos.fechaGuardado DESC
        """
    )
    suspend fun obtenerFavoritosDeUsuario(usuarioId: Long): List<Negocio>
}
