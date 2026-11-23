package dev.justinianomarcelo.lucidanapp.ui.auth

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.justinianomarcelo.lucidanapp.ui.theme.LuciDanAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val estado = viewModel.uiState

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        RegistroContenido(
            paddingValues = paddingValues,
            estado = estado,
            onCorreoChange = viewModel::onCorreoChange,
            onContrasenaChange = viewModel::onContrasenaChange,
            onConfirmarContrasenaChange = viewModel::onConfirmarContrasenaChange,
            onRegistrarse = { viewModel.registrarse(onRegistroExitoso) },
            onVolver = onVolver
        )
    }
}

@Composable
private fun RegistroContenido(
    paddingValues: PaddingValues,
    estado: AuthUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onConfirmarContrasenaChange: (String) -> Unit,
    onRegistrarse: () -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        CabeceraAuth(
            titulo = "Regístrate para descubrir Bermejo",
            subtitulo = "Guarda tus favoritos y deja reseñas de los negocios de Bermejo.",
            anchoLogo = 200.dp,
            estiloTitulo = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        OutlinedTextField(
            value = estado.correo,
            onValueChange = onCorreoChange,
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        CampoContrasena(
            value = estado.contrasena,
            onValueChange = onContrasenaChange,
            label = "Contraseña",
            supportingText = "Mínimo 8 caracteres, con mayúscula, minúscula y número.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        CampoContrasena(
            value = estado.confirmarContrasena,
            onValueChange = onConfirmarContrasenaChange,
            label = "Confirmar contraseña",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        if (estado.mensajeError != null) {
            Text(
                text = estado.mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onRegistrarse,
            enabled = !estado.cargando,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(56.dp)
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Crear cuenta")
            }
        }

        TextButton(
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text("Ya tengo cuenta, iniciar sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistroScreenPreview() {
    LuciDanAppTheme {
        RegistroContenido(
            paddingValues = PaddingValues(0.dp),
            estado = AuthUiState(),
            onCorreoChange = {},
            onContrasenaChange = {},
            onConfirmarContrasenaChange = {},
            onRegistrarse = {},
            onVolver = {}
        )
    }
}
