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
        ForeignKey(entity = Negocio::class, parentColumns = ["id"], childColumns = ["negocioId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["usuarioId", "negocioId"], unique = true),
        Index(value = ["negocioId"])
    ]
)
data class Favorito(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val negocioId: Long,
    val fechaGuardado: Date = Date()
)
