package dev.justinianomarcelo.lucidanapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Tiempo {
    /** "dd/MM/yyyy HH:mm" en la zona horaria del teléfono. */
    fun fechaHora(milis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "BO")).format(Date(milis))

    /** "hace 5 min", "hace 2 h", "hace 3 días". Menos de un minuto: "hace un momento". */
    fun haceCuanto(milis: Long, ahora: Long = System.currentTimeMillis()): String {
        val minutos = ((ahora - milis).coerceAtLeast(0)) / 60_000
        return when {
            minutos < 1 -> "hace un momento"
            minutos < 60 -> "hace $minutos min"
            minutos < 24 * 60 -> "hace ${minutos / 60} h"
            else -> {
                val dias = minutos / (24 * 60)
                "hace $dias ${if (dias == 1L) "día" else "días"}"
            }
        }
    }
}
