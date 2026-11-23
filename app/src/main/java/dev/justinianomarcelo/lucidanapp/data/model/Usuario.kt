package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val correo: String,
    val contrasenaHash: String,
    val fechaRegistro: Date = Date(),
    val rol: RolUsuario = RolUsuario.VISITANTE,
    val notificacionesActivas: Boolean = true,
    val activo: Boolean = true
)
