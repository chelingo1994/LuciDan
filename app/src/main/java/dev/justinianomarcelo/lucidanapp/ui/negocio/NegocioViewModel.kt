package dev.justinianomarcelo.lucidanapp.ui.negocio

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.auth.Validadores
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import dev.justinianomarcelo.lucidanapp.location.LocationService
import dev.justinianomarcelo.lucidanapp.util.FotoNegocio
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NegocioUiState(
    /** Lista "Mis negocios" (0..*). */
    val negocios: List<Negocio> = emptyList(),
    /** Negocio que se está editando; null si el formulario es para registrar uno nuevo. */
    val negocioExistente: Negocio? = null,
    val noEncontrado: Boolean = false,
    val nombre: String = "",
    val categoria: Rubro? = null,
    val descripcion: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val obteniendoUbicacion: Boolean = false,
    val horario: String = "",
    val foto: String = "",
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val errores: Map<String, String> = emptyMap()
)

/** Id que usa la pantalla de edición para indicar "registrar un negocio nuevo". */
const val NUEVO_NEGOCIO = 0L

/** Implementa DS07 (registro, HU08) y la edición inmediata de HU09. */
class NegocioViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())
    private val usuarioRepository = UsuarioRepository(AppDatabase.obtenerInstancia(application).usuarioDao())
    private val locationService = LocationService(application)

    var uiState by mutableStateOf(NegocioUiState())
        private set

    /** "Mis negocios": todos los negocios del usuario con sesión. */
    fun cargarNegocios() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            uiState = uiState.copy(negocios = negocioRepository.obtenerPorDueno(usuario.correo), cargando = false)
        }
    }

    /** Carga el formulario: vacío si [negocioId] es [NUEVO_NEGOCIO], o con los datos de ese negocio del usuario. */
    fun cargarNegocio(negocioId: Long) {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(cargando = false)
            return
        }
        if (negocioId == NUEVO_NEGOCIO) {
            uiState = NegocioUiState(cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, noEncontrado = false)
            val negocio = negocioRepository.obtenerDelDueno(negocioId, usuario.correo)
            uiState = if (negocio != null) {
                uiState.copy(
                    negocioExistente = negocio,
                    nombre = negocio.nombre,
                    categoria = negocio.categoria,
                    descripcion = negocio.descripcion,
                    telefono = negocio.telefono,
                    direccion = negocio.direccion,
                    latitud = negocio.latitud,
                    longitud = negocio.longitud,
                    horario = negocio.horario,
                    foto = negocio.fotos.firstOrNull().orEmpty(),
                    cargando = false
                )
            } else {
                uiState.copy(cargando = false, noEncontrado = true)
            }
        }
    }

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errores = uiState.errores - "nombre")
    }

    fun onCategoriaChange(valor: Rubro) {
        uiState = uiState.copy(categoria = valor, errores = uiState.errores - "categoria")
    }

    fun onDescripcionChange(valor: String) {
        uiState = uiState.copy(descripcion = valor, errores = uiState.errores - "descripcion")
    }

    fun onTelefonoChange(valor: String) {
        uiState = uiState.copy(telefono = valor, errores = uiState.errores - "telefono")
    }

    fun onDireccionChange(valor: String) {
        uiState = uiState.copy(direccion = valor, errores = uiState.errores - "direccion")
    }

    fun onHorarioChange(valor: String) {
        uiState = uiState.copy(horario = valor, errores = uiState.errores - "horario")
    }

    /** Captura la ubicación GPS actual para que el negocio pueda mostrarse en el mapa (HU01/HU05). */
    fun onUsarUbicacionActual() {
        viewModelScope.launch {
            uiState = uiState.copy(obteniendoUbicacion = true)
            val coordenada = locationService.obtenerUbicacionActual()
            uiState = if (coordenada != null) {
                uiState.copy(
                    latitud = coordenada.latitud,
                    longitud = coordenada.longitud,
                    obteniendoUbicacion = false,
                    errores = uiState.errores - "ubicacionGps"
                )
            } else {
                uiState.copy(
                    obteniendoUbicacion = false,
                    mensaje = "No se pudo obtener tu ubicación. Verifica el permiso y el GPS."
                )
            }
        }
    }

    /** Copia la imagen elegida de la galería al almacenamiento de la app y la usa como foto. */
    fun onFotoSeleccionada(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val ruta = withContext(Dispatchers.IO) { FotoNegocio.guardarCopia(app, uri) }
            if (ruta == null) {
                uiState = uiState.copy(mensaje = "No se pudo cargar la foto")
                return@launch
            }
            descartarFotoNoGuardada()
            uiState = uiState.copy(foto = ruta)
        }
    }

    fun onQuitarFoto() {
        descartarFotoNoGuardada()
        uiState = uiState.copy(foto = "")
    }

    /** Borra la copia del formulario solo si aún no forma parte del negocio guardado. */
    private fun descartarFotoNoGuardada() {
        val actual = uiState.foto
        if (actual != uiState.negocioExistente?.fotos?.firstOrNull()) {
            FotoNegocio.eliminarCopia(getApplication(), actual)
        }
    }

    /** DS07: valida campos obligatorios y guarda con estado Pendiente. */
    fun registrarNegocio(onExito: () -> Unit) {
        val usuario = SessionManager.usuarioActual.value ?: return
        val errores = validar()
        if (errores.isNotEmpty()) {
            uiState = uiState.copy(errores = errores)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(guardando = true)
            val negocio = Negocio(
                usuarioId = usuario.id,
                nombre = uiState.nombre.trim(),
                categoria = uiState.categoria!!,
                descripcion = uiState.descripcion.trim(),
                telefono = uiState.telefono.trim(),
                direccion = uiState.direccion.trim(),
                latitud = uiState.latitud!!,
                longitud = uiState.longitud!!,
                horario = uiState.horario.trim(),
                fotos = if (uiState.foto.isBlank()) emptyList() else listOf(uiState.foto.trim()),
                estado = EstadoNegocio.PENDIENTE,
                duenoId = IdDueno.deCorreo(usuario.correo)
            )
            val id = negocioRepository.registrarNegocio(negocio)
            if (usuario.rol != RolUsuario.NEGOCIO) {
                usuarioRepository.actualizarRol(usuario.id, RolUsuario.NEGOCIO)
                SessionManager.iniciarSesion(usuario.copy(rol = RolUsuario.NEGOCIO))
            }
            uiState = uiState.copy(
                guardando = false,
                negocioExistente = negocio.copy(id = id),
                mensaje = "Perfil pendiente de publicación"
            )
            onExito()
        }
    }

    /** TA-09-03: guarda cambios y los refleja de inmediato en la app. */
    fun guardarCambios(onExito: () -> Unit) {
        val negocioActual = uiState.negocioExistente ?: return
        val errores = validar()
        if (errores.isNotEmpty()) {
            uiState = uiState.copy(errores = errores)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(guardando = true)
            val actualizado = negocioActual.copy(
                nombre = uiState.nombre.trim(),
                categoria = uiState.categoria!!,
                descripcion = uiState.descripcion.trim(),
                telefono = uiState.telefono.trim(),
                direccion = uiState.direccion.trim(),
                latitud = uiState.latitud!!,
                longitud = uiState.longitud!!,
                horario = uiState.horario.trim(),
                fotos = if (uiState.foto.isBlank()) emptyList() else listOf(uiState.foto.trim())
            )
            negocioRepository.actualizarNegocio(actualizado)
            val fotoAnterior = negocioActual.fotos.firstOrNull()
            if (fotoAnterior != actualizado.fotos.firstOrNull()) {
                FotoNegocio.eliminarCopia(getApplication(), fotoAnterior)
            }
            uiState = uiState.copy(guardando = false, negocioExistente = actualizado, mensaje = "Cambios guardados")
            onExito()
        }
    }

    private fun validar(): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        if (uiState.nombre.isBlank()) errores["nombre"] = "El nombre es obligatorio."
        if (uiState.categoria == null) errores["categoria"] = "Selecciona una categoría."
        if (uiState.descripcion.isBlank()) errores["descripcion"] = "Cuenta qué vendes u ofreces."
        // El teléfono es opcional: solo se valida el formato si el dueño escribió algo.
        if (uiState.telefono.isNotBlank() && !Validadores.esTelefonoValido(uiState.telefono)) {
            errores["telefono"] = "El teléfono no es válido (solo números, espacios, + y -)."
        }
        if (uiState.direccion.isBlank()) errores["direccion"] = "La dirección es obligatoria."
        if (uiState.latitud == null || uiState.longitud == null) {
            errores["ubicacionGps"] = "Captura tu ubicación con el botón de arriba para aparecer en el mapa."
        }
        if (uiState.horario.isBlank()) errores["horario"] = "El horario es obligatorio."
        return errores
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }
}
