package dev.justinianomarcelo.lucidanapp.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** DS10 (HU11): panel de estadísticas del negocio con contador de vistas y clics por período. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelEstadisticasScreen(
    negocioId: Long,
    onVolver: () -> Unit,
    viewModel: EstadisticaViewModel = viewModel()
) {
    val estado = viewModel.uiState

    LaunchedEffect(negocioId) { viewModel.cargarResumen(negocioId) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (estado.cargando && estado.negocioId == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (estado.negocioId == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Registra tu negocio para ver sus estadísticas.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SelectorDePeriodo(
                periodoSeleccionado = estado.periodoDias,
                onPeriodoSeleccionado = viewModel::onPeriodoSeleccionado
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TarjetaEstadistica(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Filled.Visibility,
                    etiqueta = "Vistas",
                    valor = estado.vistas
                )
                TarjetaEstadistica(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Filled.Directions,
                    etiqueta = "Cómo llegar",
                    valor = estado.clics
                )
            }
        }
    }
}

@Composable
private fun SelectorDePeriodo(periodoSeleccionado: Int, onPeriodoSeleccionado: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EstadisticaUiState.PERIODOS_DIAS) { dias ->
            FilterChip(
                selected = dias == periodoSeleccionado,
                onClick = { onPeriodoSeleccionado(dias) },
                label = { Text("$dias días") }
            )
        }
    }
}

@Composable
private fun TarjetaEstadistica(modifier: Modifier = Modifier, icono: ImageVector, etiqueta: String, valor: Int) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "$valor",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
