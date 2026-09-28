package dev.justinianomarcelo.lucidanapp.ui.auth

data class AuthUiState(
    val correo: String = "",
    val contrasena: String = "",
    val confirmarContrasena: String = "",
    val cargando: Boolean = false,
    val mensajeError: String? = null
)
