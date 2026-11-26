package dev.justinianomarcelo.lucidanapp.data.remote

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.util.BeneficiosPlan
import dev.justinianomarcelo.lucidanapp.util.FotoNegocio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Resultado de leer una colección. [desdeServidor] es false si Firestore respondió solo con su caché
 * o si hay escrituras locales aún sin confirmar: en ambos casos la lista puede estar incompleta.
 */
data class InstantaneaNube<T>(val datos: List<T>, val desdeServidor: Boolean)

/**
 * Acceso a Firestore para los negocios. Los documentos usan como id el mismo `Long` que Room
 * (como texto). Las lecturas devuelven null si no hay red; las escrituras no esperan al servidor:
 * Firestore las guarda en disco y las reintenta solo cuando vuelve la conexión.
 */
class NubeService(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val negocios get() = firestore.collection(COLECCION_NEGOCIOS)

    suspend fun obtenerNegocios(): InstantaneaNube<Negocio>? =
        leer(COLECCION_NEGOCIOS) { it.aNegocio() }

    /**
     * Documento completo (alta o re-subida). Suspende porque convertir las fotos a texto es trabajo pesado:
     * se hace fuera del hilo principal.
     */
    suspend fun guardarNegocio(negocio: Negocio) {
        val mapa = withContext(Dispatchers.Default) { negocio.aMapa() }
        negocios.document(negocio.id.toString()).set(mapa)
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar el negocio ${negocio.id}", it) }
    }

    /**
     * Edición del perfil: escribe con merge y SIN los campos del plan ni el sello, para que un teléfono con
     * datos viejos no pise lo contratado o verificado en otro.
     */
    suspend fun actualizarPerfilNegocio(negocio: Negocio) {
        val mapa = withContext(Dispatchers.Default) { negocio.aMapa() } - CAMPOS_AJENOS_AL_PERFIL
        negocios.document(negocio.id.toString()).set(mapa, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "No se pudo actualizar el negocio ${negocio.id}", it) }
    }

    fun actualizarVerificado(negocioId: Long, verificado: Boolean) = actualizarCampo(negocioId, "verificado", verificado)

    /** Sube solo las fotos de un negocio ya existente (sin pisar el resto de sus campos). */
    suspend fun actualizarFotosNegocio(negocioId: Long, fotos: List<String>) {
        val textos = withContext(Dispatchers.Default) { fotos.paraNube() }
        actualizarCampo(negocioId, "fotos", textos)
    }

    fun actualizarEstadoNegocio(negocioId: Long, estado: EstadoNegocio) = actualizarCampo(negocioId, "estado", estado.name)

    fun actualizarPlanNegocio(negocioId: Long, destacado: Boolean, nivel: Int, hasta: Long) {
        negocios.document(negocioId.toString())
            .update(mapOf("destacado" to destacado, "planNivel" to nivel, "destacadoHasta" to hasta))
            .addOnFailureListener { Log.w(TAG, "No se pudo actualizar el plan del negocio $negocioId", it) }
    }

    fun eliminarNegocio(negocioId: Long) {
        negocios.document(negocioId.toString()).delete()
            .addOnFailureListener { Log.w(TAG, "No se pudo eliminar el negocio $negocioId", it) }
    }

    /** Todas las reseñas, o solo las de [negocioId]. Null si no hay red. */
    suspend fun obtenerResenas(negocioId: Long? = null): InstantaneaNube<Resena>? =
        leer(COLECCION_RESENAS, negocioId?.let { { c: CollectionReference -> c.whereEqualTo("negocioId", it) } }) { it.aResena() }

    /** Las reseñas no se editan ni se borran: el id (aleatorio, generado en la app) hace idempotente el reintento. */
    fun guardarResena(resena: Resena) {
        firestore.collection(COLECCION_RESENAS).document(resena.id.toString()).set(resena.aMapa())
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar la reseña ${resena.id}", it) }
    }

    private fun actualizarCampo(negocioId: Long, campo: String, valor: Any) {
        negocios.document(negocioId.toString()).update(campo, valor)
            .addOnFailureListener { Log.w(TAG, "No se pudo actualizar $campo del negocio $negocioId", it) }
    }

    private suspend fun <T> leer(
        coleccion: String,
        filtro: ((CollectionReference) -> Query)? = null,
        convertir: (DocumentSnapshot) -> T?
    ): InstantaneaNube<T>? =
        try {
            withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
                val referencia = firestore.collection(coleccion)
                val resultado = (filtro?.invoke(referencia) ?: referencia).get().await()
                InstantaneaNube(
                    datos = resultado.documents.mapNotNull(convertir),
                    desdeServidor = !resultado.metadata.isFromCache && !resultado.metadata.hasPendingWrites()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer $coleccion", e)
            null
        }

    companion object {
        const val COLECCION_NEGOCIOS = "negocios"
        const val COLECCION_RESENAS = "resenas"
        private const val TAG = "NubeService"
        private const val TIEMPO_MAXIMO_MS = 10_000L
        private val CAMPOS_AJENOS_AL_PERFIL = setOf("planNivel", "destacadoHasta", "verificado", "estado")
    }
}

/**
 * Las fotos con ruta de archivo son copias locales del teléfono y no sirven en otros dispositivos: se
 * convierten a texto Base64 para que viajen dentro del documento (sin Firebase Storage, que es de pago).
 */
private fun List<String>.paraNube(): List<String> {
    // Todas las fotos comparten el documento (máximo 1 MiB): cada una recibe una parte del espacio.
    val maximo = FotoNegocio.maximoTextoPorFoto(count { it.startsWith("/") })
    return mapNotNull { if (it.startsWith("/")) FotoNegocio.aTextoParaNube(it, maximo) else it }
}

private fun Negocio.aMapa() = mapOf(
    "nombre" to nombre,
    "categoria" to categoria.name,
    "descripcion" to descripcion,
    "telefono" to telefono,
    "direccion" to direccion,
    "latitud" to latitud,
    "longitud" to longitud,
    "horario" to horario,
    "fotos" to fotos.paraNube(),
    "estado" to estado.name,
    "destacado" to destacado,
    "planNivel" to planNivel,
    "destacadoHasta" to destacadoHasta,
    "facebook" to facebook,
    "instagram" to instagram,
    "tiktok" to tiktok,
    "ofertaTexto" to ofertaTexto,
    "ofertaHasta" to ofertaHasta,
    "verificado" to verificado,
    "duenoId" to duenoId
)

private fun Resena.aMapa() = mapOf(
    "negocioId" to negocioId,
    "autorId" to autorId,
    "calificacion" to calificacion,
    "comentario" to comentario,
    "fecha" to fecha.time
)

private fun DocumentSnapshot.aResena(): Resena? {
    val calificacion = getLong("calificacion")?.toInt()?.takeIf { it in 1..5 } ?: return null
    return Resena(
        id = id.toLongOrNull() ?: return null,
        autorId = getString("autorId").orEmpty(),
        negocioId = getLong("negocioId") ?: return null,
        calificacion = calificacion,
        comentario = getString("comentario").orEmpty(),
        fecha = Date(getLong("fecha") ?: return null)
    )
}

private inline fun <reified E : Enum<E>> String?.aEnum(): E? = enumValues<E>().firstOrNull { it.name == this }

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.fotos(): List<String> = (get("fotos") as? List<String>).orEmpty()

private fun DocumentSnapshot.aNegocio(): Negocio? {
    val id = id.toLongOrNull() ?: return null
    return Negocio(
        id = id,
        nombre = getString("nombre") ?: return null,
        // Los documentos anteriores al cambio de rubros traen categorías que ya no existen: caen en OTRO.
        categoria = Rubro.deNombre(getString("categoria")) ?: Rubro.OTRO,
        descripcion = getString("descripcion").orEmpty(),
        telefono = getString("telefono").orEmpty(),
        // "ubicacion" era el nombre del campo antes de alinearlo con el diagrama de clases.
        direccion = (getString("direccion") ?: getString("ubicacion")).orEmpty(),
        latitud = getDouble("latitud") ?: return null,
        longitud = getDouble("longitud") ?: return null,
        horario = getString("horario").orEmpty(),
        fotos = fotos(),
        estado = getString("estado").aEnum<EstadoNegocio>() ?: return null,
        destacado = getBoolean("destacado") ?: false,
        planNivel = getLong("planNivel")?.toInt() ?: 0,
        destacadoHasta = getLong("destacadoHasta") ?: 0L,
        facebook = getString("facebook").orEmpty(),
        instagram = getString("instagram").orEmpty(),
        tiktok = getString("tiktok").orEmpty(),
        ofertaTexto = getString("ofertaTexto").orEmpty().take(BeneficiosPlan.MAX_CARACTERES_OFERTA),
        ofertaHasta = getLong("ofertaHasta") ?: 0L,
        verificado = getBoolean("verificado") ?: false,
        duenoId = getString("duenoId")?.takeIf { it.isNotBlank() } ?: return null
    )
}
