package dev.justinianomarcelo.lucidanapp.data.model

/**
 * Tipo de cambio publicado por LuciDan (Firestore `info_util/cambio`). Tasas en **ARS por 1 Bs**:
 * [compraArsPorBs] es lo que se recibe al cambiar Bs → ARS y [ventaArsPorBs] lo que cuesta cada Bs
 * al cambiar ARS → Bs (`compra <= venta`). [actualizado] es epoch en milisegundos y [adminId] el hash
 * del correo del administrador que la editó (mismo formato que `Negocio.duenoId`).
 */
data class TasaCambio(
    val compraArsPorBs: Double,
    val ventaArsPorBs: Double,
    val actualizado: Long,
    val adminId: String
)

/** Cruce internacional ARS por 1 Bs calculado con open.er-api.com; solo informativo. [fechaMs] es la del dato. */
data class ReferenciaCambio(val arsPorBs: Double, val fechaMs: Long)

/** Información del paso fronterizo (Firestore `info_util/frontera`). Todo texto libre, editable por el administrador. */
data class InfoFrontera(
    val horarioPaso: String = POR_CONFIRMAR,
    val horarioChalanas: String = POR_CONFIRMAR,
    val tarifasChalanas: String = POR_CONFIRMAR,
    val recomendaciones: String = POR_CONFIRMAR,
    val telPolicia: String = POR_CONFIRMAR,
    val telBomberos: String = POR_CONFIRMAR,
    val telAmbulancia: String = POR_CONFIRMAR,
    val telMigracion: String = POR_CONFIRMAR,
    val actualizado: Long = 0L,
    val adminId: String = ""
) {
    companion object {
        const val POR_CONFIRMAR = "Por confirmar"
    }
}

data class DiaClima(
    val fecha: String,
    val maxima: Double,
    val minima: Double,
    val probLluvia: Int,
    val codigo: Int?,
    val salidaSol: String,
    val puestaSol: String
)

/** Clima actual y pronóstico de 3 días (Open-Meteo). [obtenidoMs] es cuándo se descargó. */
data class Clima(
    val temperatura: Double,
    val sensacion: Double,
    val humedad: Int,
    val viento: Double,
    val codigo: Int?,
    val dias: List<DiaClima>,
    val obtenidoMs: Long
)
