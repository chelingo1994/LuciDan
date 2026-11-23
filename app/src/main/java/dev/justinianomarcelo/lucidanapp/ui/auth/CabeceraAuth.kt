package dev.justinianomarcelo.lucidanapp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.justinianomarcelo.lucidanapp.R

/** Color de fondo del logo horizontal (logo_lucidan.png es opaco, con fondo casi blanco). */
private val FondoLogo = Color(0xFFFEFEFE)

/**
 * Cabecera centrada de las pantallas de acceso: el logo horizontal de LuciDan (símbolo + nombre), un título opcional
 * y una línea de apoyo. El logo es texto granate sobre fondo claro, así que va dentro de una tarjeta clara fija: en
 * modo oscuro se ve como una placa clara, en vez de perderse sobre la superficie oscura.
 */
@Composable
fun CabeceraAuth(
    titulo: String?,
    subtitulo: String,
    modifier: Modifier = Modifier,
    anchoLogo: Dp = 240.dp,
    estiloTitulo: TextStyle = MaterialTheme.typography.headlineLarge
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            color = FondoLogo,
            modifier = Modifier.width(anchoLogo)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_lucidan),
                contentDescription = "LuciDan, negocios de Bermejo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        if (titulo != null) {
            Text(
                text = titulo,
                style = estiloTitulo,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (titulo != null) 8.dp else 16.dp)
        )
    }
}
