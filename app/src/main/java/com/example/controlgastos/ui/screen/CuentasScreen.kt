package com.example.controlgastos.ui.screen

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.ui.theme.*
import com.example.controlgastos.viewmodel.GastosViewModel

@Composable
fun CuentasScreen(viewModel: GastosViewModel, onVolver: () -> Unit) {
    BackHandler { onVolver() }

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("LuchiCashPrefs", Context.MODE_PRIVATE) }

    val simboloMoneda = remember {
        sharedPreferences.getString("moneda", "S/")?.split(" ")?.get(0) ?: "S/"
    }

    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var cuentaAEditar by remember { mutableStateOf<Cuenta?>(null) }
    var cuentaAEliminar by remember { mutableStateOf<Cuenta?>(null) }

    // Diálogo para Agregar
    if (mostrarDialogoAgregar) {
        DialogoCuenta(
            titulo = "Agregar Nueva Cuenta",
            simboloMoneda = simboloMoneda,
            alDescartar = { mostrarDialogoAgregar = false },
            alGuardar = { nombre, saldo ->
                viewModel.agregarCuenta(nombre, saldo)
                mostrarDialogoAgregar = false
            }
        )
    }

    // Diálogo para Editar
    cuentaAEditar?.let { cuenta ->
        DialogoCuenta(
            titulo = "Editar Cuenta",
            nombreInicial = cuenta.nombre,
            saldoInicialStr = cuenta.saldoInicial.toString(),
            simboloMoneda = simboloMoneda,
            alDescartar = { cuentaAEditar = null },
            alGuardar = { nuevoNombre, nuevoSaldo ->
                viewModel.editarCuenta(cuenta, nuevoNombre, nuevoSaldo)
                cuentaAEditar = null
            }
        )
    }

    // Alerta de Confirmación para Eliminar
    cuentaAEliminar?.let { cuenta ->
        AlertDialog(
            onDismissRequest = { cuentaAEliminar = null },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro de que deseas eliminar la cuenta \"${cuenta.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarCuenta(cuenta)
                        cuentaAEliminar = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { cuentaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mis Cuentas",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoAgregar = true },
                containerColor = FabOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Cuenta")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Text(
                text = "Saldos por Método de Pago",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (cuentas.isEmpty()) {
                    item {
                        Text(
                            text = "No hay cuentas registradas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                } else {
                    items(cuentas) { cuenta ->
                        val saldoActual by viewModel.obtenerSaldoActualCuentaFlow(cuenta.nombre, cuenta.saldoInicial)
                            .collectAsState(initial = cuenta.saldoInicial)

                        ItemCuentaUI(
                            cuenta = cuenta,
                            saldo = saldoActual,
                            simboloMoneda = simboloMoneda,
                            onEditar = { cuentaAEditar = cuenta },
                            onEliminar = { cuentaAEliminar = cuenta }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoCuenta(
    titulo: String,
    nombreInicial: String = "",
    saldoInicialStr: String = "0.00",
    simboloMoneda: String,
    alDescartar: () -> Unit,
    alGuardar: (String, Double) -> Unit
) {
    var nombreCuenta by remember { mutableStateOf(nombreInicial) }
    var saldoInicial by remember { mutableStateOf(saldoInicialStr) }

    AlertDialog(
        onDismissRequest = alDescartar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombreCuenta,
                    onValueChange = { nombreCuenta = it },
                    placeholder = { Text("Nombre de la cuenta (Ej: BCP)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = saldoInicial,
                    onValueChange = { saldoInicial = it },
                    label = { Text("Saldo Inicial ($simboloMoneda 0.00)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saldoDouble = saldoInicial.toDoubleOrNull() ?: 0.0
                    if (nombreCuenta.isNotBlank()) {
                        alGuardar(nombreCuenta, saldoDouble)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = alDescartar,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Cancelar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun ItemCuentaUI(
    cuenta: Cuenta,
    saldo: Double,
    simboloMoneda: String,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cuenta.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$simboloMoneda ${String.format("%.2f", saldo)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (saldo < 0) AmountRed else TextGreen
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones de cuenta",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        expanded = false
                        onEditar()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        onEliminar()
                    }
                )
            }
        }
    }
}