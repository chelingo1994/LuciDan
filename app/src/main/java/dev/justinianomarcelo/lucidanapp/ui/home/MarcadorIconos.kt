package dev.justinianomarcelo.lucidanapp.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.util.color
import dev.justinianomarcelo.lucidanapp.util.formasSimbolo
import kotlin.math.cos
import kotlin.math.sin

/**
 * Íconos de marcador del mapa dibujados con Canvas/Path (sin imágenes externas). Transcripción del
 * ESTILO A de `design/marcadores/disenos_marcadores_lucidan.html`: pin en gota con borde blanco, símbolo
 * del rubro en blanco, variantes normal / seleccionado (x1,3 con halo) / destacado (insignia dorada).
 *
 * Todo se dibuja en el espacio de diseño del HTML (pin de 36x46, punta en (18,45)) y 1 unidad = 1 dp.
 * Los bitmaps se guardan en un LruCache por rubro+variante+densidad: cada marcador recibe su propio
 * [BitmapDrawable] (el bitmap es de solo lectura y se comparte; el drawable no), así que nunca se tiñe
 * ni se comparte el ícono por defecto de osmdroid.
 */
object MarcadorIconos {

    // Lienzo del pin: 48x54 dp con el origen del diseño en (-6,-7), para que quepan la insignia, el halo
    // y la sombra, y para que la punta (18,45) quede centrada horizontalmente (ancla X = 0,5).
    private const val ANCHO = 48f
    private const val ALTO = 54f
    private const val DESPL_X = 6f
    private const val DESPL_Y = 7f
    private const val PUNTA_X = 18f
    private const val PUNTA_Y = 45f

    /** Ancla vertical del bitmap del pin: la punta está en esta fracción de la altura. */
    const val ANCLA_X = 0.5f
    const val ANCLA_Y = (PUNTA_Y + DESPL_Y) / ALTO

    private const val ESCALA_SELECCIONADO = 1.3f
    private const val COLOR_INSIGNIA = 0xFFF5A100.toInt()
    private const val COLOR_GRUPO = 0xFF7E2A2A.toInt()
    private const val COLOR_UBICACION = 0xFF1A73E8.toInt()

    // ~8 MB: 10 rubros x 4 variantes a 3x de densidad ocupan unos 5 MB.
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun cacheado(clave: String, dibujar: () -> Bitmap): Bitmap =
        cache.get(clave) ?: dibujar().also { cache.put(clave, it) }

    // ---- API pública -------------------------------------------------------------------------------------

    /** Drawable propio (nuevo en cada llamada) del pin del [rubro]; usar con `setAnchor(ANCLA_X, ANCLA_Y)`. */
    fun pin(context: Context, rubro: Rubro, seleccionado: Boolean, destacado: Boolean): BitmapDrawable {
        val densidad = context.resources.displayMetrics.density
        val clave = "pin|${rubro.name}|$seleccionado|$destacado|$densidad"
        val bitmap = cacheado(clave) { dibujarPin(rubro, seleccionado, destacado, densidad) }
        return BitmapDrawable(context.resources, bitmap)
    }

    /** Mira con círculo discontinuo y punto central, para el "punto de búsqueda" (ancla en el centro). */
    fun puntoElegido(context: Context, color: Int): BitmapDrawable {
        val densidad = context.resources.displayMetrics.density
        val bitmap = cacheado("punto|$color|$densidad") { dibujarPuntoElegido(color, densidad) }
        return BitmapDrawable(context.resources, bitmap)
    }

    /** Punto azul con borde blanco y halo translúcido (ancla en el centro). */
    fun miUbicacion(context: Context): Bitmap {
        val densidad = context.resources.displayMetrics.density
        return cacheado("yo|$densidad") { dibujarMiUbicacion(densidad) }
    }

    /** Círculo granate con halo para los grupos de negocios; el número lo dibuja el clusterer encima (ancla centro). */
    fun grupo(context: Context): Bitmap {
        val densidad = context.resources.displayMetrics.density
        return cacheado("grupo|$densidad") { dibujarGrupo(densidad) }
    }

    // ---- Pin ---------------------------------------------------------------------------------------------

    private fun dibujarPin(rubro: Rubro, seleccionado: Boolean, destacado: Boolean, densidad: Float): Bitmap {
        val escala = if (seleccionado) ESCALA_SELECCIONADO else 1f
        val k = densidad * escala
        val bitmap = Bitmap.createBitmap(
            Math.ceil((ANCHO * k).toDouble()).toInt(),
            Math.ceil((ALTO * k).toDouble()).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(k, k)
        canvas.translate(DESPL_X, DESPL_Y)
        val color = rubro.color.toArgb()

        if (seleccionado) { // halo del color del rubro en la base
            val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = (0.28f * 255).toInt() }
            canvas.drawOval(RectF(PUNTA_X - 15f, PUNTA_Y - 5f, PUNTA_X + 15f, PUNTA_Y + 5f), halo)
        }

        // Cuerpo en gota: "M18 45C18 45 3 29 3 17a15 15 0 0 1 30 0c0 12-15 28-15 28z"
        val gota = Path().apply {
            moveTo(18f, 45f)
            cubicTo(18f, 45f, 3f, 29f, 3f, 17f)
            arcTo(RectF(3f, 2f, 33f, 32f), 180f, 180f)
            cubicTo(33f, 29f, 18f, 45f, 18f, 45f)
            close()
        }
        // La sombra (suave, hacia abajo) va en el relleno; el borde blanco se dibuja encima sin sombra.
        canvas.drawPath(gota, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
            setShadowLayer(2.5f * k, 0f, 1.5f * k, 0x4D000000)
        })
        canvas.drawPath(gota, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = 0xFFFFFFFF.toInt()
            strokeWidth = 2.5f
            strokeJoin = Paint.Join.ROUND
        })

        // Símbolo del rubro: translate(9.4 8.4) scale(.72), trazo blanco de 2,3 redondeado
        canvas.save()
        canvas.translate(9.4f, 8.4f)
        canvas.scale(0.72f, 0.72f)
        dibujarSimbolo(canvas, rubro)
        canvas.restore()

        if (destacado) dibujarInsignia(canvas, 31f, 4f)
        return bitmap
    }

    private fun dibujarInsignia(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawCircle(cx, cy, 7.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INSIGNIA })
        canvas.drawCircle(cx, cy, 7.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 2f
        })
        val estrella = Path()
        for (i in 0 until 10) {
            val a = -Math.PI / 2 + i * Math.PI / 5
            val r = if (i % 2 == 1) 4.4f * 0.45f else 4.4f
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) estrella.moveTo(x, y) else estrella.lineTo(x, y)
        }
        estrella.close()
        canvas.drawPath(estrella, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    }

    // ---- Símbolos de rubro (objeto R del HTML, viewBox 24x24) ------------------------------------------

    // Los símbolos viven en util/RubroSimbolos.kt (compartidos con el placeholder de las tarjetas).
    // Los Path se parsean una sola vez.
    private val pathsParseados = HashMap<String, Path>()

    private fun path(datos: String): Path =
        pathsParseados.getOrPut(datos) { PathParser.createPathFromPathData(datos) }

    private fun dibujarSimbolo(canvas: Canvas, rubro: Rubro) {
        val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 2.3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        rubro.formasSimbolo.forEach { forma ->
            canvas.drawPath(path(forma.datos), if (forma.relleno) relleno else trazo)
        }
    }

    // ---- Punto elegido y mi ubicación ------------------------------------------------------------------

    private fun dibujarPuntoElegido(color: Int, densidad: Float): Bitmap {
        val lado = 60f
        val bitmap = Bitmap.createBitmap((lado * densidad).toInt(), (lado * densidad).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(densidad, densidad)
        val c = lado / 2
        canvas.drawCircle(c, c, 19f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = (0.12f * 255).toInt() })
        canvas.drawCircle(c, c, 19f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = 1.6f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        })
        val mira = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(c - 26f, c, c - 12f, c, mira)
        canvas.drawLine(c + 12f, c, c + 26f, c, mira)
        canvas.drawLine(c, c - 26f, c, c - 12f, mira)
        canvas.drawLine(c, c + 12f, c, c + 26f, mira)
        canvas.drawCircle(c, c, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.drawCircle(c, c, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = 0xFFFFFFFF.toInt()
            strokeWidth = 2.2f
        })
        return bitmap
    }

    private fun dibujarMiUbicacion(densidad: Float): Bitmap {
        val lado = 40f
        val bitmap = Bitmap.createBitmap((lado * densidad).toInt(), (lado * densidad).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(densidad, densidad)
        val c = lado / 2
        canvas.drawCircle(c, c, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_UBICACION; alpha = (0.18f * 255).toInt() })
        canvas.drawCircle(c, c, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_UBICACION
            setShadowLayer(2f * densidad, 0f, densidad, 0x4D000000)
        })
        canvas.drawCircle(c, c, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 3f
        })
        return bitmap
    }

    private fun dibujarGrupo(densidad: Float): Bitmap {
        val lado = 50f
        val bitmap = Bitmap.createBitmap((lado * densidad).toInt(), (lado * densidad).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(densidad, densidad)
        val c = lado / 2
        canvas.drawCircle(c, c, 21f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRUPO; alpha = (0.25f * 255).toInt() })
        canvas.drawCircle(c, c, 15f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRUPO
            setShadowLayer(2.5f * densidad, 0f, 1.5f * densidad, 0x4D000000)
        })
        canvas.drawCircle(c, c, 15f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 2.5f
        })
        return bitmap
    }
}
