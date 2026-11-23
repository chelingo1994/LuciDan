package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.UsuarioDao
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.model.Usuario

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    suspend fun crearUsuario(usuario: Usuario): Long = usuarioDao.insertar(usuario)

    suspend fun buscarPorCorreo(correo: String): Usuario? = usuarioDao.buscarPorCorreo(correo)

    suspend fun actualizarContrasena(correo: String, nuevoHash: String) =
        usuarioDao.actualizarContrasena(correo, nuevoHash)

    suspend fun actualizarNotificaciones(usuarioId: Long, activas: Boolean) =
        usuarioDao.actualizarNotificaciones(usuarioId, activas)

    /** DS11 (HU14): panel de administración. */
    suspend fun obtenerTodos(): List<Usuario> = usuarioDao.obtenerTodos()

    suspend fun actualizarActivo(usuarioId: Long, activo: Boolean) = usuarioDao.actualizarActivo(usuarioId, activo)

    suspend fun eliminar(usuarioId: Long) = usuarioDao.eliminar(usuarioId)

    /** HU08/HU14: el dueño de un negocio pasa a rol NEGOCIO; vuelve a VISITANTE si se elimina. */
    suspend fun actualizarRol(usuarioId: Long, rol: RolUsuario) = usuarioDao.actualizarRol(usuarioId, rol)
}
