package dev.justinianomarcelo.lucidanapp.ui.planes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.repository.ContratacionRepository
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.data.repository.PlanPublicidadRepository
import kotlinx.coroutines.launch

/** Implementa DS09 (HU10): selección y contratación de un plan de publicidad. */
class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val planPublicidadRepository =
        PlanPublicidadRepository(AppDatabase.obtenerInstancia(application).planPublicidadDao())
    private val contratacionRepository = ContratacionRepository(
        AppDatabase.obtenerInstancia(application).contratacionDao(),
        negocioRepository
    )

    var uiState by mutableStateOf(PlanesUiState())
        private set

    fun cargarPlanes(negocioId: Long) {
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
            val planes = planPublicidadRepository.obtenerPlanes()
            val contratacionActiva = contratacionRepository.obtenerActivaPorNegocio(negocio.id)
            uiState = uiState.copy(
                negocioId = negocio.id,
                planes = planes,
                contratacionActiva = contratacionActiva,
                cargando = false
            )
        }
    }

    /** DS09: valida datos y registra la contratación con estado Pendiente -> Destacado activo. */
    fun contratarPlan(planId: Long) {
        val negocioId = uiState.negocioId ?: return
        viewModelScope.launch {
            uiState = uiState.copy(contratando = true)
            val plan = uiState.planes.firstOrNull { it.id == planId } ?: return@launch
            val contratacion = contratacionRepository.registrarContratacion(negocioId, planId, plan.duracionDias)
            uiState = uiState.copy(
                contratando = false,
                contratacionActiva = contratacion,
                mensaje = "Negocio destacado activo"
            )
        }
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }
}
