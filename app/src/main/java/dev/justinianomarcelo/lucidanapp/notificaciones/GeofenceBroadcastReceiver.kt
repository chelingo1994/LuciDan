package dev.justinianomarcelo.lucidanapp.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recibe las transiciones de geocerca y dispara la notificación (DS08). */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError() || evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val usuario = SessionManager.usuarioActual.value
        if (usuario?.notificacionesActivas != true) return

        val lugarId = evento.triggeringGeofences?.firstOrNull()?.requestId?.toLongOrNull() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val lugar = AppDatabase.obtenerInstancia(context).lugarDao().obtenerPorId(lugarId) ?: return@launch
            NotificationService(context).notificarLugarCercano(lugar)
        }
    }
}
