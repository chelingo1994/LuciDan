package dev.justinianomarcelo.lucidanapp.ui.infoutil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.data.model.InfoFrontera
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario

/** Formulario del administrador para la información del paso fronterizo (HU17). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarFronteraScreen(
    onVolver: () -> Unit,
    viewModel: EditarFronteraViewModel = viewModel()
) {
    val estado = viewModel.uiState
    val usuario by SessionManager.usuarioActual.collectAsState()
    val d = estado.datos

    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar paso fronterizo") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (usuario?.rol != RolUsuario.ADMINISTRADOR) {
            Text("Solo un administrador puede editar esta información.", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Los campos que dejes vacíos se mostrarán como \"${InfoFrontera.POR_CONFIRMAR}\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Campo("Horario del paso fronterizo", d.horarioPaso, multilinea = true) { t -> viewModel.onCambio { it.copy(horarioPaso = t) } }
            Campo("Horario de las chalanas", d.horarioChalanas, multilinea = true) { t -> viewModel.onCambio { it.copy(horarioChalanas = t) } }
            Campo("Tarifas de las chalanas", d.tarifasChalanas, multilinea = true) { t -> viewModel.onCambio { it.copy(tarifasChalanas = t) } }
            Campo("Recomendaciones: documentos que llevar", d.recomendaciones, multilinea = true) { t -> viewModel.onCambio { it.copy(recomendaciones = t) } }
            Campo("Teléfono de la Policía", d.telPolicia) { t -> viewModel.onCambio { it.copy(telPolicia = t) } }
            Campo("Teléfono de Bomberos", d.telBomberos) { t -> viewModel.onCambio { it.copy(telBomberos = t) } }
            Campo("Teléfono de Ambulancia", d.telAmbulancia) { t -> viewModel.onCambio { it.copy(telAmbulancia = t) } }
            Campo("Teléfono de Migración", d.telMigracion) { t -> viewModel.onCambio { it.copy(telMigracion = t) } }
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = viewModel::guardar,
                enabled = !estado.guardando && !estado.cargando,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (estado.guardando) "Guardando..." else "Guardar") }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, multilinea: Boolean = false, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        minLines = if (multilinea) 3 else 1,
        singleLine = !multilinea,
        keyboardOptions = KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}
