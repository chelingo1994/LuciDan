package dev.justinianomarcelo.lucidanapp.ui.negocio

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

/**
 * TA-09-01/02/03: edición del perfil de negocio, con carga y guardado inmediato. Con
 * [negocioId] = [NUEVO_NEGOCIO] es el formulario de registro de un negocio nuevo (DS07 / HU08).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarNegocioScreen(
    negocioId: Long,
    onVolver: () -> Unit,
    viewModel: NegocioViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(negocioId) { viewModel.cargarNegocio(negocioId) }
    val esNuevo = negocioId == NUEVO_NEGOCIO
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
                title = { Text(if (esNuevo) "Registrar negocio" else "Editar perfil de negocio") },
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
        if (estado.noEncontrado) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No encontramos este negocio en tu cuenta.", style = MaterialTheme.typography.bodyLarge)
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
            Text(
                if (esNuevo) "Registra tu negocio" else "Mantén tu ficha al día",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                if (esNuevo) "Aparecerá en el mapa de LuciDan cuando sea publicado."
                else "Actualiza los datos que ven tus clientes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            NegocioFormulario(
                nombre = estado.nombre,
                categoria = estado.categoria,
                descripcion = estado.descripcion,
                telefono = estado.telefono,
                direccion = estado.direccion,
                horario = estado.horario,
                foto = estado.foto,
                latitud = estado.latitud,
                longitud = estado.longitud,
                obteniendoUbicacion = estado.obteniendoUbicacion,
                errores = estado.errores,
                guardando = estado.guardando,
                textoBoton = if (esNuevo) "Registrar negocio" else "Guardar cambios",
                onNombreChange = viewModel::onNombreChange,
                onCategoriaChange = viewModel::onCategoriaChange,
                onDescripcionChange = viewModel::onDescripcionChange,
                onTelefonoChange = viewModel::onTelefonoChange,
                onDireccionChange = viewModel::onDireccionChange,
                onHorarioChange = viewModel::onHorarioChange,
                onUsarUbicacionActual = viewModel::onUsarUbicacionActual,
                onFotoSeleccionada = viewModel::onFotoSeleccionada,
                onQuitarFoto = viewModel::onQuitarFoto,
                onGuardar = {
                    if (esNuevo) viewModel.registrarNegocio(onVolver) else viewModel.guardarCambios(onVolver)
                }
            )
        }
    }
}
