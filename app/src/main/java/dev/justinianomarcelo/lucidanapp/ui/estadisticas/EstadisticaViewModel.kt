package dev.justinianomarcelo.lucidanapp.ui.estadisticas

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.repository.EstadisticaRepository
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import kotlinx.coroutines.launch

/** Implementa DS10 (HU11): consulta del panel de estadísticas del negocio. */
class EstadisticaViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val estadisticaRepository = EstadisticaRepository(AppDatabase.obtenerInstancia(application).estadisticaDao())

    var uiState by mutableStateOf(EstadisticaUiState())
        private set

    fun cargarResumen(negocioId: Long) {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val negocio = negocioRepository.obtenerDelDueno(negocioId, usuario.correo)
            if (negocio == null) {
                uiState = uiState.copy(cargando = false)
                return@launch
            }
            uiState = uiState.copy(negocioId = negocio.id)
            actualizarConteo()
        }
    }

    fun onPeriodoSeleccionado(periodoDias: Int) {
        uiState = uiState.copy(periodoDias = periodoDias)
        actualizarConteo()
    }

    private fun actualizarConteo() {
        val negocioId = uiState.negocioId ?: return
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val resumen = estadisticaRepository.obtenerConteo(negocioId, uiState.periodoDias)
            uiState = uiState.copy(vistas = resumen.vistas, clics = resumen.clics, cargando = false)
        }
    }
}
