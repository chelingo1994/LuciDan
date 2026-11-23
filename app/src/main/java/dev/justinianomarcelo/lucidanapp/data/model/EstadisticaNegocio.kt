package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "estadisticas_negocio",
    foreignKeys = [
        ForeignKey(entity = Negocio::class, parentColumns = ["id"], childColumns = ["negocioId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["negocioId"])]
)
data class EstadisticaNegocio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val negocioId: Long,
    val tipo: TipoEvento,
    val fecha: Date = Date()
)
