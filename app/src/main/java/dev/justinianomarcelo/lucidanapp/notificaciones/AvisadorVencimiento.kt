package dev.justinianomarcelo.lucidanapp.notificaciones

import android.content.Context
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.util.AvisoVencimiento

/**
 * Avisa al dueño de los planes por vencer. No hay WorkManager ni backend: se evalúa al abrir Home y
 * «Mis negocios». Guarda por qué vencimiento ya avisó para no repetirlo.
 */
class AvisadorVencimiento(private val context: Context) {

    fun revisar(negociosDelDueno: List<Negocio>) {
        val prefs = context.getSharedPreferences("avisos_plan", Context.MODE_PRIVATE)
        val servicio = NotificationService(context)
        val ahora = System.currentTimeMillis()
        negociosDelDueno.forEach { negocio ->
            val clave = "avisado_${negocio.id}"
            val dias = AvisoVencimiento.diasParaAvisar(negocio, ahora, prefs.getLong(clave, 0)) ?: return@forEach
            // Si no se pudo publicar (sin permiso) no se marca: se reintenta la próxima vez.
            if (servicio.notificarVencimientoPlan(negocio, dias)) {
                prefs.edit().putLong(clave, negocio.destacadoHasta).apply()
            }
        }
    }
}
