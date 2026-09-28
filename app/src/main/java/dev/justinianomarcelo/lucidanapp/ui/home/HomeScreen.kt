package dev.justinianomarcelo.lucidanapp.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.Lugar
import dev.justinianomarcelo.lucidanapp.util.BermejoZona
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLugarClick: (Long) -> Unit,
    onIrAFavoritos: () -> Unit,
    onIrAMiNegocio: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val context = LocalContext.current
    val usuario by SessionManager.usuarioActual.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuAbierto by remember { mutableStateOf(false) }

    var tienePermisoUbicacion by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        tienePermisoUbicacion = resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            resultados[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.cargarLugaresCercanos()
    }

    LaunchedEffect(Unit) {
        if (tienePermisoUbicacion) {
            viewModel.cargarLugaresCercanos()
        } else {
            val permisos = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permisos.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            lanzadorPermisos.launch(permisos.toTypedArray())
        }
    }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val centroMapa = estado.ubicacionActual?.let { GeoPoint(it.latitud, it.longitud) }
        ?: remember { GeoPoint(BermejoZona.LATITUD_CENTRO, BermejoZona.LONGITUD_CENTRO) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it, shape = MaterialTheme.shapes.small) } },
        topBar = {
            TopAppBar(
                title = { Text("LuciDan · Bermejo") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuAbierto,
                        onDismissRequest = { menuAbierto = false },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        DropdownMenuItem(
                            text = { Text("Favoritos") },
                            leadingIcon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = null) },
                            onClick = { menuAbierto = false; onIrAFavoritos() }
                        )
                        DropdownMenuItem(
                            text = { Text("Mi negocio") },
                            leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                            onClick = { menuAbierto = false; onIrAMiNegocio() }
                        )
                        val notificacionesActivas = usuario?.notificacionesActivas == true
                        DropdownMenuItem(
                            text = { Text(if (notificacionesActivas) "Desactivar notificaciones" else "Activar notificaciones") },
                            leadingIcon = {
                                Icon(
                                    if (notificacionesActivas) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuAbierto = false
                                scope.launch { viewModel.alternarNotificaciones() }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menuAbierto = false; onCerrarSesion() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MapaLugares(
                    modifier = Modifier.fillMaxSize(),
                    centro = centroMapa,
                    lugares = estado.lugares,
                    puntoSeleccionadoManual = estado.puntoSeleccionadoManual,
                    tienePermisoUbicacion = tienePermisoUbicacion,
                    onLugarClick = onLugarClick,
                    onMapLongClick = viewModel::recalcularDesdePunto
                )

                // Panel flotante: los chips sobre el mapa pierden legibilidad sin fondo propio.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        SelectorDeRadio(
                            radioSeleccionado = estado.radioSeleccionado,
                            onRadioSeleccionado = viewModel::onRadioSeleccionado
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        SelectorDeCategoria(
                            categoriaSeleccionada = estado.categoriaSeleccionada,
                            onCategoriaSeleccionada = viewModel::onCategoriaSeleccionada
                        )
                    }
                }

                if (estado.puntoSeleccionadoManual != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Buscando desde el punto elegido",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            ListaLugaresCercanos(
                lugares = estado.lugares,
                origenLat = estado.puntoSeleccionadoManual?.latitude ?: estado.ubicacionActual?.latitud,
                origenLng = estado.puntoSeleccionadoManual?.longitude ?: estado.ubicacionActual?.longitud,
                onLugarClick = onLugarClick
            )
        }
    }
}

/**
 * Mapa con tiles de OpenStreetMap (osmdroid). Reemplaza al Maps SDK for
 * Android por la decisión de arquitectura de CLAUDE.md sección 2.1: sin
 * API key ni facturación. Mantiene long-press para HU03 (DS02) igual que
 * antes hacía `onMapLongClick` de Google Maps Compose.
 */
@Composable
private fun MapaLugares(
    modifier: Modifier = Modifier,
    centro: GeoPoint,
    lugares: List<Lugar>,
    puntoSeleccionadoManual: GeoPoint?,
    tienePermisoUbicacion: Boolean,
    onLugarClick: (Long) -> Unit,
    onMapLongClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onLugarClickActual = rememberUpdatedState(onLugarClick)
    val onMapLongClickActual = rememberUpdatedState(onMapLongClick)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(centro)
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let { onMapLongClickActual.value(it) }
                        return true
                    }
                })
            )
        }
    }
    val miUbicacionOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }

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

    LaunchedEffect(tienePermisoUbicacion) {
        if (tienePermisoUbicacion) miUbicacionOverlay.enableMyLocation() else miUbicacionOverlay.disableMyLocation()
    }

    LaunchedEffect(centro) {
        mapView.controller.animateTo(centro)
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { vista ->
            val eventosOverlay = vista.overlays.firstOrNull { it is MapEventsOverlay }
            vista.overlays.clear()
            eventosOverlay?.let { vista.overlays.add(it) }
            vista.overlays.add(miUbicacionOverlay)
            lugares.forEach { lugar ->
                vista.overlays.add(
                    Marker(vista).apply {
                        position = GeoPoint(lugar.latitud, lugar.longitud)
                        title = lugar.nombre
                        snippet = lugar.categoria.etiqueta
                        setOnMarkerClickListener { _, _ ->
                            onLugarClickActual.value(lugar.id)
                            true
                        }
                    }
                )
            }
            puntoSeleccionadoManual?.let { punto ->
                vista.overlays.add(
                    Marker(vista).apply {
                        position = punto
                        title = "Punto de búsqueda"
                    }
                )
            }
            vista.invalidate()
        }
    )
}

@Composable
private fun SelectorDeRadio(radioSeleccionado: Double, onRadioSeleccionado: (Double) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeUiState.RADIOS_METROS) { radio ->
            val etiqueta = if (radio >= 1000.0) "${(radio / 1000).roundToInt()} km" else "${radio.roundToInt()} m"
            FilterChip(
                selected = radio == radioSeleccionado,
                onClick = { onRadioSeleccionado(radio) },
                label = { Text(etiqueta) }
            )
        }
    }
}

@Composable
private fun SelectorDeCategoria(
    categoriaSeleccionada: CategoriaLugar?,
    onCategoriaSeleccionada: (CategoriaLugar?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = categoriaSeleccionada == null,
                onClick = { onCategoriaSeleccionada(null) },
                label = { Text("Todas") }
            )
        }
        items(CategoriaLugar.entries.toList()) { categoria ->
            FilterChip(
                selected = categoriaSeleccionada == categoria,
                onClick = { onCategoriaSeleccionada(categoria) },
                label = { Text(categoria.etiqueta) }
            )
        }
    }
}

@Composable
private fun ListaLugaresCercanos(
    lugares: List<Lugar>,
    origenLat: Double?,
    origenLng: Double?,
    onLugarClick: (Long) -> Unit
) {
    if (lugares.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lugares) { lugar ->
            Card(
                modifier = Modifier
                    .width(232.dp)
                    .clickable { onLugarClick(lugar.id) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        lugar.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        lugar.categoria.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (origenLat != null && origenLng != null) {
                        val distanciaMetros = lugar.calcularDistancia(origenLat, origenLng)
                        val texto = if (distanciaMetros >= 1000) {
                            "%.1f km".format(distanciaMetros / 1000)
                        } else {
                            "${distanciaMetros.roundToInt()} m"
                        }
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
