package dev.justinianomarcelo.lucidanapp.ui.negocio

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.repository.NegocioRepository
import dev.justinianomarcelo.lucidanapp.util.FotoNegocio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NegocioUiState(
    val negocioExistente: Negocio? = null,
    val nombre: String = "",
    val categoria: CategoriaLugar? = null,
    val ubicacion: String = "",
    val horario: String = "",
    val foto: String = "",
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val errores: Map<String, String> = emptyMap()
)

/** Implementa DS07 (registro, HU08) y la edición inmediata de HU09. */
class NegocioViewModel(application: Application) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(AppDatabase.obtenerInstancia(application).negocioDao())

    var uiState by mutableStateOf(NegocioUiState())
        private set

    fun cargarNegocio() {
        val usuario = SessionManager.usuarioActual.value
        if (usuario == null) {
            uiState = uiState.copy(cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val negocio = negocioRepository.obtenerPorUsuario(usuario.id)
            uiState = if (negocio != null) {
                uiState.copy(
                    negocioExistente = negocio,
                    nombre = negocio.nombre,
                    categoria = negocio.categoria,
                    ubicacion = negocio.ubicacion,
                    horario = negocio.horario,
                    foto = negocio.fotos.firstOrNull().orEmpty(),
                    cargando = false
                )
            } else {
                uiState.copy(cargando = false)
            }
        }
    }

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errores = uiState.errores - "nombre")
    }

    fun onCategoriaChange(valor: CategoriaLugar) {
        uiState = uiState.copy(categoria = valor, errores = uiState.errores - "categoria")
    }

    fun onUbicacionChange(valor: String) {
        uiState = uiState.copy(ubicacion = valor, errores = uiState.errores - "ubicacion")
    }

    fun onHorarioChange(valor: String) {
        uiState = uiState.copy(horario = valor, errores = uiState.errores - "horario")
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
                ubicacion = uiState.ubicacion.trim(),
                horario = uiState.horario.trim(),
                fotos = if (uiState.foto.isBlank()) emptyList() else listOf(uiState.foto.trim()),
                estado = EstadoNegocio.PENDIENTE
            )
            val id = negocioRepository.registrarNegocio(negocio)
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
                ubicacion = uiState.ubicacion.trim(),
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
        if (uiState.ubicacion.isBlank()) errores["ubicacion"] = "La ubicación es obligatoria."
        if (uiState.horario.isBlank()) errores["horario"] = "El horario es obligatorio."
        return errores
    }

    fun limpiarMensaje() {
        uiState = uiState.copy(mensaje = null)
    }
}
