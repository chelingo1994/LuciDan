package dev.justinianomarcelo.lucidanapp.ui.negocio

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio

/**
 * Panel del negocio: si el usuario aún no tiene negocio, muestra el
 * formulario de registro (DS07 / HU08). Si ya tiene uno, muestra un
 * resumen con acceso a la edición del perfil (HU09).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelNegocioScreen(
    onVolver: () -> Unit,
    onEditarPerfil: () -> Unit,
    viewModel: NegocioViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.cargarNegocio() }
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
                title = { Text("Mi negocio") },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (estado.negocioExistente == null) {
                Text(
                    "Registra tu negocio",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Aparece en el mapa de LuciDan para que visitantes y viajeros sepan qué vendes y cómo llegar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                NegocioFormulario(
                    nombre = estado.nombre,
                    categoria = estado.categoria,
                    ubicacion = estado.ubicacion,
                    horario = estado.horario,
                    foto = estado.foto,
                    errores = estado.errores,
                    guardando = estado.guardando,
                    textoBoton = "Registrar negocio",
                    onNombreChange = viewModel::onNombreChange,
                    onCategoriaChange = viewModel::onCategoriaChange,
                    onUbicacionChange = viewModel::onUbicacionChange,
                    onHorarioChange = viewModel::onHorarioChange,
                    onFotoSeleccionada = viewModel::onFotoSeleccionada,
                    onQuitarFoto = viewModel::onQuitarFoto,
                    onGuardar = { viewModel.registrarNegocio {} }
                )
            } else {
                val negocio = estado.negocioExistente
                val publicado = negocio.estado == EstadoNegocio.PUBLICADO
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
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
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            negocio.categoria.etiqueta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilaInfo(Icons.Filled.Place, negocio.ubicacion)
                            FilaInfo(Icons.Filled.Schedule, negocio.horario)
                        }
                    }
                }
                Button(
                    onClick = onEditarPerfil,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Editar perfil")
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
