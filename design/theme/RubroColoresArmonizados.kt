package dev.justinianomarcelo.lucidanapp.util

import androidx.compose.ui.graphics.Color

// ============================================================
// Colores por rubro, ARMONIZADOS con el color de marca #632423
// mediante Blend.harmonize de material-color-utilities (Google).
// Cada rubro conserva su matiz reconocible (verde = farmacia,
// azul = transporte, etc.) pero rotado levemente hacia el maroon
// de marca, para que los marcadores del mapa y los placeholders
// se sientan parte de la misma familia visual.
//
// USO: fusionar estos valores en RubroVisual.kt (reemplazar los
// colores actuales por estos), manteniendo los íconos ya definidos.
// ============================================================

object RubroColoresArmonizados {
    val GASTRONOMIA = Color(0xFFEC5434)     // naranja cálido
    val FARMACIA_SALUD = Color(0xFF619920)  // verde
    val MODA_ROPA = Color(0xFFD83555)       // rosa/fucsia
    val CAMBIO_MONEDA = Color(0xFFFC8336)   // ámbar/dorado
    val TRANSPORTE = Color(0xFF496AC7)      // azul
    val ALOJAMIENTO = Color(0xFF47A45F)     // verde azulado
    val ABARROTES = Color(0xFF8D6E67)       // marrón
    val TECNOLOGIA = Color(0xFF6D59E6)      // índigo/violeta
    val SERVICIOS = Color(0xFF586D7E)       // gris azulado
    val OTRO = Color(0xFF888D97)            // gris neutro
}
