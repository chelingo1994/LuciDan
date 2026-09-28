package dev.justinianomarcelo.lucidanapp.util

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import java.io.File
import java.util.UUID

/**
 * Las fotos de un negocio se guardan como texto en `Negocio.fotos`. Ese texto puede ser:
 *  - una ruta de archivo dentro del almacenamiento interno de la app (foto elegida de la galería),
 *  - una URL http(s),
 *  - el nombre de un drawable incluido en la app (datos semilla).
 */
object FotoNegocio {
    private const val CARPETA = "fotos_negocio"

    /** Copia la imagen elegida al almacenamiento interno y devuelve su ruta, o null si falla. */
    fun guardarCopia(context: Context, origen: Uri): String? = runCatching {
        val carpeta = File(context.filesDir, CARPETA).apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return null
        destino.absolutePath
    }.getOrNull()

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

/** Muestra la foto de un negocio o lugar; no dibuja nada si no hay foto válida. */
@Composable
fun FotoNegocioImagen(
    valor: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val modelo = remember(valor) { FotoNegocio.modelo(context, valor) } ?: return
    AsyncImage(
        model = modelo,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
