package dev.justinianomarcelo.lucidanapp.data.repository

import android.content.Context
import dev.justinianomarcelo.lucidanapp.data.model.Clima
import dev.justinianomarcelo.lucidanapp.data.model.DiaClima
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.ReferenciaCambio
import dev.justinianomarcelo.lucidanapp.data.model.TasaCambio
import dev.justinianomarcelo.lucidanapp.data.remote.InfoUtilService
import dev.justinianomarcelo.lucidanapp.data.remote.ResultadoGuardado
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resultado de una carga. [desdeCache] es true cuando no se pudo actualizar y [valor] es el último dato
 * conocido guardado en el teléfono (puede ser null si nunca hubo uno).
 */
data class Cargado<T>(val valor: T?, val desdeCache: Boolean)

/**
 * Datos de "Info útil". Cada dato se guarda en SharedPreferences al descargarlo para mostrarlo sin internet:
 * es un simple valor JSON, así no se toca el esquema de Room.
 */
class InfoUtilRepository(
    context: Context,
    private val servicio: InfoUtilService = InfoUtilService()
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // --- Tipo de cambio publicado por LuciDan --------------------------------------------------------

    fun cambioEnCache(): TasaCambio? = prefs.getString(K_CAMBIO, null)?.let { runCatching { tasaDeJson(JSONObject(it)) }.getOrNull() }

    suspend fun cargarCambio(): Cargado<TasaCambio> {
        val remoto = servicio.obtenerCambio() ?: return Cargado(cambioEnCache(), desdeCache = true)
        // Si el servidor dice que no hay documento, no se muestra una tasa vieja como si siguiera vigente.
        if (remoto.valor == null) {
            if (!remoto.desdeServidor) return Cargado(cambioEnCache(), desdeCache = true)
            prefs.edit().remove(K_CAMBIO).apply()
            return Cargado(null, desdeCache = false)
        }
        prefs.edit().putString(K_CAMBIO, tasaAJson(remoto.valor).toString()).apply()
        return Cargado(remoto.valor, desdeCache = !remoto.desdeServidor)
    }

    suspend fun guardarCambio(tasa: TasaCambio): ResultadoGuardado {
        val resultado = servicio.guardarCambio(tasa)
        if (resultado != ResultadoGuardado.ERROR) prefs.edit().putString(K_CAMBIO, tasaAJson(tasa).toString()).apply()
        return resultado
    }

    // --- Referencia internacional (open.er-api.com) ---------------------------------------------------

    fun referenciaEnCache(): ReferenciaCambio? = prefs.getString(K_REFERENCIA, null)?.let {
        runCatching { JSONObject(it).let { j -> ReferenciaCambio(j.getDouble("ars"), j.getLong("fecha")) } }.getOrNull()
    }

    suspend fun cargarReferencia(): Cargado<ReferenciaCambio> {
        val remota = servicio.obtenerReferencia() ?: return Cargado(referenciaEnCache(), desdeCache = true)
        prefs.edit().putString(K_REFERENCIA, JSONObject().put("ars", remota.arsPorBs).put("fecha", remota.fechaMs).toString()).apply()
        return Cargado(remota, desdeCache = false)
    }

    // --- Clima (Open-Meteo) ----------------------------------------------------------------------------

    fun climaEnCache(): Clima? = prefs.getString(K_CLIMA, null)?.let { runCatching { climaDeJson(JSONObject(it)) }.getOrNull() }

    /** Si el dato guardado tiene menos de [VIGENCIA_CLIMA_MS] y no se [forzar], no se vuelve a descargar. */
    suspend fun cargarClima(forzar: Boolean, ahora: Long = System.currentTimeMillis()): Cargado<Clima> {
        val guardado = climaEnCache()
        if (!forzar && guardado != null && ahora - guardado.obtenidoMs < VIGENCIA_CLIMA_MS) return Cargado(guardado, desdeCache = false)
        val nuevo = servicio.obtenerClima() ?: return Cargado(guardado, desdeCache = true)
        prefs.edit().putString(K_CLIMA, climaAJson(nuevo).toString()).apply()
        return Cargado(nuevo, desdeCache = false)
    }

    // --- Paso fronterizo -------------------------------------------------------------------------------

    fun fronteraEnCache(): InfoFrontera? = prefs.getString(K_FRONTERA, null)?.let { runCatching { fronteraDeJson(JSONObject(it)) }.getOrNull() }

    suspend fun cargarFrontera(): Cargado<InfoFrontera> {
        val remoto = servicio.obtenerFrontera() ?: return Cargado(fronteraEnCache(), desdeCache = true)
        if (remoto.valor == null) {
            if (!remoto.desdeServidor) return Cargado(fronteraEnCache(), desdeCache = true)
            prefs.edit().remove(K_FRONTERA).apply()
            return Cargado(null, desdeCache = false)
        }
        prefs.edit().putString(K_FRONTERA, fronteraAJson(remoto.valor).toString()).apply()
        return Cargado(remoto.valor, desdeCache = !remoto.desdeServidor)
    }

    suspend fun guardarFrontera(info: InfoFrontera): ResultadoGuardado {
        val resultado = servicio.guardarFrontera(info)
        if (resultado != ResultadoGuardado.ERROR) prefs.edit().putString(K_FRONTERA, fronteraAJson(info).toString()).apply()
        return resultado
    }

    companion object {
        const val VIGENCIA_CLIMA_MS = 30 * 60 * 1000L
        private const val PREFS = "info_util_cache"
        private const val K_CAMBIO = "cambio"
        private const val K_REFERENCIA = "referencia"
        private const val K_CLIMA = "clima"
        private const val K_FRONTERA = "frontera"
    }
}

private fun tasaAJson(t: TasaCambio) = JSONObject()
    .put("compra", t.compraArsPorBs).put("venta", t.ventaArsPorBs).put("actualizado", t.actualizado).put("adminId", t.adminId)

private fun tasaDeJson(j: JSONObject) =
    TasaCambio(j.getDouble("compra"), j.getDouble("venta"), j.getLong("actualizado"), j.optString("adminId"))

private fun fronteraAJson(f: InfoFrontera) = JSONObject()
    .put("horarioPaso", f.horarioPaso).put("horarioChalanas", f.horarioChalanas).put("tarifasChalanas", f.tarifasChalanas)
    .put("recomendaciones", f.recomendaciones).put("telPolicia", f.telPolicia).put("telBomberos", f.telBomberos)
    .put("telAmbulancia", f.telAmbulancia).put("telMigracion", f.telMigracion)
    .put("actualizado", f.actualizado).put("adminId", f.adminId)

private fun fronteraDeJson(j: JSONObject) = InfoFrontera(
    horarioPaso = j.getString("horarioPaso"), horarioChalanas = j.getString("horarioChalanas"),
    tarifasChalanas = j.getString("tarifasChalanas"), recomendaciones = j.getString("recomendaciones"),
    telPolicia = j.getString("telPolicia"), telBomberos = j.getString("telBomberos"),
    telAmbulancia = j.getString("telAmbulancia"), telMigracion = j.getString("telMigracion"),
    actualizado = j.getLong("actualizado"), adminId = j.optString("adminId")
)

private fun climaAJson(c: Clima) = JSONObject()
    .put("temperatura", c.temperatura).put("sensacion", c.sensacion).put("humedad", c.humedad)
    .put("viento", c.viento).put("codigo", c.codigo ?: JSONObject.NULL).put("obtenido", c.obtenidoMs)
    .put("dias", JSONArray(c.dias.map { d ->
        JSONObject().put("fecha", d.fecha).put("max", d.maxima).put("min", d.minima).put("lluvia", d.probLluvia)
            .put("codigo", d.codigo ?: JSONObject.NULL).put("sale", d.salidaSol).put("pone", d.puestaSol)
    }))

private fun climaDeJson(j: JSONObject): Clima {
    val dias = j.getJSONArray("dias")
    return Clima(
        temperatura = j.getDouble("temperatura"), sensacion = j.getDouble("sensacion"), humedad = j.getInt("humedad"),
        viento = j.getDouble("viento"), codigo = if (j.isNull("codigo")) null else j.getInt("codigo"),
        obtenidoMs = j.getLong("obtenido"),
        dias = (0 until dias.length()).map { i ->
            val d = dias.getJSONObject(i)
            DiaClima(
                d.getString("fecha"), d.getDouble("max"), d.getDouble("min"), d.getInt("lluvia"),
                if (d.isNull("codigo")) null else d.getInt("codigo"), d.getString("sale"), d.getString("pone")
            )
        }
    )
}
