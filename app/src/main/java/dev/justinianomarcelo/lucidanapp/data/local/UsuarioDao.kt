package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.model.Usuario

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): Usuario?

    @Query("UPDATE usuarios SET contrasenaHash = :nuevoHash WHERE correo = :correo")
    suspend fun actualizarContrasena(correo: String, nuevoHash: String)

    @Query("UPDATE usuarios SET notificacionesActivas = :activas WHERE id = :usuarioId")
    suspend fun actualizarNotificaciones(usuarioId: Long, activas: Boolean)

    /** DS11 (HU14): panel de administración. */
    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodos(): List<Usuario>

    @Query("UPDATE usuarios SET activo = :activo WHERE id = :usuarioId")
    suspend fun actualizarActivo(usuarioId: Long, activo: Boolean)

    /** HU08: el dueño de un negocio pasa de VISITANTE a NEGOCIO al registrarlo. */
    @Query("UPDATE usuarios SET rol = :rol WHERE id = :usuarioId")
    suspend fun actualizarRol(usuarioId: Long, rol: RolUsuario)

    @Query("DELETE FROM usuarios WHERE id = :usuarioId")
    suspend fun eliminar(usuarioId: Long)
}
