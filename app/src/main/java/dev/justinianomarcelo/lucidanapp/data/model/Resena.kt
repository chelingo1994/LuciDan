package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Reseña de un negocio. Vive en Firestore (colección `resenas`) y Room la cachea. El id lo genera la app
 * (como en [Negocio]) para no chocar con las reseñas creadas desde otros teléfonos, y el autor se
 * identifica por el hash de su correo ([autorId]) porque el id de usuario de Room es local a cada teléfono.
 */
@Entity(
    tableName = "resenas",
    foreignKeys = [
        ForeignKey(entity = Negocio::class, parentColumns = ["id"], childColumns = ["negocioId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocioId")]
)
data class Resena(
    @PrimaryKey
    val id: Long,
    val autorId: String,
    val negocioId: Long,
    val calificacion: Int,
    val comentario: String,
    val fecha: Date = Date()
)
