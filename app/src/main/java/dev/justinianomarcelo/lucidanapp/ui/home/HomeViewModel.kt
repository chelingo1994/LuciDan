package dev.justinianomarcelo.lucidanapp.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.repository.LugarRepository
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import dev.justinianomarcelo.lucidanapp.location.Coordenada
import dev.justinianomarcelo.lucidanapp.location.LocationService
import dev.justinianomarcelo.lucidanapp.notificaciones.GeofenceManager
import dev.justinianomarcelo.lucidanapp.util.BermejoZona
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * ViewModel de Home: carga de lugares cercanos (DS01), filtro de categoría
 * (HU02) y búsqueda desde un punto del mapa (DS02).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val lugarRepository = LugarRepository(AppDatabase.obtenerInstancia(application).lugarDao())
    private val usuarioRepository = UsuarioRepository(AppDatabase.obtenerInstancia(application).usuarioDao())
    private val locationService = LocationService(application)
    private val geofenceManager = GeofenceManager(application)

    var uiState by mutableStateOf(HomeUiState())
        private set

    fun cargarLugaresCercanos() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val ubicacion = locationService.obtenerUbicacionActual()
                ?: Coordenada(BermejoZona.LATITUD_CENTRO, BermejoZona.LONGITUD_CENTRO)
            uiState = uiState.copy(ubicacionActual = ubicacion, puntoSeleccionadoManual = null)
            actualizarLugares(ubicacion.latitud, ubicacion.longitud)
        }
    }

    fun onRadioSeleccionado(radioMetros: Double) {
        uiState = uiState.copy(radioSeleccionado = radioMetros)
        origenActivo()?.let { actualizarLugares(it.first, it.second) }
    }

    fun onCategoriaSeleccionada(categoria: CategoriaLugar?) {
        uiState = uiState.copy(categoriaSeleccionada = categoria)
        origenActivo()?.let { actualizarLugares(it.first, it.second) }
    }

    /** DS02: recálculo de lugares cercanos a partir de un punto del mapa. */
    fun recalcularDesdePunto(punto: GeoPoint) {
        if (!BermejoZona.estaDentroDeCobertura(punto.latitude, punto.longitude)) {
            uiState = uiState.copy(mensaje = "Ese punto está fuera de la zona de cobertura de Bermejo.")
            return
        }
        uiState = uiState.copy(puntoSeleccionadoManual = punto, mensaje = null)
        actualizarLugares(punto.latitude, punto.longitude)
    }

    fun limpiarPuntoManual() {
        uiState = uiState.copy(puntoSeleccionadoManual = null)
        uiState.ubicacionActual?.let { actualizarLugares(it.latitud, it.longitud) }
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }

    /** TA-13-03: activa o desactiva las notificaciones de cercanía. */
    suspend fun alternarNotificaciones() {
        val usuario = SessionManager.usuarioActual.value ?: return
        val nuevoValor = !usuario.notificacionesActivas
        usuarioRepository.actualizarNotificaciones(usuario.id, nuevoValor)
        SessionManager.iniciarSesion(usuario.copy(notificacionesActivas = nuevoValor))
        if (!nuevoValor) geofenceManager.quitarGeocercas()
    }

    private fun origenActivo(): Pair<Double, Double>? {
        uiState.puntoSeleccionadoManual?.let { return it.latitude to it.longitude }
        uiState.ubicacionActual?.let { return it.latitud to it.longitud }
        return null
    }

    private fun actualizarLugares(lat: Double, lng: Double) {
        viewModelScope.launch {
            val lugares = lugarRepository.obtenerLugaresCercanos(
                lat, lng, uiState.radioSeleccionado, uiState.categoriaSeleccionada
            )
            uiState = uiState.copy(lugares = lugares, cargando = false)
            registrarNotificacionesCercania(lat, lng)
        }
    }

    /** DS08: registra geocercas sobre los destacados cercanos, si el usuario lo permite. */
    private fun registrarNotificacionesCercania(lat: Double, lng: Double) {
        val usuario = SessionManager.usuarioActual.value ?: return
        if (!usuario.notificacionesActivas) return
        viewModelScope.launch {
            val destacados = lugarRepository.buscarDestacadosCercanos(lat, lng, 2000.0)
            geofenceManager.registrarGeocercas(destacados)
        }
    }
}
