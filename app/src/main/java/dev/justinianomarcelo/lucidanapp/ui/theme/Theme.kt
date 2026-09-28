package dev.justinianomarcelo.lucidanapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Regla 60/30/10: fondo gris claro (60), superficies crema y texto oscuro (30),
// maroon solo en acciones e indicadores clave (10). El texto secundario usa
// variaciones de opacidad del color de texto, no otro color.
private val DarkColorScheme = darkColorScheme(
    primary = MaroonLight,
    onPrimary = MaroonDark,
    primaryContainer = Color(0xFF4A2220),
    onPrimaryContainer = CreamAccent,
    secondary = MaroonLight,
    tertiary = CreamAccent,
    background = Color(0xFF1C1412),
    surface = Color(0xFF241917),
    surfaceVariant = Color(0xFF33262A),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFF9F8C89),
    outlineVariant = Color(0xFF4F4342),
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E1DE),
    onPrimaryContainer = MaroonDark,
    secondary = MaroonLight,
    tertiary = MaroonDark,
    background = LightGrayBackground,
    surface = CreamAccent,
    surfaceVariant = Color(0xFFF1E4DE),
    onBackground = Color(0xFF2B1D14),
    onSurface = Color(0xFF2B1D14),
    onSurfaceVariant = Color(0xB32B1D14),
    outline = Color(0xFF8A7772),
    outlineVariant = Color(0xFFE6D6D0),
    error = ErrorRed
)

// Esquinas generosas y consistentes: chips/campos 12, tarjetas 20, hojas/diálogos 28.
private val LuciDanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LuciDanAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // La marca LuciDan usa una paleta fija (maroon/crema); no se usa color
    // dinámico para mantener la identidad visual del proyecto.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = LuciDanShapes,
        content = content
    )
}
