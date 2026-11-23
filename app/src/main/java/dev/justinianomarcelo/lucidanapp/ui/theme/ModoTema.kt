package dev.justinianomarcelo.lucidanapp.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Cómo se elige el tema de la app: el del sistema o uno fijo. */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Del sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/**
 * Preferencia de tema guardada en el teléfono (SharedPreferences). [modo] es estado de Compose: al cambiarlo,
 * toda la app se recompone con el tema nuevo sin reiniciar.
 */
object PreferenciaTema {
    private const val ARCHIVO = "ajustes"
    private const val CLAVE = "modo_tema"

    var modo by mutableStateOf(ModoTema.SISTEMA)
        private set

    /** Lee la preferencia guardada; se llama una vez al arrancar la actividad. */
    fun cargar(context: Context) {
        val guardado = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE, null)
        modo = ModoTema.entries.firstOrNull { it.name == guardado } ?: ModoTema.SISTEMA
    }

    fun cambiar(context: Context, nuevo: ModoTema) {
        modo = nuevo
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE, nuevo.name).apply()
    }

    /** true si hay que dibujar el tema oscuro con la preferencia actual. */
    @Composable
    fun esOscuro(): Boolean = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
}
