package dev.justinianomarcelo.lucidanapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Las fotos de un negocio se guardan como texto en `Negocio.fotos`. Ese texto puede ser:
 *  - una ruta de archivo dentro del almacenamiento interno de la app (foto elegida de la galería),
 *  - una URL http(s),
 *  - el nombre de un drawable incluido en la app (datos semilla).
 */
object FotoNegocio {
    private const val CARPETA = "fotos_negocio"
    private const val TAG = "FotoNegocio"
    private const val LADO_MAXIMO = 1280
    private const val CALIDAD_JPEG = 80

    /**
     * Guarda una copia reducida de la imagen elegida en el almacenamiento interno y devuelve su ruta, o
     * null si falla. La foto original de la cámara pesa varios MB: se baja a [LADO_MAXIMO] px por el lado
     * más largo y se comprime a JPEG, respetando la rotación que trae la cámara.
     */
    fun guardarCopia(context: Context, origen: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Con inJustDecodeBounds decodeStream devuelve siempre null (solo llena las medidas en `limites`):
        // el éxito se mide con lo que devuelve use { }, no con el bitmap.
        resolver.openInputStream(origen)?.use { BitmapFactory.decodeStream(it, null, limites); true }
            ?: return null.also { Log.w(TAG, "No se pudo abrir $origen") }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            Log.w(TAG, "Formato no soportado por BitmapFactory (${resolver.getType(origen)}): $origen")
            return null
        }

        // Decodifica ya reducida (potencias de 2) para no cargar la foto completa en memoria.
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo(limites.outWidth, limites.outHeight) }
        val decodificada = resolver.openInputStream(origen)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: return null
        val rotacion = resolver.openInputStream(origen)?.use { gradosDeRotacion(it) } ?: 0
        val final = ajustar(decodificada, rotacion)

        val carpeta = File(context.filesDir, CARPETA).apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.jpg")
        destino.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, it) }
        final.recycle()
        destino.absolutePath
    }.onFailure { Log.w(TAG, "No se pudo copiar la foto $origen", it) }.getOrNull()

    private fun muestreo(ancho: Int, alto: Int): Int {
        var muestreo = 1
        while (maxOf(ancho, alto) / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
        return muestreo
    }

    private fun gradosDeRotacion(entrada: InputStream): Int =
        when (ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** Escala a [LADO_MAXIMO], aplica la rotación y rellena de blanco el fondo transparente (JPEG no lo soporta). */
    private fun ajustar(original: Bitmap, rotacion: Int): Bitmap {
        val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(original.width, original.height))
        val matriz = Matrix().apply {
            postRotate(rotacion.toFloat())
            postScale(escala, escala)
        }
        val transformada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
        if (transformada !== original) original.recycle()
        if (!transformada.hasAlpha()) return transformada
        val sobreBlanco = Bitmap.createBitmap(transformada.width, transformada.height, Bitmap.Config.RGB_565)
        Canvas(sobreBlanco).apply {
            drawColor(Color.WHITE)
            drawBitmap(transformada, 0f, 0f, null)
        }
        transformada.recycle()
        return sobreBlanco
    }

    // --- Fotos en la nube como texto (Base64) -------------------------------------------------------
    // Firebase Storage es de pago, así que la foto viaja dentro del documento del negocio en Firestore
    // como "data:image/jpeg;base64,...". Un documento admite 1 MiB: por eso se reduce mucho.

    private const val PREFIJO_DATOS = "data:image/jpeg;base64,"
    private const val LADO_NUBE = 800
    private const val MAX_TEXTO_NUBE = 600_000
    private const val MAX_TEXTO_DOCUMENTO = 900_000

    /** Con varias fotos el documento se reparte entre ellas; con una sola sigue el tope de siempre. */
    fun maximoTextoPorFoto(cantidad: Int): Int =
        minOf(MAX_TEXTO_NUBE, MAX_TEXTO_DOCUMENTO / cantidad.coerceAtLeast(1))

    @Volatile
    private var carpetaFotos: File? = null

    /** Se llama al abrir la base de datos: guarda dónde dejar las fotos que bajan de la nube. */
    fun iniciar(context: Context) {
        carpetaFotos = File(context.filesDir, CARPETA).apply { mkdirs() }
    }

    /** Foto local (ruta de archivo) -> texto Base64 para Firestore; null si no existe o no cabe en el documento. */
    fun aTextoParaNube(ruta: String, maximo: Int = MAX_TEXTO_NUBE): String? = runCatching {
        if (!File(ruta).exists()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, limites)
        if (limites.outWidth <= 0) return null
        var muestreo = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestreo * 2) >= LADO_NUBE) muestreo *= 2
        val original = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestreo })
            ?: return null
        val escala = minOf(1f, LADO_NUBE.toFloat() / maxOf(original.width, original.height))
        val bitmap = if (escala < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * escala).toInt(), (original.height * escala).toInt(), true)
        } else original
        if (bitmap !== original) original.recycle()
        val salida = java.io.ByteArrayOutputStream()
        var calidad = 70
        do {
            salida.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            calidad -= 10
        } while (salida.size() * 4 / 3 > maximo && calidad >= 30)
        bitmap.recycle()
        if (salida.size() * 4 / 3 > maximo) return null
        PREFIJO_DATOS + android.util.Base64.encodeToString(salida.toByteArray(), android.util.Base64.NO_WRAP)
    }.onFailure { Log.w(TAG, "No se pudo convertir la foto $ruta", it) }.getOrNull()

    /**
     * Valor de `fotos` venido de la nube -> valor local. Un texto Base64 se guarda como archivo (con nombre
     * derivado de su contenido, así no se repite en cada sincronización) y se devuelve su ruta; lo demás
     * (URL, nombre de drawable) pasa igual. Null si no se pudo convertir.
     */
    fun deTextoDeNube(valor: String): String? {
        if (!valor.startsWith(PREFIJO_DATOS)) return valor
        val carpeta = carpetaFotos ?: return null
        return runCatching {
            val huella = java.security.MessageDigest.getInstance("SHA-256").digest(valor.toByteArray())
                .joinToString("") { "%02x".format(it) }.take(32)
            val destino = File(carpeta, "nube_$huella.jpg")
            if (!destino.exists()) {
                destino.writeBytes(android.util.Base64.decode(valor.removePrefix(PREFIJO_DATOS), android.util.Base64.DEFAULT))
            }
            destino.absolutePath
        }.onFailure { Log.w(TAG, "No se pudo guardar la foto de la nube", it) }.getOrNull()
    }

    /** Borra la foto solo si es una copia propia de la app; ignora URLs y drawables. */
    fun eliminarCopia(context: Context, valor: String?) {
        if (valor.isNullOrBlank()) return
        val archivo = File(valor)
        if (archivo.parentFile?.name == CARPETA && archivo.startsWith(context.filesDir)) archivo.delete()
    }

    /** Modelo que Coil sabe cargar, o null si no hay nada que mostrar. */
    fun modelo(context: Context, valor: String?): Any? {
        if (valor.isNullOrBlank()) return null
        if (valor.startsWith("http://") || valor.startsWith("https://")) return valor
        val archivo = File(valor)
        if (archivo.isAbsolute) return archivo.takeIf { it.exists() }
        return context.resources.getIdentifier(valor, "drawable", context.packageName).takeIf { it != 0 }
    }
}

/**
 * Muestra la foto de un negocio. Si no hay foto válida dibuja el placeholder del [rubro] (color + ícono);
 * sin [rubro] no dibuja nada.
 */
@Composable
fun FotoNegocioImagen(
    valor: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    rubro: Rubro? = null
) {
    val context = LocalContext.current
    val modelo = remember(valor) { FotoNegocio.modelo(context, valor) }
    if (modelo == null) {
        if (rubro != null) PlaceholderRubro(rubro, modifier)
        return
    }
    AsyncImage(
        model = modelo,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
