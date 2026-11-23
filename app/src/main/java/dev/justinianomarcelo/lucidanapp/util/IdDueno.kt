package dev.justinianomarcelo.lucidanapp.util

import java.security.MessageDigest

/**
 * Identifica al dueño de un negocio entre dispositivos sin publicar su correo en Firestore:
 * se guarda el SHA-256 del correo normalizado (sin espacios y en minúsculas).
 */
object IdDueno {
    /** Dueño de los negocios de ejemplo (docs/negocios_bermejo.txt): no pertenecen a ninguna cuenta. */
    const val SEMILLA = "semilla"

    fun deCorreo(correo: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(correo.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
}
