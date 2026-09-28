package dev.justinianomarcelo.lucidanapp.notificaciones

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

/**
 * Registra geocercas alrededor de los lugares destacados para disparar la
 * detección de cercanía descrita en DS08 (GeofenceManager).
 */
class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registrarGeocercas(lugares: List<Lugar>, radioMetros: Float = 300f) {
        if (lugares.isEmpty()) return
        val geocercas = lugares.map { lugar ->
            Geofence.Builder()
                .setRequestId(lugar.id.toString())
                .setCircularRegion(lugar.latitud, lugar.longitud, radioMetros)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
        val solicitud = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geocercas)
            .build()

        geofencingClient.addGeofences(solicitud, pendingIntent)
    }

    fun quitarGeocercas() {
        geofencingClient.removeGeofences(pendingIntent)
    }
}
