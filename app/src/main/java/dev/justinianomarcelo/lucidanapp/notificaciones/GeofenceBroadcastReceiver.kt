package dev.justinianomarcelo.lucidanapp.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.util.Destacados
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

        val ids = evento.triggeringGeofences?.mapNotNull { it.requestId.toLongOrNull() }.orEmpty()

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.obtenerInstancia(context).negocioDao()
            // La geocerca no vence sola: el plan se vuelve a comprobar al momento de avisar.
            val negocio = ids.firstNotNullOfOrNull { id ->
                dao.obtenerPorId(id)?.takeIf {
                    it.estado == EstadoNegocio.PUBLICADO && Destacados.beneficios(it).notificacionCercania
                }
            } ?: return@launch
            NotificationService(context).notificarNegocioCercano(negocio)
        }
    }
}
