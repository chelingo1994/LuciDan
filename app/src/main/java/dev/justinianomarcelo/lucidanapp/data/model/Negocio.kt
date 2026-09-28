package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "negocios",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["usuarioId"], unique = true)]
)
data class Negocio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val nombre: String,
    val categoria: CategoriaLugar,
    val ubicacion: String,
    val horario: String,
    val fotos: List<String> = emptyList(),
    val estado: EstadoNegocio = EstadoNegocio.PENDIENTE,
    val destacado: Boolean = false
)
