package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "contrataciones_publicidad",
    foreignKeys = [
        ForeignKey(entity = Negocio::class, parentColumns = ["id"], childColumns = ["negocioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlanPublicidad::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["negocioId"]), Index(value = ["planId"])]
)
data class ContratacionPublicidad(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val negocioId: Long,
    val planId: Long,
    val fechaInicio: Date,
    val fechaVencimiento: Date,
    val activo: Boolean = true
) {
    /** DS09: vigente si está activa y su vencimiento no pasó. */
    fun estaVigente(ahora: Date = Date()): Boolean = activo && fechaVencimiento.after(ahora)
}
