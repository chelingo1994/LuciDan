package dev.justinianomarcelo.lucidanapp.ui.detalle

import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import dev.justinianomarcelo.lucidanapp.util.FotoNegocioImagen
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Phone
import dev.justinianomarcelo.lucidanapp.util.Contacto
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.ruta.ResultadoRuta
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    negocioId: Long,
    onVolver: () -> Unit,
    onRequiereSesion: () -> Unit,
    viewModel: DetalleViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var mapaCompletoVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(negocioId) { viewModel.cargarFicha(negocioId) }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(estado.requiereSesion) {
        if (estado.requiereSesion) {
            viewModel.limpiarRequiereSesion()
            onRequiereSesion()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it, shape = MaterialTheme.shapes.small) } },
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::alternarFavorito) {
                        Icon(
                            imageVector = if (estado.esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (estado.esFavorito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (estado.cargando || estado.negocio == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                if (estado.cargando) CircularProgressIndicator() else Text("Negocio no encontrado")
            }
            return@Scaffold
        }

        val negocio = estado.negocio
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FotoNegocioImagen(
                valor = negocio.fotos.firstOrNull(),
                contentDescription = "Foto de ${negocio.nombre}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp)),
                rubro = negocio.categoria
            )
            Text(
                negocio.nombre,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 20.dp)
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        negocio.categoria.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text("%.1f".format(negocio.promedioCalificacion), style = MaterialTheme.typography.labelLarge)
            }
            Text(
                negocio.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilaDato(Icons.Filled.Schedule, "Horario", negocio.horario)
                    if (negocio.direccion.isNotBlank()) FilaDato(Icons.Filled.Place, "Dirección", negocio.direccion)
                }
            }

            if (negocio.telefono.isNotBlank()) {
                val esCelular = Contacto.esCelular(negocio.telefono)
                OutlinedButton(
                    onClick = { Contacto.contactar(context, negocio.telefono) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
                ) {
                    Icon(if (esCelular) Icons.AutoMirrored.Filled.Chat else Icons.Filled.Phone, contentDescription = null)
                    Text(
                        (if (esCelular) "WhatsApp · " else "Llamar · ") + negocio.telefono,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Button(
                onClick = viewModel::trazarRuta,
                enabled = !estado.calculandoRuta,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = if (negocio.telefono.isNotBlank()) 12.dp else 24.dp).height(56.dp)
            ) {
                if (estado.calculandoRuta) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text("Cómo llegar")
                }
            }

            estado.ruta?.let { ruta ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ResumenRuta(ruta)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            MapaRuta(
                                modifier = Modifier.fillMaxSize(),
                                destino = GeoPoint(negocio.latitud, negocio.longitud),
                                nombreDestino = negocio.nombre,
                                ruta = ruta
                            )
                            // El mini-mapa vive dentro del scroll de la pantalla, así que
                            // no puede ganarle el gesto de pellizco al scroll: se toca
                            // para abrirlo en pantalla completa, donde sí se puede hacer
                            // zoom y desplazarlo libremente.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(onClickLabel = "Ampliar el mapa de la ruta", role = Role.Button) { mapaCompletoVisible = true }
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Fullscreen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Ampliar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Reseñas", style = MaterialTheme.typography.titleMedium)
            FormularioResena(
                calificacionSeleccionada = estado.calificacionSeleccionada,
                comentario = estado.comentario,
                enviando = estado.enviandoResena,
                onCalificacionSeleccionada = viewModel::onCalificacionSeleccionada,
                onComentarioChange = viewModel::onComentarioChange,
                onEnviar = viewModel::enviarResena
            )

            Spacer(modifier = Modifier.padding(top = 12.dp))

            if (estado.resenas.isEmpty()) {
                Text(
                    "Aún no hay reseñas para este negocio.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column {
                    estado.resenas.forEach { resena -> FilaResena(resena) }
                }
            }
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (mapaCompletoVisible) {
        val ruta = estado.ruta
        val negocio = estado.negocio
        if (ruta != null && negocio != null) {
            Dialog(
                onDismissRequest = { mapaCompletoVisible = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        // Aquí el mapa es la única superficie con gestos de la pantalla
                        // (no hay ningún scroll compitiendo), así que el pellizco para
                        // zoom y el arrastre funcionan de forma nativa con osmdroid.
                        MapaRuta(
                            modifier = Modifier.fillMaxSize(),
                            destino = GeoPoint(negocio.latitud, negocio.longitud),
                            nombreDestino = negocio.nombre,
                            ruta = ruta
                        )

                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp
                        ) {
                            IconButton(onClick = { mapaCompletoVisible = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                            }
                        }

                        // Resumen en la zona del pulgar, sobre el borde inferior.
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(negocio.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Box(modifier = Modifier.padding(top = 8.dp)) { ResumenRuta(ruta) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini-mapa con la ruta caminando resuelta por OSRM (DS04, ver CLAUDE.md
 * sección 2.1). Reemplaza al GoogleMap + Polyline de Maps Compose.
 * Recibe el destino como punto genérico.
 */
@Composable
internal fun MapaRuta(modifier: Modifier = Modifier, destino: GeoPoint, nombreDestino: String, ruta: ResultadoRuta) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorRuta = android.graphics.Color.parseColor("#69F0AE") // verde claro
    val colorRutaBorde = android.graphics.Color.parseColor("#1B5E20") // verde oscuro

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { vista ->
            vista.overlays.clear()

            if (ruta.puntos.isNotEmpty()) {
                // Borde verde oscuro debajo para que el trazo claro resalte sobre el mapa
                vista.overlays.add(
                    Polyline(vista).apply {
                        setPoints(ruta.puntos)
                        outlinePaint.color = colorRutaBorde
                        outlinePaint.strokeWidth = 20f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                )
                vista.overlays.add(
                    Polyline(vista).apply {
                        setPoints(ruta.puntos)
                        outlinePaint.color = colorRuta
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                )
            }
            vista.overlays.add(Marker(vista).apply { position = destino; title = nombreDestino })
            ruta.puntos.firstOrNull()?.let { origen ->
                vista.overlays.add(Marker(vista).apply { position = origen; title = "Tu ubicación" })
            }

            if (ruta.puntos.size >= 2) {
                vista.post { vista.zoomToBoundingBox(BoundingBox.fromGeoPoints(ruta.puntos), true, 64) }
            } else {
                vista.controller.setZoom(15.0)
                vista.controller.setCenter(destino)
            }
            vista.invalidate()
        }
    )
}

/** Distancia y duración de la ruta: el valor destaca, la etiqueta acompaña. */
@Composable
internal fun ResumenRuta(ruta: ResultadoRuta) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp).size(24.dp)
            )
        }
        Column {
            Text(ruta.duracionTexto, style = MaterialTheme.typography.titleMedium)
            Text(
                "${ruta.distanciaTexto} · caminando",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun FilaDato(icono: androidx.compose.ui.graphics.vector.ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column {
            Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FormularioResena(
    calificacionSeleccionada: Int,
    comentario: String,
    enviando: Boolean,
    onCalificacionSeleccionada: (Int) -> Unit,
    onComentarioChange: (String) -> Unit,
    onEnviar: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row {
            for (estrella in 1..5) {
                IconButton(onClick = { onCalificacionSeleccionada(estrella) }) {
                    val marcada = estrella <= calificacionSeleccionada
                    Icon(
                        imageVector = if (marcada) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Calificar con $estrella ${if (estrella == 1) "estrella" else "estrellas"}",
                        tint = if (marcada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        OutlinedTextField(
            value = comentario,
            onValueChange = onComentarioChange,
            label = { Text("Comparte tu experiencia") },
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = onEnviar,
            enabled = !enviando,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp)
        ) {
            Text(if (enviando) "Enviando..." else "Publicar reseña")
        }
    }
}

@Composable
private fun FilaResena(resena: Resena) {
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "BO")) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row {
            repeat(5) { indice ->
                Icon(
                    imageVector = if (indice < resena.calificacion) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (indice < resena.calificacion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (resena.comentario.isNotBlank()) {
            Text(resena.comentario, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            formato.format(resena.fecha),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
