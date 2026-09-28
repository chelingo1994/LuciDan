package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.NegocioDao
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

class NegocioRepository(private val negocioDao: NegocioDao) {

    suspend fun obtenerPorUsuario(usuarioId: Long): Negocio? = negocioDao.obtenerPorUsuario(usuarioId)

    /** DS07: registra el negocio en estado Pendiente. */
    suspend fun registrarNegocio(negocio: Negocio): Long = negocioDao.insertar(negocio)

    /** HU09: guarda cambios del perfil y los refleja de inmediato. */
    suspend fun actualizarNegocio(negocio: Negocio) = negocioDao.actualizar(negocio)
}
