package dev.justinianomarcelo.lucidanapp.ruta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToLong

data class ResultadoRuta(
    val distanciaTexto: String,
    val duracionTexto: String,
    val puntos: List<GeoPoint>
)

/**
 * Calcula rutas caminando entre dos puntos usando el servidor demo público
 * de OSRM (DS04, ver CLAUDE.md sección 2.1). No requiere API key ni
 * facturación; si más adelante el volumen de uso lo justifica, se puede
 * apuntar a un servidor OSRM propio cambiando solo [BASE_URL].
 */
class RutaService {

    suspend fun calcularRuta(origen: GeoPoint, destino: GeoPoint): ResultadoRuta? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "$BASE_URL/${origen.longitude},${origen.latitude};${destino.longitude},${destino.latitude}" +
                    "?overview=full&geometries=geojson"
            )
            val conexion = url.openConnection() as HttpURLConnection
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            // El proxy nginx del demo público de OSRM devuelve 403 al
            // User-Agent por defecto de Android (Dalvik/...); un User-Agent
            // de navegador lo evita.
            conexion.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android) LuciDanApp/1.0"
            )

            val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
            conexion.disconnect()

            val json = JSONObject(cuerpo)
            if (json.optString("code") != "Ok") return@withContext null
            val rutas = json.getJSONArray("routes")
            if (rutas.length() == 0) return@withContext null

            val ruta = rutas.getJSONObject(0)
            val distanciaMetros = ruta.getDouble("distance")
            val duracionSegundos = ruta.getDouble("duration")
            val coordenadas = ruta.getJSONObject("geometry").getJSONArray("coordinates")

            // GeoJSON entrega [longitud, latitud] por punto.
            val puntos = (0 until coordenadas.length()).map { indice ->
                val par = coordenadas.getJSONArray(indice)
                GeoPoint(par.getDouble(1), par.getDouble(0))
            }

            ResultadoRuta(
                distanciaTexto = formatearDistancia(distanciaMetros),
                duracionTexto = formatearDuracion(duracionSegundos),
                puntos = puntos
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun formatearDistancia(metros: Double): String = when {
        metros >= 1000 -> String.format(Locale("es", "BO"), "%.1f km", metros / 1000)
        else -> "${metros.roundToLong()} m"
    }

    private fun formatearDuracion(segundos: Double): String {
        val minutosTotales = (segundos / 60).roundToLong().coerceAtLeast(1)
        val horas = minutosTotales / 60
        val minutos = minutosTotales % 60
        return if (horas > 0) "$horas h $minutos min" else "$minutos min"
    }

    private companion object {
        const val BASE_URL = "https://router.project-osrm.org/route/v1/foot"
    }
}
