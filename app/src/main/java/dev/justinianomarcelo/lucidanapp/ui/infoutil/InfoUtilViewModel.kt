package dev.justinianomarcelo.lucidanapp.ui.infoutil

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.model.Clima
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.ReferenciaCambio
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.model.TasaCambio
import dev.justinianomarcelo.lucidanapp.data.remote.ResultadoGuardado
import dev.justinianomarcelo.lucidanapp.data.repository.InfoUtilRepository
import dev.justinianomarcelo.lucidanapp.util.Cambio
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.launch

/** De dónde sale la tasa que usa la calculadora. */
enum class OrigenTasa { LUCIDAN, REFERENCIA }

data class TasaAplicable(val compra: Double, val venta: Double, val origen: OrigenTasa)

data class InfoUtilUiState(
    val cambio: TasaCambio? = null,
    val cambioDesdeCache: Boolean = false,
    val referencia: ReferenciaCambio? = null,
    val clima: Clima? = null,
    val climaDesdeCache: Boolean = false,
    val frontera: InfoFrontera? = null,
    val fronteraDesdeCache: Boolean = false,
    val cargandoCambio: Boolean = false,
    val cargandoClima: Boolean = false,
    val cargandoFrontera: Boolean = false,
    val monto: String = "",
    val sentido: Cambio.Sentido = Cambio.Sentido.BS_A_ARS,
    val dialogoTasaAbierto: Boolean = false,
    val guardandoTasa: Boolean = false,
    val errorTasa: String? = null,
    val mensaje: String? = null
) {
    /** La tasa de LuciDan manda; si el admin aún no cargó ninguna se usa la referencia internacional (una sola tasa). */
    val tasaAplicable: TasaAplicable?
        get() = cambio?.let { TasaAplicable(it.compraArsPorBs, it.ventaArsPorBs, OrigenTasa.LUCIDAN) }
            ?: referencia?.let { TasaAplicable(it.arsPorBs, it.arsPorBs, OrigenTasa.REFERENCIA) }

    /** Resultado de la calculadora, o null si no hay monto válido o no hay ninguna tasa. */
    val resultado: Double?
        get() {
            val tasa = tasaAplicable ?: return null
            val monto = Cambio.parsearNumero(monto) ?: return null
            return Cambio.convertir(monto, sentido, tasa.compra, tasa.venta)
        }
}

class InfoUtilViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = InfoUtilRepository(application)

    // Lo último guardado se ve al instante, también sin internet; luego se actualiza con la red.
    var uiState by mutableStateOf(
        InfoUtilUiState(
            cambio = repositorio.cambioEnCache(),
            referencia = repositorio.referenciaEnCache(),
            clima = repositorio.climaEnCache(),
            frontera = repositorio.fronteraEnCache()
        )
    )
        private set

    private val esAdministrador get() = SessionManager.usuarioActual.value?.rol == RolUsuario.ADMINISTRADOR

    /** Al abrir: cambio, referencia y frontera siempre; el clima solo si el dato guardado tiene más de 30 min. */
    fun cargar(forzarClima: Boolean = false) {
        cargarCambio()
        cargarClima(forzarClima)
        cargarFrontera()
    }

    fun actualizarTodo() = cargar(forzarClima = true)

    fun cargarClima(forzar: Boolean) {
        if (uiState.cargandoClima) return
        viewModelScope.launch {
            uiState = uiState.copy(cargandoClima = true)
            val r = repositorio.cargarClima(forzar)
            uiState = uiState.copy(cargandoClima = false, clima = r.valor, climaDesdeCache = r.desdeCache)
        }
    }

    private fun cargarCambio() {
        if (uiState.cargandoCambio) return
        viewModelScope.launch {
            uiState = uiState.copy(cargandoCambio = true)
            val tasa = repositorio.cargarCambio()
            val referencia = repositorio.cargarReferencia()
            uiState = uiState.copy(
                cargandoCambio = false,
                cambio = tasa.valor,
                cambioDesdeCache = tasa.desdeCache,
                referencia = referencia.valor
            )
        }
    }

    private fun cargarFrontera() {
        if (uiState.cargandoFrontera) return
        viewModelScope.launch {
            uiState = uiState.copy(cargandoFrontera = true)
            val r = repositorio.cargarFrontera()
            uiState = uiState.copy(cargandoFrontera = false, frontera = r.valor, fronteraDesdeCache = r.desdeCache)
        }
    }

    // --- Calculadora ---------------------------------------------------------------------------------------

    fun onMontoCambiado(texto: String) {
        // Solo dígitos y un separador decimal: evita montos con letras o signos.
        if (texto.all { it.isDigit() || it == '.' || it == ',' } && texto.count { it == '.' || it == ',' } <= 1) {
            uiState = uiState.copy(monto = texto)
        }
    }

    fun invertirSentido() {
        uiState = uiState.copy(
            sentido = if (uiState.sentido == Cambio.Sentido.BS_A_ARS) Cambio.Sentido.ARS_A_BS else Cambio.Sentido.BS_A_ARS
        )
    }

    // --- Edición del administrador ----------------------------------------------------------------------

    fun abrirDialogoTasa() {
        if (esAdministrador) uiState = uiState.copy(dialogoTasaAbierto = true, errorTasa = null)
    }

    fun cerrarDialogoTasa() {
        uiState = uiState.copy(dialogoTasaAbierto = false, errorTasa = null)
    }

    fun guardarTasa(compra: String, venta: String) {
        val usuario = SessionManager.usuarioActual.value
        if (usuario?.rol != RolUsuario.ADMINISTRADOR) return
        val error = Cambio.validarTasa(compra, venta)
        if (error != null) {
            uiState = uiState.copy(errorTasa = error)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(guardandoTasa = true, errorTasa = null)
            val tasa = TasaCambio(
                compraArsPorBs = Cambio.parsearNumero(compra)!!,
                ventaArsPorBs = Cambio.parsearNumero(venta)!!,
                actualizado = System.currentTimeMillis(),
                adminId = IdDueno.deCorreo(usuario.correo)
            )
            when (repositorio.guardarCambio(tasa)) {
                ResultadoGuardado.ERROR ->
                    uiState = uiState.copy(guardandoTasa = false, errorTasa = MENSAJE_ERROR_GUARDAR)
                ResultadoGuardado.PENDIENTE ->
                    uiState = uiState.copy(
                        guardandoTasa = false, dialogoTasaAbierto = false, cambio = tasa, mensaje = MENSAJE_PENDIENTE
                    )
                ResultadoGuardado.CONFIRMADO ->
                    uiState = uiState.copy(
                        guardandoTasa = false, dialogoTasaAbierto = false, cambio = tasa,
                        cambioDesdeCache = false, mensaje = "Tasa actualizada."
                    )
            }
        }
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }

    companion object {
        const val MENSAJE_ERROR_GUARDAR = "No se pudo guardar. Revisa que las reglas de Firestore estén publicadas."
        const val MENSAJE_PENDIENTE = "Sin conexión: se guardó en este teléfono y se publicará al volver internet."
    }
}
