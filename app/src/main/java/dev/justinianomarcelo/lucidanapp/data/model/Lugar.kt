package dev.justinianomarcelo.lucidanapp.data.model

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lugares")
data class Lugar(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val categoria: CategoriaLugar,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    val horario: String,
    val rangoPrecio: String,
    val fotos: List<String> = emptyList(),
    val promedioCalificacion: Double = 0.0,
    val destacado: Boolean = false
) {
    /** Distancia en metros desde [latOrigen]/[lngOrigen] hasta este lugar. */
    fun calcularDistancia(latOrigen: Double, lngOrigen: Double): Double {
        val resultado = FloatArray(1)
        Location.distanceBetween(latOrigen, lngOrigen, latitud, longitud, resultado)
        return resultado[0].toDouble()
    }
}
