package dev.justinianomarcelo.lucidanapp.ui.admin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.launch

/** Implementa DS11 (HU14): gestión de roles y permisos desde el panel de administración. */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioRepository = UsuarioRepository(AppDatabase.obtenerInstancia(application).usuarioDao())
    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())

    var uiState by mutableStateOf(AdminUiState())
        private set

    /** DS11: valida rol = ADMINISTRADOR antes de mostrar el panel. */
    fun cargarPanel() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null || usuario.rol != RolUsuario.ADMINISTRADOR) {
            uiState = uiState.copy(accesoDenegado = true, cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            negocioRepository.sincronizar()
            val usuarios = usuarioRepository.obtenerTodos()
            val negocios = negocioRepository.obtenerTodos()
            uiState = uiState.copy(usuarios = usuarios, negocios = negocios, cargando = false)
        }
    }

    fun cambiarEstadoUsuario(usuarioId: Long, activo: Boolean) {
        viewModelScope.launch {
            usuarioRepository.actualizarActivo(usuarioId, activo)
            refrescar()
        }
    }

    fun eliminarUsuario(usuarioId: Long) {
        viewModelScope.launch {
            usuarioRepository.eliminar(usuarioId)
            uiState = uiState.copy(mensaje = "Cuenta eliminada")
            refrescar()
        }
    }

    fun cambiarEstadoNegocio(negocioId: Long, estado: EstadoNegocio) {
        viewModelScope.launch {
            negocioRepository.actualizarEstado(negocioId, estado)
            refrescar()
        }
    }

    /** Al eliminar su último negocio, el dueño vuelve a rol VISITANTE (ya no administra ninguno). */
    fun eliminarNegocio(negocioId: Long) {
        viewModelScope.launch {
            val negocio = negocioRepository.obtenerNegocioPorId(negocioId)
            negocioRepository.eliminar(negocioId)
            // Un dueño puede tener varios negocios (0..*): solo vuelve a VISITANTE si no le queda ninguno.
            // Los negocios de otros teléfonos no tienen usuario local: el dueño se busca por su hash de correo.
            val dueno = negocio?.let { n -> usuarioRepository.obtenerTodos().firstOrNull { IdDueno.deCorreo(it.correo) == n.duenoId } }
            val leQuedanNegocios = negocio != null && negocioRepository.obtenerTodos().any { it.duenoId == negocio.duenoId }
            if (dueno != null && !leQuedanNegocios) usuarioRepository.actualizarRol(dueno.id, RolUsuario.VISITANTE)
            uiState = uiState.copy(mensaje = "Negocio eliminado")
            refrescar()
        }
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }

    private suspend fun refrescar() {
        uiState = uiState.copy(
            usuarios = usuarioRepository.obtenerTodos(),
            negocios = negocioRepository.obtenerTodos()
        )
    }
}
