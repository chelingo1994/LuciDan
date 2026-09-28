package dev.justinianomarcelo.lucidanapp.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine

data class Coordenada(val latitud: Double, val longitud: Double)

/**
 * Obtiene la ubicación actual del dispositivo (DS01). El llamador es
 * responsable de haber solicitado y obtenido el permiso de ubicación antes
 * de invocar [obtenerUbicacionActual].
 */
class LocationService(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Coordenada? = suspendCancellableCoroutine { continuacion ->
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                continuacion.resumeWith(
                    Result.success(location?.let { Coordenada(it.latitude, it.longitude) })
                )
            }
            .addOnFailureListener {
                continuacion.resumeWith(Result.success(null))
            }
    }
}
