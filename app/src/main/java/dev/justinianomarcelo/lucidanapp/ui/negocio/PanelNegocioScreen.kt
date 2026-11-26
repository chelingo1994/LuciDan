package dev.justinianomarcelo.lucidanapp.ui.negocio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import dev.justinianomarcelo.lucidanapp.util.FotoNegocioImagen
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.ui.componentes.InsigniaDestacado
import dev.justinianomarcelo.lucidanapp.util.Destacados
import dev.justinianomarcelo.lucidanapp.util.LimiteFotos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Mis negocios" (HU08/HU09): un usuario puede tener 0 o varios negocios (diagrama de clases, 0..*).
 * Lista cada negocio con sus accesos (editar, publicidad, estadísticas) y permite agregar otro.
 * El formulario de registro (DS07) y el de edición son la misma pantalla, [EditarNegocioScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelNegocioScreen(
    onVolver: () -> Unit,
    onAgregarNegocio: () -> Unit,
    onEditarNegocio: (Long) -> Unit,
    onVerPlanes: (Long) -> Unit,
    onVerEstadisticas: (Long) -> Unit,
    viewModel: NegocioViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.cargarNegocios() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it, shape = MaterialTheme.shapes.small) } },
        topBar = {
            TopAppBar(
                title = { Text("Mis negocios") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (estado.negocios.isEmpty()) {
                    Text(
                        "Registra tu negocio",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        "Aparece en el mapa de LuciDan para que visitantes y viajeros sepan qué vendes y cómo llegar. " +
                            "Puedes registrar más de un negocio con tu cuenta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Button(
                    onClick = onAgregarNegocio,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(if (estado.negocios.isEmpty()) "Registrar negocio" else "Agregar otro negocio")
                }
            }
            items(estado.negocios, key = { it.id }) { negocio ->
                TarjetaNegocio(
                    negocio = negocio,
                    onEditar = { onEditarNegocio(negocio.id) },
                    onVerPlanes = { onVerPlanes(negocio.id) },
                    onVerEstadisticas = { onVerEstadisticas(negocio.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TarjetaNegocio(
    negocio: Negocio,
    onEditar: () -> Unit,
    onVerPlanes: () -> Unit,
    onVerEstadisticas: () -> Unit
) {
    val publicado = negocio.estado == EstadoNegocio.PUBLICADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            FotoNegocioImagen(
                valor = negocio.fotos.firstOrNull(),
                contentDescription = null, // decorativa: el nombre del negocio va justo debajo
                modifier = Modifier.fillMaxWidth().height(160.dp),
                rubro = negocio.categoria
            )
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = CircleShape,
                color = if (publicado) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    etiquetaEstado(negocio.estado),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (publicado) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Text(
                negocio.nombre,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (Destacados.nivelVigente(negocio) > 0) {
                InsigniaDestacado(Modifier.padding(top = 4.dp))
                Text(
                    "Plan activo hasta ${SimpleDateFormat("dd/MM/yyyy", Locale("es", "BO")).format(Date(negocio.destacadoHasta))}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val maximoFotos = LimiteFotos.maximo(negocio)
            if (negocio.fotos.size > maximoFotos) {
                Text(
                    "Tienes ${negocio.fotos.size} fotos y tu plan muestra solo las primeras $maximoFotos. " +
                        "No se borró ninguna: renueva o mejora tu plan para mostrarlas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                negocio.categoria.etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                negocio.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (negocio.telefono.isNotBlank()) FilaInfo(Icons.Filled.Phone, negocio.telefono)
                if (negocio.direccion.isNotBlank()) FilaInfo(Icons.Filled.Place, negocio.direccion)
                FilaInfo(Icons.Filled.Schedule, negocio.horario)
            }
            Button(
                onClick = onEditar,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(48.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Editar perfil")
            }
            OutlinedButton(
                onClick = onVerPlanes,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Publicidad destacada")
            }
            OutlinedButton(
                onClick = onVerEstadisticas,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp)
            ) {
                Icon(Icons.Filled.BarChart, contentDescription = null)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Ver estadísticas")
            }
        }
        }
    }
}

@Composable
private fun FilaInfo(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun etiquetaEstado(estado: EstadoNegocio) = when (estado) {
    EstadoNegocio.PENDIENTE -> "Pendiente de publicación"
    EstadoNegocio.PUBLICADO -> "Publicado"
    EstadoNegocio.SUSPENDIDO -> "Suspendido"
}
