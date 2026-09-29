package com.example.controlgastos.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.data.Usuario
import com.example.controlgastos.viewmodel.GastosViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: GastosViewModel,
    onLoginExitoso: (String, String) -> Unit // Retorna el correo y el ID (UUID) del usuario
) {
    var esRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Campos de seguridad para el registro
    var preguntaSeguridad by remember { mutableStateOf("") }
    var respuestaSeguridad by remember { mutableStateOf("") }

    var mostrarDialogoRecuperar by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- LOGO DE LUCHICASH ---
                Image(
                    painter = painterResource(id = R.drawable.luchicash),
                    contentDescription = "Logo de LuchiCash",
                    modifier = Modifier
                        .size(110.dp)
                        .padding(bottom = 4.dp)
                )

                Text(
                    text = if (esRegistro) "Crear Cuenta" else "Iniciar Sesión",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (esRegistro) "Regístrate para respaldar tu data" else "Bienvenido de nuevo a LuchiCash",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (esRegistro) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (esRegistro) {
                    OutlinedTextField(
                        value = preguntaSeguridad,
                        onValueChange = { preguntaSeguridad = it },
                        label = { Text("Pregunta de seguridad (Ej: Mi mascota)") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = respuestaSeguridad,
                        onValueChange = { respuestaSeguridad = it },
                        label = { Text("Respuesta secreta") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (correo.isBlank() || contrasena.isBlank() || (esRegistro && (nombre.isBlank() || preguntaSeguridad.isBlank() || respuestaSeguridad.isBlank()))) {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            if (esRegistro) {
                                val usuarioId = viewModel.registrarUsuario(
                                    nombre.trim(),
                                    correo.trim(),
                                    contrasena,
                                    preguntaSeguridad.trim(),
                                    respuestaSeguridad.trim()
                                )
                                if (usuarioId != null) {
                                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    onLoginExitoso(correo.trim(), usuarioId)
                                } else {
                                    Toast.makeText(context, "El correo ya está registrado o hubo un error", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val usuarioId = viewModel.loginUsuario(correo.trim(), contrasena)
                                if (usuarioId != null) {
                                    onLoginExitoso(correo.trim(), usuarioId)
                                } else {
                                    Toast.makeText(context, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (esRegistro) "Registrarse" else "Ingresar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (!esRegistro) {
                    TextButton(onClick = { mostrarDialogoRecuperar = true }) {
                        Text(text = "¿Olvidaste tu contraseña?", color = MaterialTheme.colorScheme.primary)
                    }
                }

                TextButton(onClick = { esRegistro = !esRegistro }) {
                    Text(
                        text = if (esRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (mostrarDialogoRecuperar) {
        DialogoRecuperarContrasena(
            viewModel = viewModel,
            onDismiss = { mostrarDialogoRecuperar = false }
        )
    }
}

@Composable
fun DialogoRecuperarContrasena(
    viewModel: GastosViewModel,
    onDismiss: () -> Unit
) {
    var paso by remember { mutableStateOf(1) }
    var correo by remember { mutableStateOf("") }
    var usuarioRecuperado by remember { mutableStateOf<Usuario?>(null) }
    var respuestaIngresada by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (paso == 1) "Recuperar Contraseña" else "Responde tu pregunta")
        },
        text = {
            if (paso == 1) {
                Column {
                    Text("Ingresa tu correo para buscar tu pregunta de seguridad.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Tu correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column {
                    Text(text = "¿${usuarioRecuperado?.preguntaSeguridad}?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = respuestaIngresada,
                        onValueChange = { respuestaIngresada = it },
                        label = { Text("Tu respuesta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevaContrasena,
                        onValueChange = { nuevaContrasena = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        if (paso == 1) {
                            if (correo.isNotBlank()) {
                                val user = viewModel.obtenerPreguntaSeguridad(correo.trim())
                                if (user != null && user.preguntaSeguridad.isNotBlank()) {
                                    usuarioRecuperado = user
                                    paso = 2
                                } else {
                                    Toast.makeText(context, "Correo no encontrado o sin pregunta de seguridad", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            if (respuestaIngresada.trim().equals(usuarioRecuperado?.respuestaSeguridad, ignoreCase = true)) {
                                if (nuevaContrasena.isNotBlank()) {
                                    val exito = viewModel.restablecerContrasena(usuarioRecuperado!!, nuevaContrasena)
                                    if (exito) {
                                        Toast.makeText(context, "¡Contraseña actualizada con éxito!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Error al actualizar en la nube", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Ingresa una nueva contraseña", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Respuesta incorrecta", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ) {
                Text(if (paso == 1) "Buscar" else "Restablecer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}