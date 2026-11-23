package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.FavoritoDao
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

class FavoritoRepository(private val favoritoDao: FavoritoDao) {

    suspend fun esFavorito(usuarioId: Long, negocioId: Long): Boolean =
        favoritoDao.esFavorito(usuarioId, negocioId)

    suspend fun alternarFavorito(usuarioId: Long, negocioId: Long): Boolean {
        return if (favoritoDao.esFavorito(usuarioId, negocioId)) {
            favoritoDao.eliminar(usuarioId, negocioId)
            false
        } else {
            favoritoDao.insertar(Favorito(usuarioId = usuarioId, negocioId = negocioId))
            true
        }
    }

    suspend fun obtenerFavoritosDeUsuario(usuarioId: Long): List<Negocio> =
        favoritoDao.obtenerFavoritosDeUsuario(usuarioId)
}
