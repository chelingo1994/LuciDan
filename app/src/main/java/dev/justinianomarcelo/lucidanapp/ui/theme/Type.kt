package dev.justinianomarcelo.lucidanapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Jerarquía con 4 tamaños (28 / 18 / 14 / 12) y 2 pesos (Normal / SemiBold),
// una sola familia. Los estilos de Material que no se usan mapean al tamaño
// más cercano para que cualquier componente nuevo herede la misma escala.
private val Familia = FontFamily.Default

private fun estilo(tamano: Int, peso: FontWeight, interlineado: Int, espaciado: Double = 0.0) = TextStyle(
    fontFamily = Familia,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = interlineado.sp,
    letterSpacing = espaciado.sp
)

val Typography = Typography(
    headlineLarge = estilo(28, FontWeight.SemiBold, 36),
    headlineMedium = estilo(28, FontWeight.SemiBold, 36),
    headlineSmall = estilo(18, FontWeight.SemiBold, 24),
    titleLarge = estilo(18, FontWeight.SemiBold, 24),
    titleMedium = estilo(18, FontWeight.SemiBold, 24),
    titleSmall = estilo(14, FontWeight.SemiBold, 20, 0.1),
    bodyLarge = estilo(16, FontWeight.Normal, 24, 0.2),
    bodyMedium = estilo(14, FontWeight.Normal, 20, 0.2),
    bodySmall = estilo(12, FontWeight.Normal, 16, 0.2),
    labelLarge = estilo(14, FontWeight.SemiBold, 20, 0.1),
    labelMedium = estilo(12, FontWeight.SemiBold, 16, 0.3),
    labelSmall = estilo(12, FontWeight.SemiBold, 16, 0.3)
)
