package dev.justinianomarcelo.lucidanapp.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.AuthService
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.auth.Validadores
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.model.Usuario
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel de registro / inicio de sesión / recuperación de contraseña (HU12),
 * según S1_DS06_Registro_Login.puml.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioRepository = UsuarioRepository(
        AppDatabase.obtenerInstancia(application).usuarioDao()
    )
    private val authService = AuthService(usuarioRepository)

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onCorreoChange(valor: String) {
        uiState = uiState.copy(correo = valor, mensajeError = null)
    }

    fun onContrasenaChange(valor: String) {
        uiState = uiState.copy(contrasena = valor, mensajeError = null)
    }

    fun onConfirmarContrasenaChange(valor: String) {
        uiState = uiState.copy(confirmarContrasena = valor, mensajeError = null)
    }

    fun limpiarFormulario() {
        uiState = AuthUiState()
    }

    /** VM valida formato de correo y contraseña segura, cifra y crea el usuario. */
    fun registrarse(onExito: () -> Unit) {
        val correo = uiState.correo.trim()
        val contrasena = uiState.contrasena

        if (!Validadores.esCorreoValido(correo)) {
            uiState = uiState.copy(mensajeError = "Ingresa un correo electrónico válido.")
            return
        }
        if (!Validadores.esContrasenaSegura(contrasena)) {
            uiState = uiState.copy(mensajeError = Validadores.mensajeRequisitosContrasena())
            return
        }
        if (contrasena != uiState.confirmarContrasena) {
            uiState = uiState.copy(mensajeError = "Las contraseñas no coinciden.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, mensajeError = null)

            if (usuarioRepository.buscarPorCorreo(correo) != null) {
                uiState = uiState.copy(cargando = false, mensajeError = "Ya existe una cuenta con ese correo.")
                return@launch
            }

            val hash = authService.cifrarContrasena(contrasena)
            val nuevoUsuario = Usuario(
                correo = correo,
                contrasenaHash = hash,
                fechaRegistro = Date(),
                rol = RolUsuario.VISITANTE,
                notificacionesActivas = true
            )
            val id = usuarioRepository.crearUsuario(nuevoUsuario)

            SessionManager.iniciarSesion(nuevoUsuario.copy(id = id))
            uiState = uiState.copy(cargando = false)
            onExito()
        }
    }

    fun iniciarSesion(onExito: () -> Unit) {
        val correo = uiState.correo.trim()
        val contrasena = uiState.contrasena

        if (correo.isBlank() || contrasena.isBlank()) {
            uiState = uiState.copy(mensajeError = "Ingresa correo y contraseña.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, mensajeError = null)

            val usuarioAutenticado = authService.autenticar(correo, contrasena)
            if (usuarioAutenticado != null) {
                SessionManager.iniciarSesion(usuarioAutenticado)
                uiState = uiState.copy(cargando = false)
                onExito()
            } else {
                uiState = uiState.copy(cargando = false, mensajeError = "Correo o contraseña incorrectos.")
            }
        }
    }

    /** Recuperación local de contraseña (TA-12-05): no hay backend de correo aún. */
    fun recuperarContrasena(onExito: () -> Unit) {
        val correo = uiState.correo.trim()
        val nuevaContrasena = uiState.contrasena

        if (!Validadores.esCorreoValido(correo)) {
            uiState = uiState.copy(mensajeError = "Ingresa un correo electrónico válido.")
            return
        }
        if (!Validadores.esContrasenaSegura(nuevaContrasena)) {
            uiState = uiState.copy(mensajeError = Validadores.mensajeRequisitosContrasena())
            return
        }
        if (nuevaContrasena != uiState.confirmarContrasena) {
            uiState = uiState.copy(mensajeError = "Las contraseñas no coinciden.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, mensajeError = null)

            if (usuarioRepository.buscarPorCorreo(correo) == null) {
                uiState = uiState.copy(cargando = false, mensajeError = "No existe una cuenta con ese correo.")
                return@launch
            }

            val nuevoHash = authService.cifrarContrasena(nuevaContrasena)
            usuarioRepository.actualizarContrasena(correo, nuevoHash)
            uiState = uiState.copy(cargando = false)
            onExito()
        }
    }
}
