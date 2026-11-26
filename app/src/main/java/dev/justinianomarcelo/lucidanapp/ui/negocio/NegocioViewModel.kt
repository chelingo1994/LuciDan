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
import dev.justinianomarcelo.lucidanapp.notificaciones.AvisadorVencimiento
import dev.justinianomarcelo.lucidanapp.util.Beneficios
import dev.justinianomarcelo.lucidanapp.util.BeneficiosPlan
import dev.justinianomarcelo.lucidanapp.util.Destacados
import dev.justinianomarcelo.lucidanapp.util.LimiteFotos
import dev.justinianomarcelo.lucidanapp.util.RedSocial
import dev.justinianomarcelo.lucidanapp.util.VigenciaOferta
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
    /** La primera es la portada. */
    val fotos: List<String> = emptyList(),
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val ofertaTexto: String = "",
    /** null: conservar la vigencia de la oferta actual (o «hasta hoy» si es nueva). */
    val ofertaVigencia: VigenciaOferta? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val errores: Map<String, String> = emptyMap()
) {
    /** Lo que permite el plan vigente del negocio que se edita (uno nuevo no tiene plan). */
    val beneficios: Beneficios
        get() = negocioExistente?.let { Destacados.beneficios(it) } ?: BeneficiosPlan.de(BeneficiosPlan.SIN_PLAN)

    /** Vencimiento de la oferta ya publicada y todavía vigente, o 0. */
    val ofertaActivaHasta: Long
        get() = negocioExistente?.takeIf { it.ofertaTexto.isNotBlank() && it.ofertaHasta > System.currentTimeMillis() }
            ?.ofertaHasta ?: 0
}

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
            val negocios = negocioRepository.obtenerPorDueno(usuario.correo)
            if (usuario.notificacionesActivas) AvisadorVencimiento(getApplication()).revisar(negocios)
            uiState = uiState.copy(negocios = negocios, cargando = false)
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
                    fotos = negocio.fotos,
                    facebook = negocio.facebook,
                    instagram = negocio.instagram,
                    tiktok = negocio.tiktok,
                    // Una oferta vencida no vuelve a publicarse sola al guardar otros cambios.
                    ofertaTexto = if (negocio.ofertaHasta > System.currentTimeMillis()) negocio.ofertaTexto else "",
                    ofertaVigencia = null,
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

    fun onRedChange(red: RedSocial, valor: String) {
        val errores = uiState.errores - red.name
        uiState = when (red) {
            RedSocial.FACEBOOK -> uiState.copy(facebook = valor, errores = errores)
            RedSocial.INSTAGRAM -> uiState.copy(instagram = valor, errores = errores)
            RedSocial.TIKTOK -> uiState.copy(tiktok = valor, errores = errores)
        }
    }

    fun onOfertaChange(valor: String) {
        uiState = uiState.copy(ofertaTexto = valor.take(BeneficiosPlan.MAX_CARACTERES_OFERTA))
    }

    fun onOfertaVigenciaChange(vigencia: VigenciaOferta) {
        uiState = uiState.copy(ofertaVigencia = vigencia)
    }

    /** false (con aviso) si el plan no permite agregar otra foto. */
    fun puedeAgregarFoto(): Boolean {
        val maximo = uiState.beneficios.maxFotos
        if (uiState.fotos.size < maximo) return true
        uiState = uiState.copy(mensaje = LimiteFotos.mensaje(maximo))
        return false
    }

    /** Copia la imagen elegida de la galería al almacenamiento de la app y la agrega a las fotos. */
    fun onFotoSeleccionada(uri: Uri) {
        if (!puedeAgregarFoto()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val ruta = withContext(Dispatchers.IO) { FotoNegocio.guardarCopia(app, uri) }
            if (ruta == null) {
                uiState = uiState.copy(mensaje = "No se pudo cargar la foto")
                return@launch
            }
            uiState = uiState.copy(fotos = uiState.fotos + ruta)
        }
    }

    /** Borra la copia solo si aún no forma parte del negocio guardado. */
    fun onQuitarFoto(foto: String) {
        if (foto !in uiState.negocioExistente?.fotos.orEmpty()) FotoNegocio.eliminarCopia(getApplication(), foto)
        uiState = uiState.copy(fotos = uiState.fotos - foto)
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
                fotos = uiState.fotos,
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
                fotos = uiState.fotos
            ).conExtrasDelPlan()
            negocioRepository.actualizarNegocio(actualizado)
            (negocioActual.fotos - actualizado.fotos.toSet()).forEach { FotoNegocio.eliminarCopia(getApplication(), it) }
            uiState = uiState.copy(guardando = false, negocioExistente = actualizado, mensaje = "Cambios guardados")
            onExito()
        }
    }

    /** Redes y oferta: solo se cambian si el plan las incluye; si no, se conservan las guardadas. */
    private fun Negocio.conExtrasDelPlan(): Negocio {
        val beneficios = uiState.beneficios
        var resultado = this
        if (beneficios.redesSociales) {
            resultado = resultado.copy(
                facebook = RedSocial.FACEBOOK.normalizar(uiState.facebook).orEmpty(),
                instagram = RedSocial.INSTAGRAM.normalizar(uiState.instagram).orEmpty(),
                tiktok = RedSocial.TIKTOK.normalizar(uiState.tiktok).orEmpty()
            )
        }
        if (beneficios.ofertaDelDia) {
            val texto = uiState.ofertaTexto.trim()
            val ahora = System.currentTimeMillis()
            val hasta = when {
                texto.isEmpty() -> 0L
                else -> uiState.ofertaVigencia?.hasta(ahora)
                    ?: uiState.ofertaActivaHasta.takeIf { it > 0 }
                    ?: VigenciaOferta.HOY.hasta(ahora)
            }
            resultado = resultado.copy(ofertaTexto = texto, ofertaHasta = hasta)
        }
        return resultado
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
        if (uiState.beneficios.redesSociales) {
            listOf(
                RedSocial.FACEBOOK to uiState.facebook,
                RedSocial.INSTAGRAM to uiState.instagram,
                RedSocial.TIKTOK to uiState.tiktok
            ).forEach { (red, valor) ->
                if (red.normalizar(valor) == null) errores[red.name] = "Escribe tu usuario o un enlace de ${red.etiqueta}."
            }
        }
        return errores
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }
}
