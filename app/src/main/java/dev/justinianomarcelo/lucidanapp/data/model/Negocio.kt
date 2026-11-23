package dev.justinianomarcelo.lucidanapp.data.model

import android.location.Location
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Los negocios viven en Firestore y Room los guarda como caché. Como las cuentas son locales de cada
 * teléfono, [usuarioId] solo vale en el dispositivo que lo creó; el dueño se identifica entre
 * dispositivos con [duenoId]: un usuario puede poseer varios negocios (0..*, diagrama de clases) (hash del correo, ver [dev.justinianomarcelo.lucidanapp.util.IdDueno]).
 */
@Entity(tableName = "negocios", indices = [Index(value = ["duenoId"])])
data class Negocio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long = 0,
    val nombre: String,
    val categoria: Rubro,
    val descripcion: String,
    val telefono: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val horario: String,
    val fotos: List<String> = emptyList(),
    /** Promedio de las reseñas locales (HU06); no viaja a Firestore. */
    val promedioCalificacion: Double = 0.0,
    val estado: EstadoNegocio = EstadoNegocio.PENDIENTE,
    val destacado: Boolean = false,
    val duenoId: String = ""
) {
    /** Distancia en metros desde [latOrigen]/[lngOrigen] hasta este negocio. */
    fun calcularDistancia(latOrigen: Double, lngOrigen: Double): Double {
        val resultado = FloatArray(1)
        Location.distanceBetween(latOrigen, lngOrigen, latitud, longitud, resultado)
        return resultado[0].toDouble()
    }
}
