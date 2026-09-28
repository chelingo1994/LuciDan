package dev.justinianomarcelo.lucidanapp.ui.detalle

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.repository.FavoritoRepository
import dev.justinianomarcelo.lucidanapp.data.repository.LugarRepository
import dev.justinianomarcelo.lucidanapp.data.repository.ResenaRepository
import dev.justinianomarcelo.lucidanapp.location.LocationService
import dev.justinianomarcelo.lucidanapp.ruta.RutaService
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * ViewModel de la ficha de detalle. Implementa, sobre una misma pantalla,
 * los flujos DS03 (ficha), DS04 (trazado de ruta) y DS05 (registro de
 * reseña) — las tres interacciones viven en DetalleScreen y comparten el
 * mismo lugar, por lo que un único ViewModel evita duplicar estado.
 */
class DetalleViewModel(application: Application) : AndroidViewModel(application) {

    private val lugarRepository = LugarRepository(AppDatabase.obtenerInstancia(application).lugarDao())
    private val resenaRepository = ResenaRepository(
        AppDatabase.obtenerInstancia(application).resenaDao(),
        lugarRepository
    )
    private val favoritoRepository = FavoritoRepository(AppDatabase.obtenerInstancia(application).favoritoDao())
    private val locationService = LocationService(application)
    private val rutaService = RutaService()

    var uiState by mutableStateOf(DetalleUiState())
        private set

    private var lugarIdActual: Long = 0

    /** DS03: carga la ficha al tocar un marcador. */
    fun cargarFicha(lugarId: Long) {
        lugarIdActual = lugarId
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val lugar = lugarRepository.obtenerLugarPorId(lugarId)
            val resenas = resenaRepository.obtenerResenasDeLugar(lugarId)
            val usuario = SessionManager.usuarioActual.value
            val esFavorito = usuario?.let { favoritoRepository.esFavorito(it.id, lugarId) } ?: false
            uiState = uiState.copy(
                lugar = lugar,
                resenas = resenas,
                esFavorito = esFavorito,
                cargando = false
            )
        }
    }

    /** DS04: traza la ruta caminando desde la ubicación actual hasta el lugar. */
    fun trazarRuta() {
        val lugar = uiState.lugar ?: return
        viewModelScope.launch {
            uiState = uiState.copy(calculandoRuta = true, mensaje = null)
            val origen = locationService.obtenerUbicacionActual()
            if (origen == null) {
                uiState = uiState.copy(calculandoRuta = false, mensaje = "No se pudo obtener tu ubicación actual.")
                return@launch
            }
            val destino = GeoPoint(lugar.latitud, lugar.longitud)
            val resultado = rutaService.calcularRuta(GeoPoint(origen.latitud, origen.longitud), destino)
            uiState = if (resultado != null) {
                uiState.copy(calculandoRuta = false, ruta = resultado)
            } else {
                uiState.copy(
                    calculandoRuta = false,
                    mensaje = "No se pudo calcular la ruta. Verifica tu conexión a internet."
                )
            }
        }
    }

    /** TA-07-01/03: alterna el favorito, exige sesión iniciada. */
    fun alternarFavorito() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(requiereSesion = true)
            return
        }
        viewModelScope.launch {
            val nuevoValor = favoritoRepository.alternarFavorito(usuario.id, lugarIdActual)
            uiState = uiState.copy(esFavorito = nuevoValor)
        }
    }

    fun onCalificacionSeleccionada(valor: Int) {
        uiState = uiState.copy(calificacionSeleccionada = valor)
    }

    fun onComentarioChange(texto: String) {
        uiState = uiState.copy(comentario = texto)
    }

    /** DS05: valida sesión iniciada antes de permitir calificar. */
    fun enviarResena() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(requiereSesion = true)
            return
        }
        if (uiState.calificacionSeleccionada !in 1..5) {
            uiState = uiState.copy(mensaje = "Selecciona una calificación de 1 a 5 estrellas.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(enviandoResena = true)
            resenaRepository.guardarResena(
                usuarioId = usuario.id,
                lugarId = lugarIdActual,
                calificacion = uiState.calificacionSeleccionada,
                comentario = uiState.comentario.trim()
            )
            val lugarActualizado = lugarRepository.obtenerLugarPorId(lugarIdActual)
            val resenas = resenaRepository.obtenerResenasDeLugar(lugarIdActual)
            uiState = uiState.copy(
                lugar = lugarActualizado,
                resenas = resenas,
                calificacionSeleccionada = 0,
                comentario = "",
                enviandoResena = false,
                mensaje = "¡Gracias por tu reseña!"
            )
        }
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }

    fun limpiarRequiereSesion() {
        uiState = uiState.copy(requiereSesion = false)
    }
}
