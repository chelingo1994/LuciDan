package dev.justinianomarcelo.lucidanapp.ui.estadisticas

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.repository.EstadisticaRepository
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.util.Destacados
import kotlinx.coroutines.launch

/** Implementa DS10 (HU11): consulta del panel de estadísticas del negocio. */
class EstadisticaViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val estadisticaRepository = EstadisticaRepository(AppDatabase.obtenerInstancia(application).estadisticaDao())

    var uiState by mutableStateOf(EstadisticaUiState())
        private set

    /** Rubro del negocio consultado, solo si su plan incluye la comparación. */
    private var rubroAComparar: Rubro? = null

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
            rubroAComparar = negocio.categoria.takeIf { Destacados.beneficios(negocio).comparacionRubro }
            uiState = uiState.copy(
                rubro = negocio.categoria.etiqueta,
                negocioId = negocio.id,
                porPeriodo = Destacados.beneficios(negocio).estadisticasPorPeriodo
            )
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
            val periodo = uiState.periodoDias.takeIf { uiState.porPeriodo }
            val resumen = estadisticaRepository.obtenerConteo(negocioId, periodo)
            val promedio = rubroAComparar?.let { estadisticaRepository.obtenerPromedioRubro(negocioId, it, periodo) }
            uiState = uiState.copy(vistas = resumen.vistas, clics = resumen.clics, promedioRubro = promedio, cargando = false)
        }
    }
}
