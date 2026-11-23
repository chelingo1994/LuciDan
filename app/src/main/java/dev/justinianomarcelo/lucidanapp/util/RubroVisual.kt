package dev.justinianomarcelo.lucidanapp.util

import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.justinianomarcelo.lucidanapp.data.model.Rubro

/**
 * Color del rubro, armonizado con el maroon de marca (#632423) con Blend.harmonize de
 * material-color-utilities: cada rubro conserva su matiz reconocible pero rotado hacia la marca.
 * Es fijo (no cambia con el tema claro/oscuro) para que marcador del mapa y placeholder coincidan.
 */
val Rubro.color: Color
    get() = when (this) {
        Rubro.GASTRONOMIA -> Color(0xFFEC5434)     // naranja cálido
        Rubro.FARMACIA_SALUD -> Color(0xFF619920)  // verde
        Rubro.MODA_ROPA -> Color(0xFFD83555)       // rosa/fucsia
        Rubro.CAMBIO_MONEDA -> Color(0xFFFC8336)   // ámbar/dorado
        Rubro.TRANSPORTE -> Color(0xFF496AC7)      // azul
        Rubro.ALOJAMIENTO -> Color(0xFF47A45F)     // verde azulado
        Rubro.ABARROTES -> Color(0xFF8D6E67)       // marrón
        Rubro.TECNOLOGIA -> Color(0xFF6D59E6)      // índigo/violeta
        Rubro.SERVICIOS -> Color(0xFF586D7E)       // gris azulado
        Rubro.OTRO -> Color(0xFF888D97)            // gris neutro
    }

/** Fondo de color del rubro con su ícono: lo que se ve cuando el negocio no tiene foto. */
@Composable
fun PlaceholderRubro(rubro: Rubro, modifier: Modifier = Modifier, tamanoIcono: Dp = 28.dp) {
    Box(
        modifier = modifier.background(rubro.color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        // El color puro del rubro no llega a 3:1 contra su propio fondo tintado en varios rubros (p. ej. ámbar
        // 2,1:1 en claro): se mezcla 35% con onSurface para el ícono. El fondo y el marcador usan el color puro.
        Icon(
            rubro.icono,
            contentDescription = null, // decorativo: el rubro ya se lee como texto junto a la foto
            tint = lerp(rubro.color, MaterialTheme.colorScheme.onSurface, 0.35f),
            modifier = Modifier.size(tamanoIcono)
        )
    }
}
