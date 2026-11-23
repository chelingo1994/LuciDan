package dev.justinianomarcelo.lucidanapp.ui.admin

import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Usuario

data class AdminUiState(
    val usuarios: List<Usuario> = emptyList(),
    val negocios: List<Negocio> = emptyList(),
    val accesoDenegado: Boolean = false,
    val cargando: Boolean = true,
    val mensaje: String? = null
)
