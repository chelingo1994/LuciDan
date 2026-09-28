package dev.justinianomarcelo.lucidanapp.ui.favoritos

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.local.AppDatabase
import dev.justinianomarcelo.lucidanapp.data.model.Lugar
import dev.justinianomarcelo.lucidanapp.data.repository.FavoritoRepository
import kotlinx.coroutines.launch

data class FavoritosUiState(
    val lugares: List<Lugar> = emptyList(),
    val cargando: Boolean = true
)

class FavoritosViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritoRepository = FavoritoRepository(AppDatabase.obtenerInstancia(application).favoritoDao())

    var uiState by mutableStateOf(FavoritosUiState())
        private set

    fun cargarFavoritos() {
        val usuario = SessionManager.usuarioActual.value ?: run {
            uiState = uiState.copy(lugares = emptyList(), cargando = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val lugares = favoritoRepository.obtenerFavoritosDeUsuario(usuario.id)
            uiState = uiState.copy(lugares = lugares, cargando = false)
        }
    }
}
