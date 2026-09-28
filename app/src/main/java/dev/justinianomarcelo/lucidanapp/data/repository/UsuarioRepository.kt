package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.UsuarioDao
import dev.justinianomarcelo.lucidanapp.data.model.Usuario

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    suspend fun crearUsuario(usuario: Usuario): Long = usuarioDao.insertar(usuario)

    suspend fun buscarPorCorreo(correo: String): Usuario? = usuarioDao.buscarPorCorreo(correo)

    suspend fun actualizarContrasena(correo: String, nuevoHash: String) =
        usuarioDao.actualizarContrasena(correo, nuevoHash)

    suspend fun actualizarNotificaciones(usuarioId: Long, activas: Boolean) =
        usuarioDao.actualizarNotificaciones(usuarioId, activas)
}
