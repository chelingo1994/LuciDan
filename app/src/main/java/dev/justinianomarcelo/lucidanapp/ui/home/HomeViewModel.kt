package dev.justinianomarcelo.lucidanapp.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.repository.ContratacionRepository
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.data.repository.ResenaRepository
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import dev.justinianomarcelo.lucidanapp.location.Coordenada
import dev.justinianomarcelo.lucidanapp.location.LocationService
import dev.justinianomarcelo.lucidanapp.notificaciones.AvisadorVencimiento
import dev.justinianomarcelo.lucidanapp.notificaciones.GeofenceManager
import dev.justinianomarcelo.lucidanapp.util.BermejoZona
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * ViewModel de Home: carga de negocios cercanos (DS01), filtro de categoría
 * (HU02) y búsqueda desde un punto del mapa (DS02).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val resenaRepository = ResenaRepository(AppDatabase.obtenerInstancia(application).resenaDao(), negocioRepository)
    private val usuarioRepository = UsuarioRepository(AppDatabase.obtenerInstancia(application).usuarioDao())
    private val contratacionRepository = ContratacionRepository(
        AppDatabase.obtenerInstancia(application).contratacionDao(),
        negocioRepository
    )
    private val locationService = LocationService(application)
    private val geofenceManager = GeofenceManager(application)

    var uiState by mutableStateOf(HomeUiState())
        private set

    /** Negocio cuyo marcador se ve como "seleccionado"; vive aquí para sobrevivir a la ida y vuelta al detalle. */
    var negocioSeleccionadoId by mutableStateOf<Long?>(null)
        private set

    fun onNegocioSeleccionado(id: Long?) {
        negocioSeleccionadoId = id
    }

    /** Solo vale el último cálculo: al escribir rápido, los anteriores no deben pisar al más nuevo. */
    private var trabajoActualizacion: Job? = null

    fun cargarNegociosCercanos() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            // DS09: sustituye al proceso programado de desactivación de planes vencidos.
            contratacionRepository.desactivarVencidas()
            contratacionRepository.aplicarPlanes()
            val ubicacion = locationService.obtenerUbicacionActual()
                ?: Coordenada(BermejoZona.LATITUD_CENTRO, BermejoZona.LONGITUD_CENTRO)
            uiState = uiState.copy(ubicacionActual = ubicacion, puntoSeleccionadoManual = null)
            actualizarNegocios(ubicacion.latitud, ubicacion.longitud)
            sincronizarConNube()
            avisarPlanesPorVencer()
        }
    }

    /**
     * Pone al día la caché local con Firestore sin bloquear la pantalla: Home ya mostró lo que hay en
     * Room y solo se recalcula si la sincronización trajo algo.
     */
    private suspend fun sincronizarConNube() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NUBE, Context.MODE_PRIVATE)
        // La subida de los negocios locales anteriores a Firestore se hace una sola vez por teléfono.
        val subirHuerfanos = !prefs.getBoolean(CLAVE_NEGOCIOS_SUBIDOS, false)
        val duenos = if (subirHuerfanos) usuarioRepository.obtenerTodos().map { IdDueno.deCorreo(it.correo) }.toSet() else emptySet()
        val negociosOk = negocioRepository.sincronizar { negocio: Negocio ->
            subirHuerfanos && negocio.duenoId in duenos
        }
        // La nube (o su caché) pudo traer el negocio sin los datos del plan: se reponen los contratados aquí.
        contratacionRepository.aplicarPlanes()
        if (negociosOk) {
            prefs.edit().putBoolean(CLAVE_NEGOCIOS_SUBIDOS, true).apply()
            // Las reseñas cuelgan de los negocios, así que se bajan después de ellos.
            resenaRepository.sincronizar()
            // Un fallo de reseñas no debe impedir refrescar el mapa; este recálculo ya incluye los promedios.
            origenActivo()?.let { actualizarNegocios(it.first, it.second) }
        }
    }

    /** Aviso al dueño (plan Trimestral) cuando faltan pocos días; sin backend, se revisa al abrir la app. */
    private suspend fun avisarPlanesPorVencer() {
        val usuario = SessionManager.usuarioActual.value ?: return
        if (!usuario.notificacionesActivas) return
        AvisadorVencimiento(getApplication()).revisar(negocioRepository.obtenerPorDueno(usuario.correo))
    }

    fun onRadioSeleccionado(radioMetros: Double) {
        uiState = uiState.copy(radioSeleccionado = radioMetros, busquedaAmpliada = false)
        origenActivo()?.let { actualizarNegocios(it.first, it.second) }
    }

    /** Buscador: filtra negocios por texto, combinado con el radio y la categoría. */
    fun onConsultaCambiada(consulta: String) {
        uiState = uiState.copy(consulta = consulta, busquedaAmpliada = uiState.busquedaAmpliada && consulta.isNotBlank())
        origenActivo()?.let { actualizarNegocios(it.first, it.second, registrarGeocercas = false) }
    }

    /** Cuando el texto no da resultados en el radio, permite buscarlo en toda la zona de Bermejo. */
    fun onAmpliarBusqueda(ampliar: Boolean) {
        uiState = uiState.copy(busquedaAmpliada = ampliar)
        origenActivo()?.let { actualizarNegocios(it.first, it.second, registrarGeocercas = false) }
    }

    fun onCategoriaSeleccionada(categoria: Rubro?) {
        uiState = uiState.copy(categoriaSeleccionada = categoria)
        origenActivo()?.let { actualizarNegocios(it.first, it.second) }
    }

    /** DS02: recálculo de negocios cercanos a partir de un punto del mapa. */
    fun recalcularDesdePunto(punto: GeoPoint) {
        if (!BermejoZona.estaDentroDeCobertura(punto.latitude, punto.longitude)) {
            uiState = uiState.copy(mensaje = "Ese punto está fuera de la zona de cobertura de Bermejo.")
            return
        }
        uiState = uiState.copy(puntoSeleccionadoManual = punto, mensaje = null)
        actualizarNegocios(punto.latitude, punto.longitude)
    }

    fun limpiarPuntoManual() {
        uiState = uiState.copy(puntoSeleccionadoManual = null)
        uiState.ubicacionActual?.let { actualizarNegocios(it.latitud, it.longitud) }
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

    /** DS01/HU02: negocios publicados dentro del radio, filtrados por rubro y texto (sección 2.2 de CLAUDE.md). */
    private fun actualizarNegocios(lat: Double, lng: Double, registrarGeocercas: Boolean = true) {
        trabajoActualizacion?.cancel()
        trabajoActualizacion = viewModelScope.launch {
            val radio = if (uiState.busquedaAmpliada) Double.MAX_VALUE else uiState.radioSeleccionado
            val negocios = negocioRepository.obtenerNegociosCercanos(
                lat, lng, radio, uiState.categoriaSeleccionada, uiState.consulta
            )
            uiState = uiState.copy(
                negocios = negocios,
                enRadio = negocios.count { it.calcularDistancia(lat, lng) <= radio },
                carrusel = negocioRepository.obtenerParaCarrusel(),
                cargando = false
            )
            // Las geocercas no dependen del texto buscado: no se recalculan en cada tecla.
            if (registrarGeocercas) registrarNotificacionesCercania(lat, lng)
        }
    }

    /** DS08: registra geocercas sobre los destacados cercanos, si el usuario lo permite. */
    private fun registrarNotificacionesCercania(lat: Double, lng: Double) {
        val usuario = SessionManager.usuarioActual.value ?: return
        if (!usuario.notificacionesActivas) return
        viewModelScope.launch {
            val destacados = negocioRepository.buscarDestacadosCercanos(lat, lng, 2000.0)
            geofenceManager.registrarGeocercas(destacados)
        }
    }

    private companion object {
        const val PREFS_NUBE = "nube"
        const val CLAVE_NEGOCIOS_SUBIDOS = "negocios_locales_subidos"
    }
}
