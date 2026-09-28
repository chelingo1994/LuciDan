package dev.justinianomarcelo.lucidanapp.ui.negocio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import dev.justinianomarcelo.lucidanapp.util.FotoNegocioImagen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar

/** Formulario compartido por el registro (HU08) y la edición (HU09) de negocio. */
@Composable
fun NegocioFormulario(
    nombre: String,
    categoria: CategoriaLugar?,
    ubicacion: String,
    horario: String,
    foto: String,
    errores: Map<String, String>,
    guardando: Boolean,
    textoBoton: String,
    onNombreChange: (String) -> Unit,
    onCategoriaChange: (CategoriaLugar) -> Unit,
    onUbicacionChange: (String) -> Unit,
    onHorarioChange: (String) -> Unit,
    onFotoSeleccionada: (Uri) -> Unit,
    onQuitarFoto: () -> Unit,
    onGuardar: () -> Unit
) {
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onFotoSeleccionada(uri)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre del negocio") },
            leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
            isError = errores.containsKey("nombre"),
            shape = RoundedCornerShape(16.dp),
            supportingText = { errores["nombre"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Categoría", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CategoriaLugar.entries.toList()) { opcion ->
                FilterChip(
                    selected = categoria == opcion,
                    onClick = { onCategoriaChange(opcion) },
                    label = { Text(opcion.etiqueta) }
                )
            }
        }
        errores["categoria"]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = ubicacion,
            onValueChange = onUbicacionChange,
            label = { Text("Ubicación (dirección)") },
            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
            isError = errores.containsKey("ubicacion"),
            shape = RoundedCornerShape(16.dp),
            supportingText = { errores["ubicacion"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = horario,
            onValueChange = onHorarioChange,
            label = { Text("Horario de atención") },
            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            isError = errores.containsKey("horario"),
            shape = RoundedCornerShape(16.dp),
            supportingText = { errores["horario"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text("Foto del negocio (opcional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        if (foto.isNotBlank()) {
            FotoNegocioImagen(
                valor = foto,
                contentDescription = "Foto del negocio",
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = if (foto.isNotBlank()) 8.dp else 0.dp)
        ) {
            OutlinedButton(
                onClick = {
                    selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (foto.isBlank()) "Elegir foto" else "Cambiar foto", modifier = Modifier.padding(start = 8.dp))
            }
            if (foto.isNotBlank()) {
                TextButton(onClick = onQuitarFoto) { Text("Quitar") }
            }
        }

        Button(
            onClick = onGuardar,
            enabled = !guardando,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp)
        ) {
            if (guardando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(textoBoton)
            }
        }
    }
}
