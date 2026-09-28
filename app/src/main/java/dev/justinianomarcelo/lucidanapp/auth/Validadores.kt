package dev.justinianomarcelo.lucidanapp.auth

import android.util.Patterns

/**
 * Validación de formato de correo y de contraseña segura (TA-12-03).
 */
object Validadores {

    private const val LONGITUD_MINIMA_CONTRASENA = 8

    fun esCorreoValido(correo: String): Boolean =
        correo.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(correo).matches()

    fun esContrasenaSegura(contrasena: String): Boolean {
        if (contrasena.length < LONGITUD_MINIMA_CONTRASENA) return false
        val tieneMayuscula = contrasena.any { it.isUpperCase() }
        val tieneMinuscula = contrasena.any { it.isLowerCase() }
        val tieneDigito = contrasena.any { it.isDigit() }
        return tieneMayuscula && tieneMinuscula && tieneDigito
    }

    fun mensajeRequisitosContrasena(): String =
        "Mínimo $LONGITUD_MINIMA_CONTRASENA caracteres, con mayúscula, minúscula y número."
}
