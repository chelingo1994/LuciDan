package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

@Dao
interface FavoritoDao {

    @Insert
    suspend fun insertar(favorito: Favorito)

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND lugarId = :lugarId")
    suspend fun eliminar(usuarioId: Long, lugarId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND lugarId = :lugarId)")
    suspend fun esFavorito(usuarioId: Long, lugarId: Long): Boolean

    @Query(
        """
        SELECT lugares.* FROM lugares
        INNER JOIN favoritos ON lugares.id = favoritos.lugarId
        WHERE favoritos.usuarioId = :usuarioId
        ORDER BY favoritos.fechaGuardado DESC
        """
    )
    suspend fun obtenerFavoritosDeUsuario(usuarioId: Long): List<Lugar>
}
