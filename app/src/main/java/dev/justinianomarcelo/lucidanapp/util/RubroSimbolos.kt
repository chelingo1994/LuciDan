package dev.justinianomarcelo.lucidanapp.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.justinianomarcelo.lucidanapp.data.model.Rubro

/**
 * Fuente ÚNICA de los símbolos de rubro (viewBox 24x24, tomados de design/marcadores/disenos_marcadores_lucidan.html).
 * La usan los marcadores del mapa (`MarcadorIconos`, con Canvas) y el placeholder de las tarjetas
 * (`Rubro.icono`, como ImageVector), de modo que mapa y lista muestran exactamente los mismos dibujos.
 */
class FormaSimbolo(val datos: String, val relleno: Boolean = false)

/** Rectángulo redondeado como datos de path (para poder tratar todo como path). */
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" + "v${h - 2 * r}a$r $r 0 0 1 -$r $r" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 -$r -$r" + "v${-(h - 2 * r)}a$r $r 0 0 1 $r -${r}z"

private fun circulo(cx: Float, cy: Float, r: Float, relleno: Boolean = false) =
    FormaSimbolo("M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z", relleno)

val Rubro.formasSimbolo: List<FormaSimbolo>
    get() = when (this) {
        Rubro.GASTRONOMIA -> listOf(FormaSimbolo("M7 3v6a2.5 2.5 0 0 0 5 0V3M9.5 11v10M17 21V3c-2.5 1.5-3.5 4.5-3.5 8H17"))
        Rubro.FARMACIA_SALUD -> listOf(FormaSimbolo("M12 5v14M5 12h14"))
        Rubro.MODA_ROPA -> listOf(FormaSimbolo("M8.5 3 3.5 6l2 3.5 2.5-1.2V21h8V8.3l2.5 1.2 2-3.5-5-3c-.6 1.6-1.9 2.5-3.5 2.5S9.1 4.6 8.5 3z"))
        Rubro.CAMBIO_MONEDA -> listOf(FormaSimbolo("M4 8.5h14.5M15 5l3.5 3.5L15 12M20 15.5H5.5M9 12l-3.5 3.5L9 19"))
        Rubro.TRANSPORTE -> listOf(
            FormaSimbolo(rect(4.5f, 4f, 15f, 13f, 2.5f)),
            FormaSimbolo("M4.5 11h15"),
            circulo(8.5f, 19.5f, 1.3f, relleno = true),
            circulo(15.5f, 19.5f, 1.3f, relleno = true)
        )
        Rubro.ALOJAMIENTO -> listOf(
            FormaSimbolo("M3.5 18.5V6M3.5 14h17v4.5M20.5 14v-2.5a2.5 2.5 0 0 0-2.5-2.5h-7v5"),
            circulo(7.3f, 10.8f, 1.6f)
        )
        Rubro.ABARROTES -> listOf(FormaSimbolo("M3.5 10h17l-1.6 9.5H5.1L3.5 10zM8 10l3-6M16 10l-3-6"))
        Rubro.TECNOLOGIA -> listOf(
            FormaSimbolo(rect(3.5f, 4.5f, 17f, 11f, 1.8f)),
            FormaSimbolo("M9 20h6M12 15.5V20")
        )
        Rubro.SERVICIOS -> listOf(FormaSimbolo("M3.5 9.5 12 4.5l8.5 5zM5.5 11.5v6M9.8 11.5v6M14.2 11.5v6M18.5 11.5v6M3.5 20h17"))
        Rubro.OTRO -> listOf(
            FormaSimbolo("M4 9.5 5.5 4h13L20 9.5M4 9.5a2.7 2.7 0 0 0 5.3 0 2.7 2.7 0 0 0 5.4 0 2.7 2.7 0 0 0 5.3 0M5.5 12v8h13v-8M10 20v-5h4v5")
        )
    }

private val cacheIconos = HashMap<Rubro, ImageVector>()

/** Mismo símbolo que el marcador del mapa, como ImageVector de trazo (se tiñe con `tint`). */
val Rubro.icono: ImageVector
    get() = cacheIconos.getOrPut(this) {
        ImageVector.Builder(
            name = "Rubro_$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            formasSimbolo.forEach { forma ->
                if (forma.relleno) {
                    addPath(pathData = addPathNodes(forma.datos), fill = SolidColor(Color.White))
                } else {
                    addPath(
                        pathData = addPathNodes(forma.datos), stroke = SolidColor(Color.White), strokeLineWidth = 2.3f,
                        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
                    )
                }
            }
        }.build()
    }
