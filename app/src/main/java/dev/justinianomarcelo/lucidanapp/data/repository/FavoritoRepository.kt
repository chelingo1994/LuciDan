package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.FavoritoDao
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

class FavoritoRepository(private val favoritoDao: FavoritoDao) {

    suspend fun esFavorito(usuarioId: Long, lugarId: Long): Boolean =
        favoritoDao.esFavorito(usuarioId, lugarId)

    suspend fun alternarFavorito(usuarioId: Long, lugarId: Long): Boolean {
        return if (favoritoDao.esFavorito(usuarioId, lugarId)) {
            favoritoDao.eliminar(usuarioId, lugarId)
            false
        } else {
            favoritoDao.insertar(Favorito(usuarioId = usuarioId, lugarId = lugarId))
            true
        }
    }

    suspend fun obtenerFavoritosDeUsuario(usuarioId: Long): List<Lugar> =
        favoritoDao.obtenerFavoritosDeUsuario(usuarioId)
}
