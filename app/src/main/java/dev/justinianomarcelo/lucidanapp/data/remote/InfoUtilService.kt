package dev.justinianomarcelo.lucidanapp.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dev.justinianomarcelo.lucidanapp.data.model.Clima
import dev.justinianomarcelo.lucidanapp.data.model.DiaClima
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.ReferenciaCambio
import dev.justinianomarcelo.lucidanapp.data.model.TasaCambio
import dev.justinianomarcelo.lucidanapp.util.BermejoZona
import dev.justinianomarcelo.lucidanapp.util.Cambio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Un documento de Firestore; [desdeServidor] es false si vino solo de la caché local de Firestore. */
data class DocumentoNube<T>(val valor: T?, val desdeServidor: Boolean)

enum class ResultadoGuardado {
    /** El servidor confirmó la escritura. */
    CONFIRMADO,

    /** Sin conexión: Firestore la guardó en el teléfono y la enviará al volver internet. */
    PENDIENTE,

    /** Firestore la rechazó (reglas o formato). */
    ERROR
}

/**
 * Fuentes de la sección "Info útil": documentos `info_util/cambio` e `info_util/frontera` de Firestore y
 * dos APIs públicas sin clave: open.er-api.com (referencia internacional ARS/Bs) y Open-Meteo (clima).
 * Las lecturas devuelven null si no hay red.
 */
class InfoUtilService(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val coleccion get() = firestore.collection(COLECCION)

    suspend fun obtenerCambio(): DocumentoNube<TasaCambio>? = leer(DOC_CAMBIO) { it.aTasa() }

    suspend fun obtenerFrontera(): DocumentoNube<InfoFrontera>? = leer(DOC_FRONTERA) { it.aFrontera() }

    suspend fun guardarCambio(tasa: TasaCambio): ResultadoGuardado = guardar(
        DOC_CAMBIO,
        mapOf(
            "compraArsPorBs" to tasa.compraArsPorBs,
            "ventaArsPorBs" to tasa.ventaArsPorBs,
            "actualizado" to tasa.actualizado,
            "adminId" to tasa.adminId
        )
    )

    suspend fun guardarFrontera(info: InfoFrontera): ResultadoGuardado = guardar(
        DOC_FRONTERA,
        mapOf(
            "horarioPaso" to info.horarioPaso,
            "horarioChalanas" to info.horarioChalanas,
            "tarifasChalanas" to info.tarifasChalanas,
            "recomendaciones" to info.recomendaciones,
            "telPolicia" to info.telPolicia,
            "telBomberos" to info.telBomberos,
            "telAmbulancia" to info.telAmbulancia,
            "telMigracion" to info.telMigracion,
            "actualizado" to info.actualizado,
            "adminId" to info.adminId
        )
    )

    private suspend fun <T> leer(id: String, convertir: (DocumentSnapshot) -> T?): DocumentoNube<T>? =
        try {
            withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
                val documento = coleccion.document(id).get().await()
                DocumentoNube(
                    valor = if (documento.exists()) convertir(documento) else null,
                    desdeServidor = !documento.metadata.isFromCache
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer info_util/$id", e)
            null
        }

    private suspend fun guardar(id: String, datos: Map<String, Any>): ResultadoGuardado =
        try {
            val tarea = coleccion.document(id).set(datos)
            // Sin red, set() no termina hasta reconectar (la escritura queda en cola): se da por pendiente.
            withTimeoutOrNull(TIEMPO_GUARDADO_MS) { tarea.await(); ResultadoGuardado.CONFIRMADO }
                ?: ResultadoGuardado.PENDIENTE
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo guardar info_util/$id", e)
            ResultadoGuardado.ERROR
        }

    // --- APIs públicas (sin API key) ---------------------------------------------------------------------

    /** open.er-api.com: cruce `ARS/USD ÷ BOB/USD`. Null si falla o falta alguna de las dos monedas. */
    suspend fun obtenerReferencia(): ReferenciaCambio? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(descargar(URL_REFERENCIA))
            if (json.optString("result") != "success") return@withContext null
            val tasas = json.optJSONObject("rates") ?: return@withContext null
            val cruce = Cambio.cruceArsPorBs(
                tasas.optDouble("ARS").takeIf { !it.isNaN() },
                tasas.optDouble("BOB").takeIf { !it.isNaN() }
            ) ?: return@withContext null
            val fechaMs = json.optLong("time_last_update_unix", 0L) * 1000
            ReferenciaCambio(cruce, if (fechaMs > 0) fechaMs else System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener la referencia internacional", e)
            null
        }
    }

    /** Open-Meteo: clima actual y pronóstico de 3 días en Bermejo. */
    suspend fun obtenerClima(): Clima? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(descargar(urlClima()))
            val actual = json.getJSONObject("current")
            val diario = json.getJSONObject("daily")
            val fechas = diario.getJSONArray("time")
            val dias = (0 until fechas.length()).map { i ->
                DiaClima(
                    fecha = fechas.getString(i),
                    maxima = diario.getJSONArray("temperature_2m_max").getDouble(i),
                    minima = diario.getJSONArray("temperature_2m_min").getDouble(i),
                    probLluvia = diario.getJSONArray("precipitation_probability_max").optInt(i, 0),
                    codigo = diario.optJSONArray("weather_code")?.takeUnless { it.isNull(i) }?.getInt(i),
                    salidaSol = diario.getJSONArray("sunrise").getString(i).substringAfter('T'),
                    puestaSol = diario.getJSONArray("sunset").getString(i).substringAfter('T')
                )
            }
            Clima(
                temperatura = actual.getDouble("temperature_2m"),
                sensacion = actual.getDouble("apparent_temperature"),
                humedad = actual.getInt("relative_humidity_2m"),
                viento = actual.getDouble("wind_speed_10m"),
                codigo = if (actual.isNull("weather_code")) null else actual.getInt("weather_code"),
                dias = dias,
                obtenidoMs = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener el clima", e)
            null
        }
    }

    private fun descargar(url: String): String {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = TIEMPO_RED_MS
            conexion.readTimeout = TIEMPO_RED_MS
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) LuciDanApp/1.0")
            if (conexion.responseCode !in 200..299) error("HTTP ${conexion.responseCode} en $url")
            return conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    companion object {
        const val COLECCION = "info_util"
        const val DOC_CAMBIO = "cambio"
        const val DOC_FRONTERA = "frontera"
        private const val TAG = "InfoUtilService"
        private const val TIEMPO_MAXIMO_MS = 10_000L
        private const val TIEMPO_GUARDADO_MS = 8_000L
        private const val TIEMPO_RED_MS = 10_000
        private const val URL_REFERENCIA = "https://open.er-api.com/v6/latest/USD"

        private fun urlClima() =
            "https://api.open-meteo.com/v1/forecast?latitude=${BermejoZona.LATITUD_CENTRO}" +
                "&longitude=${BermejoZona.LONGITUD_CENTRO}" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=America%2FLa_Paz&forecast_days=3"
    }
}

private fun DocumentSnapshot.aTasa(): TasaCambio? = TasaCambio(
    compraArsPorBs = getDouble("compraArsPorBs") ?: return null,
    ventaArsPorBs = getDouble("ventaArsPorBs") ?: return null,
    actualizado = getLong("actualizado") ?: 0L,
    adminId = getString("adminId").orEmpty()
)

private fun DocumentSnapshot.aFrontera(): InfoFrontera {
    fun texto(campo: String) = getString(campo)?.takeIf { it.isNotBlank() } ?: InfoFrontera.POR_CONFIRMAR
    return InfoFrontera(
        horarioPaso = texto("horarioPaso"),
        horarioChalanas = texto("horarioChalanas"),
        tarifasChalanas = texto("tarifasChalanas"),
        recomendaciones = texto("recomendaciones"),
        telPolicia = texto("telPolicia"),
        telBomberos = texto("telBomberos"),
        telAmbulancia = texto("telAmbulancia"),
        telMigracion = texto("telMigracion"),
        actualizado = getLong("actualizado") ?: 0L,
        adminId = getString("adminId").orEmpty()
    )
}
