package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
