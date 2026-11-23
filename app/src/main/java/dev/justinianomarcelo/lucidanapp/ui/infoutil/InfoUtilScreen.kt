package dev.justinianomarcelo.lucidanapp.ui.infoutil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.model.Clima
import dev.justinianomarcelo.lucidanapp.data.model.DiaClima
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.util.Cambio
import dev.justinianomarcelo.lucidanapp.util.ClimaCodigos
import dev.justinianomarcelo.lucidanapp.util.Contacto
import dev.justinianomarcelo.lucidanapp.util.IconoClima
import dev.justinianomarcelo.lucidanapp.util.Tiempo
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val LOCALE_BO = Locale("es", "BO")

/** HU16 (tipo de cambio) y HU17 (paso fronterizo) de la EPIC07, más el clima de Bermejo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoUtilScreen(
    onVolver: () -> Unit,
    onEditarFrontera: () -> Unit,
    viewModel: InfoUtilViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val usuario by SessionManager.usuarioActual.collectAsState()
    val esAdmin = usuario?.rol == RolUsuario.ADMINISTRADOR
    val snackbarHostState = remember { SnackbarHostState() }
    val cargando = estado.cargandoCambio || estado.cargandoClima || estado.cargandoFrontera

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it, shape = MaterialTheme.shapes.small) } },
        topBar = {
            TopAppBar(
                title = { Text("Info útil") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (cargando) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = viewModel::actualizarTodo) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TarjetaCambio(estado, esAdmin, viewModel) }
            item { TarjetaClima(estado, onReintentar = { viewModel.cargarClima(forzar = true) }) }
            item { TarjetaFrontera(estado, esAdmin, onEditarFrontera) }
        }
    }

    if (estado.dialogoTasaAbierto) {
        DialogoTasa(
            compraInicial = estado.cambio?.compraArsPorBs?.let { formatoEditable(it) }.orEmpty(),
            ventaInicial = estado.cambio?.ventaArsPorBs?.let { formatoEditable(it) }.orEmpty(),
            error = estado.errorTasa,
            guardando = estado.guardandoTasa,
            onGuardar = viewModel::guardarTasa,
            onCancelar = viewModel::cerrarDialogoTasa
        )
    }
}

@Composable
private fun Tarjeta(titulo: String, icono: ImageVector, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(titulo, style = MaterialTheme.typography.titleMedium)
            }
            contenido()
        }
    }
}

@Composable
private fun Nota(texto: String) =
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

private fun monto(valor: Double) = String.format(LOCALE_BO, "%,.2f", valor)

/** Para editar: punto decimal y sin separador de miles. */
private fun formatoEditable(valor: Double) = String.format(Locale.US, "%.2f", valor)

// --- Cambio de moneda ------------------------------------------------------------------------------------

@Composable
private fun TarjetaCambio(estado: InfoUtilUiState, esAdmin: Boolean, viewModel: InfoUtilViewModel) {
    Tarjeta("Cambio de moneda", Icons.Filled.CurrencyExchange) {
        val cambio = estado.cambio
        if (cambio != null) {
            FilaDato("Compra (Bs → ARS)", "1 Bs = ${monto(cambio.compraArsPorBs)} ARS")
            FilaDato("Venta (ARS → Bs)", "1 Bs = ${monto(cambio.ventaArsPorBs)} ARS")
            Nota(
                if (estado.cambioDesdeCache) "Sin conexión. Última actualización: ${Tiempo.fechaHora(cambio.actualizado)}"
                else "Actualizado el ${Tiempo.fechaHora(cambio.actualizado)}"
            )
        } else {
            Text("Aún no hay una tasa publicada por LuciDan.", style = MaterialTheme.typography.bodyMedium)
        }
        estado.referencia?.let {
            Nota("Referencia internacional: 1 Bs ≈ ${monto(it.arsPorBs)} ARS (dato del ${Tiempo.fechaHora(it.fechaMs)})")
            Nota("Datos: ExchangeRate-API")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Calculadora(estado, viewModel)
        Nota("Tasa referencial publicada por LuciDan. Confirma el valor en la casa de cambio.")
        if (esAdmin) {
            OutlinedButton(onClick = viewModel::abrirDialogoTasa, modifier = Modifier.fillMaxWidth()) {
                Text("Actualizar tasa")
            }
        }
    }
}

@Composable
private fun Calculadora(estado: InfoUtilUiState, viewModel: InfoUtilViewModel) {
    val bsAArs = estado.sentido == Cambio.Sentido.BS_A_ARS
    val tasa = estado.tasaAplicable
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = estado.monto,
            onValueChange = viewModel::onMontoCambiado,
            label = { Text(if (bsAArs) "Monto en Bs" else "Monto en ARS") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = viewModel::invertirSentido) {
            Icon(Icons.Filled.SwapVert, contentDescription = "Invertir sentido: ahora ${if (bsAArs) "de Bs a ARS" else "de ARS a Bs"}")
        }
    }
    when {
        tasa == null -> Nota("No hay ninguna tasa disponible para calcular. Conéctate a internet e intenta de nuevo.")
        else -> {
            val resultado = estado.resultado
            Text(
                text = if (resultado == null) "Escribe un monto para convertir"
                else "${monto(Cambio.parsearNumero(estado.monto) ?: 0.0)} ${if (bsAArs) "Bs" else "ARS"} = ${monto(resultado)} ${if (bsAArs) "ARS" else "Bs"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Nota(
                when {
                    tasa.origen == OrigenTasa.REFERENCIA ->
                        "Se usó la referencia internacional (1 Bs ≈ ${monto(tasa.compra)} ARS) porque LuciDan aún no publicó una tasa."
                    bsAArs -> "Se aplicó la tasa de compra: 1 Bs = ${monto(tasa.compra)} ARS."
                    else -> "Se aplicó la tasa de venta: 1 Bs = ${monto(tasa.venta)} ARS."
                }
            )
        }
    }
}

@Composable
private fun DialogoTasa(
    compraInicial: String,
    ventaInicial: String,
    error: String?,
    guardando: Boolean,
    onGuardar: (String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var compra by remember { mutableStateOf(compraInicial) }
    var venta by remember { mutableStateOf(ventaInicial) }
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Actualizar tasa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Nota("Escribe cuántos pesos argentinos (ARS) equivalen a 1 Bs. La compra no puede ser mayor que la venta.")
                OutlinedTextField(
                    value = compra, onValueChange = { compra = it }, label = { Text("Compra (ARS por 1 Bs)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = venta, onValueChange = { venta = it }, label = { Text("Venta (ARS por 1 Bs)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(compra, venta) }, enabled = !guardando) { Text(if (guardando) "Guardando..." else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } }
    )
}

// --- Clima ------------------------------------------------------------------------------------------------

private fun IconoClima.vector(): ImageVector = when (this) {
    IconoClima.SOL -> Icons.Filled.WbSunny
    IconoClima.PARCIAL -> Icons.Filled.WbCloudy
    IconoClima.NUBLADO -> Icons.Filled.Cloud
    IconoClima.NIEBLA -> Icons.Filled.BlurOn
    IconoClima.LLUVIA -> Icons.Filled.WaterDrop
    IconoClima.TORMENTA -> Icons.Filled.Thunderstorm
    IconoClima.NIEVE -> Icons.Filled.AcUnit
    IconoClima.DESCONOCIDO -> Icons.Filled.HelpOutline
}

private fun grados(valor: Double) = "${valor.roundToInt()}°"

@Composable
private fun TarjetaClima(estado: InfoUtilUiState, onReintentar: () -> Unit) {
    Tarjeta("Clima en Bermejo", Icons.Filled.WbSunny) {
        val clima = estado.clima
        when {
            clima != null -> ContenidoClima(clima, estado.climaDesdeCache)
            estado.cargandoClima -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Cargando el clima...", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text("No se pudo obtener el clima. Verifica tu conexión a internet.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun ContenidoClima(clima: Clima, desdeCache: Boolean) {
    val condicion = ClimaCodigos.deCodigo(clima.codigo)
    val hoy = clima.dias.firstOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(condicion.icono.vector(), contentDescription = condicion.descripcion, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(grados(clima.temperatura), style = MaterialTheme.typography.displayMedium)
            Text(condicion.descripcion, style = MaterialTheme.typography.titleSmall)
        }
    }
    Text(
        "Sensación ${grados(clima.sensacion)} · Humedad ${clima.humedad}% · Viento ${clima.viento.roundToInt()} km/h",
        style = MaterialTheme.typography.bodyMedium
    )
    if (hoy != null) {
        Text(
            "Hoy: mínima ${grados(hoy.minima)} · máxima ${grados(hoy.maxima)} · lluvia ${hoy.probLluvia}%",
            style = MaterialTheme.typography.bodyMedium
        )
        Text("Sale el sol ${hoy.salidaSol} · Se pone ${hoy.puestaSol}", style = MaterialTheme.typography.bodyMedium)
    }
    if (clima.dias.isNotEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            clima.dias.forEachIndexed { indice, dia -> DiaPronostico(dia, indice) }
        }
    }
    Nota(
        if (desdeCache) "Sin conexión, datos de ${Tiempo.haceCuanto(clima.obtenidoMs)}"
        else "Actualizado ${Tiempo.haceCuanto(clima.obtenidoMs)}"
    )
    Nota("Datos: Open-Meteo.com")
}

@Composable
private fun DiaPronostico(dia: DiaClima, indice: Int) {
    val nombre = when (indice) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> runCatching {
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dia.fecha)!!
            SimpleDateFormat("EEEE", LOCALE_BO).format(fecha).replaceFirstChar { it.uppercase() }
        }.getOrDefault(dia.fecha)
    }
    val condicion = ClimaCodigos.deCodigo(dia.codigo)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(nombre, style = MaterialTheme.typography.labelLarge)
        Icon(condicion.icono.vector(), contentDescription = condicion.descripcion, tint = MaterialTheme.colorScheme.primary)
        Text("${grados(dia.maxima)} / ${grados(dia.minima)}", style = MaterialTheme.typography.bodyMedium)
        Text("Lluvia ${dia.probLluvia}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Paso fronterizo --------------------------------------------------------------------------------------

@Composable
private fun TarjetaFrontera(estado: InfoUtilUiState, esAdmin: Boolean, onEditar: () -> Unit) {
    val info = estado.frontera ?: InfoFrontera()
    Tarjeta("Paso fronterizo Bermejo – Aguas Blancas", Icons.Filled.Terrain) {
        FilaDato("Horario del paso", info.horarioPaso)
        FilaDato("Horario de las chalanas", info.horarioChalanas)
        FilaDato("Tarifas de las chalanas", info.tarifasChalanas)
        FilaDato("Qué documentos llevar", info.recomendaciones)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Teléfonos útiles", style = MaterialTheme.typography.titleSmall)
        FilaTelefono("Policía", info.telPolicia)
        FilaTelefono("Bomberos", info.telBomberos)
        FilaTelefono("Ambulancia", info.telAmbulancia)
        FilaTelefono("Migración", info.telMigracion)
        Nota(
            when {
                info.actualizado <= 0L -> "Información aún no confirmada por LuciDan."
                estado.fronteraDesdeCache -> "Sin conexión. Última actualización: ${Tiempo.fechaHora(info.actualizado)}"
                else -> "Actualizado el ${Tiempo.fechaHora(info.actualizado)}"
            }
        )
        if (esAdmin) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text("Editar información") }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Un teléfono con al menos 3 dígitos es tocable y abre el marcador; "Por confirmar" se muestra como texto. */
@Composable
private fun FilaTelefono(etiqueta: String, valor: String) {
    val context = LocalContext.current
    val llamable = Contacto.soloDigitos(valor).length >= 3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (llamable) Modifier.clickable(onClickLabel = "Llamar a $etiqueta") { Contacto.llamar(context, valor) } else Modifier)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyLarge, color = if (llamable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        if (llamable) Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
