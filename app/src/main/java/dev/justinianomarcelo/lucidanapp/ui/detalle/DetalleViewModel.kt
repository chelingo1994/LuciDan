package dev.justinianomarcelo.lucidanapp.ui.detalle

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.TipoEvento
import dev.justinianomarcelo.lucidanapp.data.repository.EstadisticaRepository
import dev.justinianomarcelo.lucidanapp.data.repository.FavoritoRepository
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.data.repository.ResenaRepository
import dev.justinianomarcelo.lucidanapp.location.LocationService
import dev.justinianomarcelo.lucidanapp.ruta.RutaService
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * ViewModel de la ficha de detalle. Implementa, sobre una misma pantalla,
 * los flujos DS03 (ficha del negocio), DS04 (trazado de ruta) y DS05 (registro
 * de reseña), más el registro de vistas y clics de "Cómo llegar" (DS10, HU11).
 * Las interacciones viven en DetalleScreen y comparten el mismo negocio, por lo
 * que un único ViewModel evita duplicar estado.
 */
class DetalleViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val resenaRepository = ResenaRepository(
        AppDatabase.obtenerInstancia(application).resenaDao(),
        negocioRepository
    )
    private val estadisticaRepository = EstadisticaRepository(AppDatabase.obtenerInstancia(application).estadisticaDao())
    private val favoritoRepository = FavoritoRepository(AppDatabase.obtenerInstancia(application).favoritoDao())
    private val locationService = LocationService(application)
    private val rutaService = RutaService()

    var uiState by mutableStateOf(DetalleUiState())
        private set

    private var negocioIdActual: Long = 0

    /** DS03: carga la ficha al tocar un marcador. */
    fun cargarFicha(negocioId: Long) {
        negocioIdActual = negocioId
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val negocio = negocioRepository.obtenerNegocioPorId(negocioId)
            val resenas = resenaRepository.obtenerResenasDeNegocio(negocioId)
            val usuario = SessionManager.usuarioActual.value
            val esFavorito = usuario?.let { favoritoRepository.esFavorito(it.id, negocioId) } ?: false
            uiState = uiState.copy(
                negocio = negocio,
                resenas = resenas,
                esFavorito = esFavorito,
                cargando = false
            )
            if (negocio != null) estadisticaRepository.registrarEvento(negocio.id, TipoEvento.VISTA)
            refrescarResenasDesdeNube(negocioId)
        }
    }

    /** Muestra primero lo que hay en Room y luego trae las reseñas que otros usuarios publicaron desde sus teléfonos. */
    private suspend fun refrescarResenasDesdeNube(negocioId: Long) {
        if (!resenaRepository.sincronizar(negocioId) || negocioId != negocioIdActual) return
        uiState = uiState.copy(
            negocio = negocioRepository.obtenerNegocioPorId(negocioId) ?: uiState.negocio,
            resenas = resenaRepository.obtenerResenasDeNegocio(negocioId)
        )
    }

    /** DS04: traza la ruta caminando desde la ubicación actual hasta el negocio. */
    fun trazarRuta() {
        val negocio = uiState.negocio ?: return
        viewModelScope.launch {
            uiState = uiState.copy(calculandoRuta = true, mensaje = null)
            val origen = locationService.obtenerUbicacionActual()
            if (origen == null) {
                uiState = uiState.copy(calculandoRuta = false, mensaje = "No se pudo obtener tu ubicación actual.")
                return@launch
            }
            val destino = GeoPoint(negocio.latitud, negocio.longitud)
            val resultado = rutaService.calcularRuta(GeoPoint(origen.latitud, origen.longitud), destino)
            uiState = if (resultado != null) {
                uiState.copy(calculandoRuta = false, ruta = resultado)
            } else {
                uiState.copy(
                    calculandoRuta = false,
                    mensaje = "No se pudo calcular la ruta. Verifica tu conexión a internet."
                )
            }
            if (resultado != null) estadisticaRepository.registrarEvento(negocio.id, TipoEvento.CLIC_COMO_LLEGAR)
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
            val nuevoValor = favoritoRepository.alternarFavorito(usuario.id, negocioIdActual)
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
                autorId = IdDueno.deCorreo(usuario.correo),
                negocioId = negocioIdActual,
                calificacion = uiState.calificacionSeleccionada,
                comentario = uiState.comentario.trim()
            )
            val negocioActualizado = negocioRepository.obtenerNegocioPorId(negocioIdActual)
            val resenas = resenaRepository.obtenerResenasDeNegocio(negocioIdActual)
            uiState = uiState.copy(
                negocio = negocioActualizado,
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
