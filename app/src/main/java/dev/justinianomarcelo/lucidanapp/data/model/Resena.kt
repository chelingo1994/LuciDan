package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "resenas",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lugar::class, parentColumns = ["id"], childColumns = ["lugarId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("usuarioId"), Index("lugarId")]
)
data class Resena(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val lugarId: Long,
    val calificacion: Int,
    val comentario: String,
    val fecha: Date = Date()
)
