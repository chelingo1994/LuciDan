package dev.justinianomarcelo.lucidanapp.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.justinianomarcelo.lucidanapp.MainActivity
import dev.justinianomarcelo.lucidanapp.R
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

/**
 * Publica la notificación local de cercanía (DS08).
 *
 * El despacho vía Firebase Cloud Messaging requiere el proyecto Firebase
 * propio de Marcelo (google-services.json), aún no configurado — ver
 * CLAUDE.md sección 8. Esta clase implementa el tramo on-device: cuando
 * [dev.justinianomarcelo.lucidanapp.notificaciones.GeofenceManager] detecta
 * que el usuario entró al radio de un negocio destacado, aquí se construye y
 * muestra la notificación local que cumple el mismo rol descrito en el DS08.
 */
class NotificationService(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun notificarNegocioCercano(negocio: Negocio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        crearCanalSiNecesario()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_NEGOCIO_ID, negocio.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            negocio.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Tienes un negocio destacado cerca")
            .setContentText(negocio.nombre)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(negocio.id.toInt(), notificacion)
    }

    private fun crearCanalSiNecesario() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            CANAL_ID,
            "Negocios cercanos",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de negocios destacados cerca de tu ubicación"
        }
        manager.createNotificationChannel(canal)
    }

    companion object {
        const val CANAL_ID = "lucidan_cercania"
    }
}
