package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "favoritos",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lugar::class, parentColumns = ["id"], childColumns = ["lugarId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["usuarioId", "lugarId"], unique = true),
        Index(value = ["lugarId"])
    ]
)
data class Favorito(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val lugarId: Long,
    val fechaGuardado: Date = Date()
)
