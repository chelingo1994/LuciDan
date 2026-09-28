package dev.justinianomarcelo.lucidanapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.justinianomarcelo.lucidanapp.navigation.LuciDanNavHost
import dev.justinianomarcelo.lucidanapp.ui.theme.LuciDanAppTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // osmdroid exige un userAgent propio para respetar la política de uso
        // de tiles de OpenStreetMap (ver CLAUDE.md sección 2.1).
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        val lugarIdInicial = intent.getLongExtra(EXTRA_LUGAR_ID, -1L).takeIf { it >= 0 }

        setContent {
            LuciDanAppTheme {
                LuciDanNavHost(lugarIdInicial = lugarIdInicial)
            }
        }
    }

    companion object {
        const val EXTRA_LUGAR_ID = "extra_lugar_id"
    }
}
