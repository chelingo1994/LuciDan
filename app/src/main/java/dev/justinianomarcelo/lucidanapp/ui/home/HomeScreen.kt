package dev.justinianomarcelo.lucidanapp.ui.home

import dev.justinianomarcelo.lucidanapp.ui.theme.PreferenciaTema
import dev.justinianomarcelo.lucidanapp.ui.theme.ModoTema
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.ui.graphics.Color
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
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import dev.justinianomarcelo.lucidanapp.ui.componentes.InsigniaDestacado
import dev.justinianomarcelo.lucidanapp.ui.componentes.CintaOferta
import dev.justinianomarcelo.lucidanapp.util.Ofertas
import dev.justinianomarcelo.lucidanapp.util.BermejoZona
import dev.justinianomarcelo.lucidanapp.util.Destacados
import dev.justinianomarcelo.lucidanapp.util.FotoNegocioImagen
import dev.justinianomarcelo.lucidanapp.util.color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNegocioClick: (Long) -> Unit,
    onIrAFavoritos: () -> Unit,
    onIrAMiNegocio: () -> Unit,
    onIrAAdmin: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val context = LocalContext.current
    val usuario by SessionManager.usuarioActual.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuAbierto by remember { mutableStateOf(false) }
    var dialogoTemaAbierto by remember { mutableStateOf(false) }

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
        viewModel.cargarNegociosCercanos()
    }

    LaunchedEffect(Unit) {
        if (tienePermisoUbicacion) {
            viewModel.cargarNegociosCercanos()
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
                            text = { Text("Mis negocios") },
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
                        if (usuario?.rol == RolUsuario.ADMINISTRADOR) {
                            DropdownMenuItem(
                                text = { Text("Administración") },
                                leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null) },
                                onClick = { menuAbierto = false; onIrAAdmin() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Tema: ${PreferenciaTema.modo.etiqueta}") },
                            leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                            onClick = { menuAbierto = false; dialogoTemaAbierto = true }
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
        // El mapa ocupa toda la pantalla y la lista vive en una hoja inferior que se pliega: así los
        // resultados no le quitan espacio ni toques al mapa (basta con arrastrarla hacia abajo).
        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            sheetPeekHeight = ALTURA_HOJA_PLEGADA,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            sheetContent = {
                ListaNegociosCercanos(
                    negocios = estado.negocios,
                    enRadio = estado.enRadio,
                    carrusel = estado.carrusel,
                    origenLat = estado.puntoSeleccionadoManual?.latitude ?: estado.ubicacionActual?.latitud,
                    origenLng = estado.puntoSeleccionadoManual?.longitude ?: estado.ubicacionActual?.longitud,
                    onNegocioClick = onNegocioClick,
                    consulta = estado.consulta,
                    busquedaAmpliada = estado.busquedaAmpliada,
                    onAmpliarBusqueda = viewModel::onAmpliarBusqueda
                )
            }
        ) { espacioHoja ->
            Box(modifier = Modifier.fillMaxSize()) {
                MapaNegocios(
                    modifier = Modifier.fillMaxSize(),
                    centro = centroMapa,
                    negocios = estado.negocios,
                    seleccionadoId = viewModel.negocioSeleccionadoId,
                    onSeleccionar = viewModel::onNegocioSeleccionado,
                    puntoSeleccionadoManual = estado.puntoSeleccionadoManual,
                    tienePermisoUbicacion = tienePermisoUbicacion,
                    onNegocioClick = onNegocioClick,
                    onMapLongClick = viewModel::recalcularDesdePunto
                )

                // Panel flotante: los chips sobre el mapa pierden legibilidad sin fondo propio.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        CampoBusqueda(
                            consulta = estado.consulta,
                            onConsultaCambiada = viewModel::onConsultaCambiada,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        SelectorDeRadio(
                            radioSeleccionado = estado.radioSeleccionado,
                            onRadioSeleccionado = viewModel::onRadioSeleccionado
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        SelectorDeRubro(
                            categoriaSeleccionada = estado.categoriaSeleccionada,
                            onCategoriaSeleccionada = viewModel::onCategoriaSeleccionada
                        )
                    }
                }

                if (estado.puntoSeleccionadoManual != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(espacioHoja)
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
        }
    }
    if (dialogoTemaAbierto) {
        DialogoTema(
            modoActual = PreferenciaTema.modo,
            onElegir = { PreferenciaTema.cambiar(context, it); dialogoTemaAbierto = false },
            onCerrar = { dialogoTemaAbierto = false }
        )
    }
}

/** Selector de tema: del sistema, claro u oscuro. Cada opción es una fila seleccionable de al menos 48 dp. */
@Composable
private fun DialogoTema(modoActual: ModoTema, onElegir: (ModoTema) -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Tema de la app") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ModoTema.entries.forEach { modo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = modo == modoActual, role = Role.RadioButton, onClick = { onElegir(modo) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = modo == modoActual, onClick = null)
                        Text(modo.etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

private val ALTURA_HOJA_PLEGADA = 96.dp

/**
 * Mapa con tiles de OpenStreetMap (osmdroid). Reemplaza al Maps SDK for
 * Android por la decisión de arquitectura de CLAUDE.md sección 2.1: sin
 * API key ni facturación. Mantiene long-press para HU03 (DS02) igual que
 * antes hacía `onMapLongClick` de Google Maps Compose.
 */
@Composable
private fun MapaNegocios(
    modifier: Modifier = Modifier,
    centro: GeoPoint,
    negocios: List<Negocio>,
    seleccionadoId: Long?,
    onSeleccionar: (Long?) -> Unit,
    puntoSeleccionadoManual: GeoPoint?,
    tienePermisoUbicacion: Boolean,
    onNegocioClick: (Long) -> Unit,
    onMapLongClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onNegocioClickActual = rememberUpdatedState(onNegocioClick)
    val onMapLongClickActual = rememberUpdatedState(onMapLongClick)

    val onSeleccionarActual = rememberUpdatedState(onSeleccionar)
    val colorPrimario = MaterialTheme.colorScheme.primary.toArgb()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(centro)
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        onSeleccionarActual.value(null)
                        return false
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let { onMapLongClickActual.value(it) }
                        return true
                    }
                })
            )
        }
    }
    val miUbicacionOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            val punto = MarcadorIconos.miUbicacion(context)
            setPersonIcon(punto)
            setDirectionArrow(punto, punto)
            setPersonAnchor(0.5f, 0.5f)
            setDirectionAnchor(0.5f, 0.5f)
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
            // Los negocios muy juntos se agrupan en un círculo granate con el número (osmbonuspack). El seleccionado va
            // fuera del grupo, encima, para que siempre se vea.
            val grupos = RadiusMarkerClusterer(context).apply {
                setIcon(MarcadorIconos.grupo(context))
                setRadius((36 * context.resources.displayMetrics.density).toInt())
                setMaxClusteringZoomLevel(16)
                textPaint.apply {
                    color = android.graphics.Color.WHITE
                    textSize = 13 * context.resources.displayMetrics.scaledDensity
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            }
            vista.overlays.add(grupos)
            // Los del norte se dibujan primero y los del sur encima (la punta del pin queda abajo).
            val ahora = System.currentTimeMillis()
            val marcadores = negocios.sortedWith(compareByDescending<Negocio> { it.latitud }.thenBy { it.id })
                .map { negocio ->
                    val seleccionado = negocio.id == seleccionadoId
                    val destacado = Destacados.beneficios(negocio, ahora).destacado
                    val marcador = Marker(vista).apply {
                        position = GeoPoint(negocio.latitud, negocio.longitud)
                        title = negocio.nombre
                        snippet = negocio.categoria.etiqueta
                        // Drawable propio por marcador (el bitmap viene del caché); no se tiñe el ícono por defecto.
                        icon = MarcadorIconos.pin(context, negocio.categoria, seleccionado, destacado)
                        setAnchor(MarcadorIconos.ANCLA_X, MarcadorIconos.ANCLA_Y)
                        setOnMarkerClickListener { _, _ ->
                            onSeleccionarActual.value(negocio.id)
                            onNegocioClickActual.value(negocio.id)
                            true
                        }
                    }
                    Triple(marcador, seleccionado, destacado)
                }
            marcadores.filterNot { it.second || it.third }.forEach { grupos.add(it.first) }
            grupos.invalidate()
            // Los destacados no se agrupan y van encima de los demás; el seleccionado, encima de todos.
            marcadores.filter { it.third && !it.second }.forEach { vista.overlays.add(it.first) }
            marcadores.filter { it.second }.forEach { vista.overlays.add(it.first) }
            puntoSeleccionadoManual?.let { punto ->
                vista.overlays.add(
                    Marker(vista).apply {
                        position = punto
                        title = "Punto de búsqueda"
                        icon = MarcadorIconos.puntoElegido(context, colorPrimario)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )
            }
            vista.invalidate()
        }
    )
}

@Composable
private fun CampoBusqueda(
    consulta: String,
    onConsultaCambiada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = consulta,
        onValueChange = onConsultaCambiada,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar negocios", maxLines = 1) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (consulta.isNotEmpty()) {
                IconButton(onClick = { onConsultaCambiada("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() })
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
private fun SelectorDeRubro(
    categoriaSeleccionada: Rubro?,
    onCategoriaSeleccionada: (Rubro?) -> Unit
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
        items(Rubro.entries.toList()) { categoria ->
            FilterChip(
                selected = categoriaSeleccionada == categoria,
                onClick = { onCategoriaSeleccionada(categoria) },
                label = { Text(categoria.etiqueta) }
            )
        }
    }
}

/** Contenido de la hoja inferior: encabezado con el conteo (visible plegada) y la lista de resultados. */
@Composable
private fun ListaNegociosCercanos(
    negocios: List<Negocio>,
    enRadio: Int,
    carrusel: List<Negocio>,
    origenLat: Double?,
    origenLng: Double?,
    onNegocioClick: (Long) -> Unit,
    consulta: String,
    busquedaAmpliada: Boolean,
    onAmpliarBusqueda: (Boolean) -> Unit
) {
    val total = negocios.size
    val lejanos = total - enRadio
    val zona = if (busquedaAmpliada) "en Bermejo" else "cerca"
    val resumen = when {
        total == 0 -> "Sin resultados ${if (busquedaAmpliada) "en Bermejo" else "en este radio"}"
        lejanos > 0 -> "$enRadio $zona · $lejanos ${if (lejanos == 1) "destacado" else "destacados"} más lejos"
        else -> "$total ${if (total == 1) "negocio" else "negocios"} $zona"
    }
    val ahora = System.currentTimeMillis()
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                resumen,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            )
            if (busquedaAmpliada) {
                TextButton(onClick = { onAmpliarBusqueda(false) }) { Text("Usar radio") }
            } else if (enRadio == 0 && consulta.isNotBlank()) {
                TextButton(onClick = { onAmpliarBusqueda(true) }) { Text("Buscar en todo Bermejo") }
            }
        }
        HorizontalDivider()
        // La hoja llega hasta el borde inferior (edge-to-edge): el último negocio no debe quedar bajo la barra del sistema.
        val barraSistema = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + barraSistema)) {
            if (carrusel.isNotEmpty()) {
                item(key = "carrusel") { CarruselDestacados(carrusel, onNegocioClick) }
            }
            items(negocios, key = { "negocio-${it.id}" }) { negocio ->
                FilaCercana(
                    nombre = negocio.nombre,
                    etiqueta = negocio.categoria.etiqueta,
                    foto = negocio.fotos.firstOrNull(),
                    rubro = negocio.categoria,
                    distanciaMetros = origenLat?.let { lat -> origenLng?.let { lng -> negocio.calcularDistancia(lat, lng) } },
                    destacado = Destacados.beneficios(negocio, ahora).destacado,
                    oferta = Ofertas.vigente(negocio, ahora),
                    onClick = { onNegocioClick(negocio.id) }
                )
            }
        }
    }
}

/** Carrusel «Destacados» (plan Trimestral). */
@Composable
private fun CarruselDestacados(negocios: List<Negocio>, onNegocioClick: (Long) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            "Destacados",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(negocios, key = { it.id }) { negocio ->
                Surface(
                    modifier = Modifier
                        .width(160.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = "Ver ${negocio.nombre}", role = Role.Button) { onNegocioClick(negocio.id) },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column {
                        FotoNegocioImagen(
                            negocio.fotos.firstOrNull(), null,
                            Modifier.fillMaxWidth().height(96.dp), rubro = negocio.categoria
                        )
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(negocio.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            InsigniaDestacado(Modifier.padding(top = 4.dp))
                            Ofertas.vigente(negocio)?.let { CintaOferta(it, Modifier.padding(top = 4.dp)) }
                        }
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun FilaCercana(
    nombre: String,
    etiqueta: String,
    foto: String?,
    rubro: Rubro,
    distanciaMetros: Double?,
    destacado: Boolean,
    oferta: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Ver $nombre", role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Sin foto se ve el placeholder del rubro, del mismo color que el marcador del mapa.
            FotoNegocioImagen(foto, null, Modifier.fillMaxSize(), rubro = rubro) // decorativa: el nombre ya está en el texto
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (destacado) InsigniaDestacado(Modifier.padding(vertical = 2.dp))
            if (oferta != null) CintaOferta(oferta, Modifier.padding(vertical = 2.dp))
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (distanciaMetros != null) {
                val texto = if (distanciaMetros >= 1000) "%.1f km".format(distanciaMetros / 1000) else "${distanciaMetros.roundToInt()} m"
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.NearMe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
