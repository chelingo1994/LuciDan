package dev.justinianomarcelo.lucidanapp.ui.infoutil

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.remote.ResultadoGuardado
import dev.justinianomarcelo.lucidanapp.data.repository.InfoUtilRepository
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.launch

data class EditarFronteraUiState(
    val datos: InfoFrontera = InfoFrontera(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
    val mensaje: String? = null
)

/** Formulario del administrador para `info_util/frontera`. */
class EditarFronteraViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = InfoUtilRepository(application)

    var uiState by mutableStateOf(EditarFronteraUiState(datos = repositorio.fronteraEnCache() ?: InfoFrontera()))
        private set

    init {
        viewModelScope.launch {
            val actual = repositorio.cargarFrontera().valor
            uiState = uiState.copy(datos = actual ?: uiState.datos, cargando = false)
        }
    }

    fun onCambio(transformar: (InfoFrontera) -> InfoFrontera) {
        uiState = uiState.copy(datos = transformar(uiState.datos), error = null)
    }

    fun guardar() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario?.rol != RolUsuario.ADMINISTRADOR) {
            uiState = uiState.copy(error = "Solo un administrador puede editar esta información.")
            return
        }
        val d = uiState.datos
        val campos = listOf(
            d.horarioPaso, d.horarioChalanas, d.tarifasChalanas, d.recomendaciones,
            d.telPolicia, d.telBomberos, d.telAmbulancia, d.telMigracion
        )
        if (campos.any { it.length > LARGO_MAXIMO }) {
            uiState = uiState.copy(error = "Cada campo admite hasta $LARGO_MAXIMO caracteres.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(guardando = true, error = null)
            // Un campo vacío vuelve a "Por confirmar" para que el usuario nunca vea un hueco.
            fun lleno(texto: String) = texto.trim().ifEmpty { InfoFrontera.POR_CONFIRMAR }
            val nuevo = InfoFrontera(
                horarioPaso = lleno(d.horarioPaso), horarioChalanas = lleno(d.horarioChalanas),
                tarifasChalanas = lleno(d.tarifasChalanas), recomendaciones = lleno(d.recomendaciones),
                telPolicia = lleno(d.telPolicia), telBomberos = lleno(d.telBomberos),
                telAmbulancia = lleno(d.telAmbulancia), telMigracion = lleno(d.telMigracion),
                actualizado = System.currentTimeMillis(), adminId = IdDueno.deCorreo(usuario.correo)
            )
            uiState = when (repositorio.guardarFrontera(nuevo)) {
                ResultadoGuardado.ERROR -> uiState.copy(guardando = false, error = InfoUtilViewModel.MENSAJE_ERROR_GUARDAR)
                ResultadoGuardado.PENDIENTE ->
                    uiState.copy(guardando = false, guardado = true, mensaje = InfoUtilViewModel.MENSAJE_PENDIENTE)
                ResultadoGuardado.CONFIRMADO ->
                    uiState.copy(guardando = false, guardado = true, mensaje = "Información actualizada.")
            }
        }
    }

    companion object {
        const val LARGO_MAXIMO = 1500
    }
}
