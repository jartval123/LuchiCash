package com.example.controlgastos.ui.screen

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.data.Cuota
import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.data.Deuda
import com.example.controlgastos.viewmodel.GastosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeudasScreen(
    viewModel: GastosViewModel,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }

    val deudas by viewModel.deudas.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var deudaSeleccionadaParaDetalle by remember { mutableStateOf<Deuda?>(null) }

    val totalPorPagar = deudas.sumOf { it.montoTotal }

    if (mostrarDialogoCrear) {
        DialogoCrearDeuda(
            onDismiss = { mostrarDialogoCrear = false },
            onGuardar = { entidad, tipo, montoPrestadoReal, numCuotas, cuotasEditadas ->
                viewModel.guardarDeudaConCronograma(entidad, tipo, montoPrestadoReal, numCuotas, cuotasEditadas)
                mostrarDialogoCrear = false
            }
        )
    }

    if (deudaSeleccionadaParaDetalle != null) {
        DetalleDeudaScreen(
            deuda = deudaSeleccionadaParaDetalle!!,
            viewModel = viewModel,
            cuentasDisponibles = cuentas,
            onVolver = { deudaSeleccionadaParaDetalle = null }
        )
    } else {
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
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mis Préstamos y Deudas",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { mostrarDialogoCrear = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Deuda")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Total Capital Prestado", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "S/ ${String.format("%.2f", totalPorPagar)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cronogramas Activos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (deudas.isEmpty()) {
                        item {
                            Text(
                                text = "No hay deudas o préstamos registrados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    } else {
                        items(deudas) { deuda ->
                            ItemDeudaCard(deuda = deuda, viewModel = viewModel) {
                                deudaSeleccionadaParaDetalle = deuda
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDeudaCard(deuda: Deuda, viewModel: GastosViewModel, onClick: () -> Unit) {
    val cuotasFlow = viewModel.obtenerCuotasDeDeuda(deuda.id)
    val cuotas by cuotasFlow.collectAsState(initial = emptyList())
    val cuotasPagadas = cuotas.count { it.pagada }
    val progreso = if (cuotas.isNotEmpty()) cuotasPagadas.toFloat() / cuotas.size.toFloat() else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = deuda.entidad, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = deuda.tipo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "S/ ${String.format("%.2f", deuda.montoTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Prestado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$cuotasPagadas de ${cuotas.size} cuotas pagadas",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCrearDeuda(
    onDismiss: () -> Unit,
    onGuardar: (String, String, Double, Int, List<Pair<Double, Long>>) -> Unit
) {
    var entidad by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("Préstamo Personal") }
    var expandidoTipo by remember { mutableStateOf(false) }

    // Campos separados: Monto Prestado (que se guardará) y Monto con Intereses (para dividir cuotas)
    var montoPrestadoStr by remember { mutableStateOf("") }
    var montoConInteresesStr by remember { mutableStateOf("") }
    var numCuotasStr by remember { mutableStateOf("") }

    var mostrarCalendarioCronograma by remember { mutableStateOf(false) }
    var cuotasEditadasTemp by remember { mutableStateOf<List<Pair<Double, Long>>>(emptyList()) }
    var cronogramaGenerado by remember { mutableStateOf(false) }

    val tiposDeuda = listOf("Préstamo Personal", "Pago IGV", "Arbitrios", "Préstamo Negocio", "Otro")
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (mostrarCalendarioCronograma) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val calSelected = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }

                // Las cuotas se calculan usando el monto con intereses (si está vacío, toma el prestado)
                val montoParaCuotas = montoConInteresesStr.toDoubleOrNull() ?: (montoPrestadoStr.toDoubleOrNull() ?: 0.0)
                val cuotasCount = numCuotasStr.toIntOrNull() ?: 0
                val montoBase = if (cuotasCount > 0) montoParaCuotas / cuotasCount else 0.0

                val listaTemp = mutableListOf<Pair<Double, Long>>()

                for (i in 1..cuotasCount) {
                    listaTemp.add(Pair(montoBase, calSelected.timeInMillis))
                    calSelected.add(Calendar.MONTH, 1)
                }

                cuotasEditadasTemp = listaTemp
                cronogramaGenerado = true
                mostrarCalendarioCronograma = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        modifier = Modifier.fillMaxHeight(0.9f),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Registrar Nueva Deuda", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = entidad,
                    onValueChange = { entidad = it },
                    label = { Text("Entidad (Ej. BBVA, SUNAT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandidoTipo,
                    onExpandedChange = { expandidoTipo = !expandidoTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Deuda") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoTipo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandidoTipo,
                        onDismissRequest = { expandidoTipo = false }
                    ) {
                        tiposDeuda.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    expandidoTipo = false
                                }
                            )
                        }
                    }
                }

                // Monto prestado real (el que se guardará en la tabla)
                OutlinedTextField(
                    value = montoPrestadoStr,
                    onValueChange = { montoPrestadoStr = it },
                    label = { Text("Monto Prestado (Capital Real)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Monto total con intereses (solo para la división automática de cuotas)
                OutlinedTextField(
                    value = montoConInteresesStr,
                    onValueChange = { montoConInteresesStr = it },
                    label = { Text("Monto Total con Intereses (para cuotas)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = numCuotasStr,
                    onValueChange = { numCuotasStr = it },
                    label = { Text("Número de Cuotas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val prestado = montoPrestadoStr.toDoubleOrNull() ?: 0.0
                        val conIntereses = montoConInteresesStr.toDoubleOrNull() ?: prestado
                        val cuotasCount = numCuotasStr.toIntOrNull() ?: 0
                        if (prestado > 0 && conIntereses > 0 && cuotasCount > 0) {
                            mostrarCalendarioCronograma = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GENERAR CRONOGRAMA", color = MaterialTheme.colorScheme.onPrimary)
                }

                if (cronogramaGenerado) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cronograma Generado (Editable):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cuotasEditadasTemp.indices.toList()) { index ->
                            val cuotaItem = cuotasEditadasTemp[index]
                            var montoStrItem by remember { mutableStateOf(cuotaItem.first.toString()) }
                            val fechaStrItem = dateFormat.format(Date(cuotaItem.second))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cuota ${index + 1}\n$fechaStrItem",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = cuotaItem.second }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                                                    val nuevaLista = cuotasEditadasTemp.toMutableList()
                                                    nuevaLista[index] = Pair(cuotaItem.first, newCal.timeInMillis)
                                                    cuotasEditadasTemp = nuevaLista
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                    )

                                    OutlinedTextField(
                                        value = montoStrItem,
                                        onValueChange = {
                                            montoStrItem = it
                                            val nuevoMonto = it.toDoubleOrNull() ?: cuotaItem.first
                                            val nuevaLista = cuotasEditadasTemp.toMutableList()
                                            nuevaLista[index] = Pair(nuevoMonto, cuotaItem.second)
                                            cuotasEditadasTemp = nuevaLista
                                        },
                                        modifier = Modifier.width(120.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (entidad.isNotBlank() && cronogramaGenerado && cuotasEditadasTemp.isNotEmpty()) {
                        val montoPrestadoReal = montoPrestadoStr.toDoubleOrNull() ?: 0.0
                        val numCuotasReal = cuotasEditadasTemp.size

                        // Se guarda el monto prestado real en la base de datos y el cronograma con las cuotas generadas
                        onGuardar(entidad, tipoSeleccionado, montoPrestadoReal, numCuotasReal, cuotasEditadasTemp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar Deuda", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DetalleDeudaScreen(
    deuda: Deuda,
    viewModel: GastosViewModel,
    cuentasDisponibles: List<Cuenta>,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }

    val cuotasFlow = viewModel.obtenerCuotasDeDeuda(deuda.id)
    val cuotas by cuotasFlow.collectAsState(initial = emptyList())
    var cuotaParaPagar by remember { mutableStateOf<Cuota?>(null) }

    if (cuotaParaPagar != null) {
        DialogoSeleccionarCuentaPago(
            cuentas = cuentasDisponibles,
            onDismiss = { cuotaParaPagar = null },
            onCuentaSeleccionada = { nombreCuenta ->
                viewModel.pagarCuotaDeuda(cuotaParaPagar!!, deuda.entidad, nombreCuenta)
                cuotaParaPagar = null
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(text = deuda.entidad, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {
                    viewModel.eliminarDeudaCompleta(deuda)
                    onVolver()
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar Deuda", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = deuda.entidad, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Monto original prestado: S/ ${String.format("%.2f", deuda.montoTotal)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val totalPendiente = cuotas.filter { !it.pagada }.sumOf { it.montoCuota }
                    Text(text = "Pendiente a pagar: S/ ${String.format("%.2f", totalPendiente)}", color = AmountRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cuotas) { cuota ->
                    val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(cuota.fechaVencimiento))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Cuota ${cuota.nroCuota}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "Vence: $fechaStr", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "S/ ${String.format("%.2f", cuota.montoCuota)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                if (cuota.pagada) {
                                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                                        Text(text = "PAGADO", color = TagGreenText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = { cuotaParaPagar = cuota },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("PAGAR", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoSeleccionarCuentaPago(
    cuentas: List<Cuenta>,
    onDismiss: () -> Unit,
    onCuentaSeleccionada: (String) -> Unit
) {
    var cuentaSeleccionada by remember { mutableStateOf(cuentas.firstOrNull()?.nombre ?: "Efectivo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Seleccionar Cuenta de Pago", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Elige la cuenta o método de pago de donde se descontará el dinero:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val listaNombres = if (cuentas.isNotEmpty()) cuentas.map { it.nombre } else listOf("Efectivo")

                listaNombres.forEach { nombre ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cuentaSeleccionada = nombre }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cuentaSeleccionada == nombre,
                            onClick = { cuentaSeleccionada = nombre }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = nombre, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCuentaSeleccionada(cuentaSeleccionada) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirmar Pago", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}