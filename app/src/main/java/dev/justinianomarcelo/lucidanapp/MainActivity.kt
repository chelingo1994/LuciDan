package dev.justinianomarcelo.lucidanapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import dev.justinianomarcelo.lucidanapp.navigation.LuciDanNavHost
import dev.justinianomarcelo.lucidanapp.ui.theme.LuciDanAppTheme
import dev.justinianomarcelo.lucidanapp.ui.theme.PreferenciaTema
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        enableEdgeToEdge()

        // osmdroid exige un userAgent propio para respetar la política de uso
        // de tiles de OpenStreetMap (ver CLAUDE.md sección 2.1).
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        val negocioIdInicial = intent.getLongExtra(EXTRA_NEGOCIO_ID, -1L).takeIf { it >= 0 }

        setContent {
            val oscuro = PreferenciaTema.esOscuro()
            // Los íconos de las barras del sistema deben contrastar con el tema elegido en la app, que puede
            // diferir del tema del sistema.
            DisposableEffect(oscuro) {
                // Estilos explícitos (no `auto`): `auto` deja que el sistema decida el contraste de la barra de
                // navegación según SU tema, que puede ser el contrario al elegido en la app.
                val velo = if (oscuro) Color.argb(0xB3, 0x20, 0x1A, 0x1A) else Color.argb(0xE6, 0xFF, 0xFB, 0xFF)
                enableEdgeToEdge(
                    statusBarStyle = if (oscuro) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (oscuro) SystemBarStyle.dark(velo)
                    else SystemBarStyle.light(velo, velo)
                )
                onDispose { }
            }
            LuciDanAppTheme(darkTheme = oscuro) {
                LuciDanNavHost(negocioIdInicial = negocioIdInicial)
            }
        }
    }

    companion object {
        const val EXTRA_NEGOCIO_ID = "extra_negocio_id"
    }
}
