package dev.justinianomarcelo.lucidanapp.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Usuario
import dev.justinianomarcelo.lucidanapp.util.Sello

private sealed interface ElementoLista {
    data class Encabezado(val titulo: String) : ElementoLista
    data class FilaUsuario(val usuario: Usuario) : ElementoLista
    data class FilaNegocio(val negocio: Negocio) : ElementoLista
}

/** DS11 (HU14): panel de administración, lista de cuentas y negocios con activar/suspender/eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onVolver: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmarEliminarUsuario by remember { mutableStateOf<Usuario?>(null) }
    var confirmarEliminarNegocio by remember { mutableStateOf<Negocio?>(null) }

    LaunchedEffect(Unit) { viewModel.cargarPanel() }
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
                title = { Text("Administración") },
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

        if (estado.accesoDenegado) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    "Acceso denegado: esta sección es solo para administradores.",
                    modifier = Modifier.padding(horizontal = 32.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        val elementos = buildList {
            add(ElementoLista.Encabezado("Usuarios (${estado.usuarios.size})"))
            estado.usuarios.forEach { add(ElementoLista.FilaUsuario(it)) }
            add(ElementoLista.Encabezado("Negocios (${estado.negocios.size})"))
            estado.negocios.forEach { add(ElementoLista.FilaNegocio(it)) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            items(elementos) { elemento ->
                when (elemento) {
                    is ElementoLista.Encabezado -> Text(
                        elemento.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    is ElementoLista.FilaUsuario -> FilaUsuarioAdmin(
                        usuario = elemento.usuario,
                        onCambiarEstado = { activo -> viewModel.cambiarEstadoUsuario(elemento.usuario.id, activo) },
                        onEliminar = { confirmarEliminarUsuario = elemento.usuario }
                    )
                    is ElementoLista.FilaNegocio -> FilaNegocioAdmin(
                        negocio = elemento.negocio,
                        onCambiarEstado = { estadoNuevo -> viewModel.cambiarEstadoNegocio(elemento.negocio.id, estadoNuevo) },
                        onEliminar = { confirmarEliminarNegocio = elemento.negocio },
                        onCambiarVerificado = { viewModel.cambiarVerificado(elemento.negocio.id, it) }
                    )
                }
            }
        }
    }

    confirmarEliminarUsuario?.let { usuario ->
        AlertDialog(
            onDismissRequest = { confirmarEliminarUsuario = null },
            title = { Text("¿Eliminar cuenta?") },
            text = { Text("Se eliminará la cuenta de ${usuario.correo}. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarUsuario(usuario.id)
                    confirmarEliminarUsuario = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminarUsuario = null }) { Text("Cancelar") }
            }
        )
    }

    confirmarEliminarNegocio?.let { negocio ->
        AlertDialog(
            onDismissRequest = { confirmarEliminarNegocio = null },
            title = { Text("¿Eliminar negocio?") },
            text = { Text("Se eliminará \"${negocio.nombre}\" del mapa. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarNegocio(negocio.id)
                    confirmarEliminarNegocio = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminarNegocio = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaUsuarioAdmin(usuario: Usuario, onCambiarEstado: (Boolean) -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(usuario.correo, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        usuario.rol.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EtiquetaEstado(if (usuario.activo) "Activo" else "Suspendido", usuario.activo)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCambiarEstado(!usuario.activo) }) {
                    Icon(
                        if (usuario.activo) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (usuario.activo) "Suspender" else "Activar")
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FilaNegocioAdmin(
    negocio: Negocio,
    onCambiarEstado: (EstadoNegocio) -> Unit,
    onEliminar: () -> Unit,
    onCambiarVerificado: (Boolean) -> Unit
) {
    val publicado = negocio.estado == EstadoNegocio.PUBLICADO
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(negocio.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        negocio.categoria.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EtiquetaEstado(etiquetaEstadoNegocio(negocio.estado), publicado)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCambiarEstado(if (publicado) EstadoNegocio.SUSPENDIDO else EstadoNegocio.PUBLICADO) }) {
                    Icon(
                        if (publicado) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (publicado) "Suspender" else "Publicar")
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = { onCambiarVerificado(!negocio.verificado) }) {
                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (negocio.verificado) "Quitar verificación" else "Marcar como verificado")
            }
            if (negocio.verificado) {
                Text(
                    if (Sello.visible(negocio)) "Sello visible en la ficha."
                    else "Verificado, pero el sello solo se ve con un plan Trimestral vigente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(texto: String, positivo: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (positivo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (positivo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private fun etiquetaEstadoNegocio(estado: EstadoNegocio) = when (estado) {
    EstadoNegocio.PENDIENTE -> "Pendiente"
    EstadoNegocio.PUBLICADO -> "Publicado"
    EstadoNegocio.SUSPENDIDO -> "Suspendido"
}
