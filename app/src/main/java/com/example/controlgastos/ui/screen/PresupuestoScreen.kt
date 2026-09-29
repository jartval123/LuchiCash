package com.example.controlgastos.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.data.PresupuestoFijo
import com.example.controlgastos.viewmodel.GastosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PresupuestoScreen(viewModel: GastosViewModel, onVolver: () -> Unit) {
    BackHandler { onVolver() }

    val presupuestos by viewModel.presupuestos.collectAsState()
    val totalPresupuestado by viewModel.totalPresupuestado.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarDialogo by remember { mutableStateOf(false) }

    if (mostrarDialogo) {
        DialogoAgregarPresupuesto(
            cuentasDisponibles = cuentas,
            alDescartar = { mostrarDialogo = false },
            alGuardar = { desc, monto, cat, dia, metodo ->
                viewModel.agregarPresupuesto(desc, monto, cat, dia, metodo)
                mostrarDialogo = false
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
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Presupuestos Fijos", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Presupuesto")
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total de Gastos Fijos (Mensual)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", totalPresupuestado ?: 0.0)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Lista de Gastos Fijos",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (presupuestos.isEmpty()) {
                    item {
                        Text(
                            "No tienes gastos fijos registrados.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(presupuestos) { pres ->
                        ItemPresupuesto(
                            presupuesto = pres,
                            alPagar = { viewModel.pagarPresupuesto(pres) },
                            alEliminar = { viewModel.eliminarPresupuesto(pres) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemPresupuesto(presupuesto: PresupuestoFijo, alPagar: () -> Unit, alEliminar: () -> Unit) {
    val mesActual = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
    val estaPagado = presupuesto.ultimoMesPagado == mesActual

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presupuesto.descripcion,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${presupuesto.categoria} • ${presupuesto.metodoPago}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vence el día ${presupuesto.diaVencimiento}",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "S/ ${String.format("%.2f", presupuesto.monto)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    )
                    IconButton(onClick = alEliminar, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = alPagar,
                enabled = !estaPagado,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (estaPagado) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primary,
                    contentColor = if (estaPagado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color(0xFFE8F5E9),
                    disabledContentColor = Color(0xFF2E7D32)
                )
            ) {
                Text(text = if (estaPagado) "PAGADO ESTE MES ✓" else "PAGAR AHORA", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoAgregarPresupuesto(
    cuentasDisponibles: List<Cuenta>,
    alDescartar: () -> Unit,
    alGuardar: (String, Double, String, Int, String) -> Unit
) {
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var diaVencimiento by remember { mutableStateOf("") }

    // Estados para mensajes de error de validación
    var errorDescripcion by remember { mutableStateOf<String?>(null) }
    var errorMonto by remember { mutableStateOf<String?>(null) }
    var errorDia by remember { mutableStateOf<String?>(null) }
    var errorCategoriaPersonalizada by remember { mutableStateOf<String?>(null) }

    val opcionesCategoria = listOf("Vivienda", "Servicios", "Suscripciones", "Educación", "Transporte", "Otro")
    var categoriaSeleccionada by remember { mutableStateOf(opcionesCategoria[0]) }
    var categoriaPersonalizada by remember { mutableStateOf("") }
    var expandidoCategoria by remember { mutableStateOf(false) }

    val opcionesMetodo = if (cuentasDisponibles.isNotEmpty()) cuentasDisponibles.map { it.nombre } else listOf("Efectivo")
    var metodoPago by remember { mutableStateOf(opcionesMetodo.firstOrNull() ?: "Efectivo") }
    var expandidoMetodo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = alDescartar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Nuevo Gasto Fijo",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Campo Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        if (it.isNotBlank()) errorDescripcion = null
                    },
                    label = { Text("Descripción (Ej. Luz) *") },
                    singleLine = true,
                    isError = errorDescripcion != null,
                    modifier = Modifier.fillMaxWidth()
                )
                errorDescripcion?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Campo Monto
                OutlinedTextField(
                    value = monto,
                    onValueChange = {
                        monto = it
                        if ((it.toDoubleOrNull() ?: 0.0) > 0.0) errorMonto = null
                    },
                    label = { Text("Monto mensual (S/) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMonto != null,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMonto?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Campo Día Vencimiento
                OutlinedTextField(
                    value = diaVencimiento,
                    onValueChange = {
                        if (it.length <= 2) {
                            diaVencimiento = it.filter { char -> char.isDigit() }
                            val d = diaVencimiento.toIntOrNull() ?: 0
                            if (d in 1..31) errorDia = null
                        }
                    },
                    label = { Text("Día de pago (1 al 31) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorDia != null,
                    modifier = Modifier.fillMaxWidth()
                )
                errorDia?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Categoría
                ExposedDropdownMenuBox(expanded = expandidoCategoria, onExpandedChange = { expandidoCategoria = !expandidoCategoria }) {
                    OutlinedTextField(
                        value = categoriaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoCategoria) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = expandidoCategoria, onDismissRequest = { expandidoCategoria = false }) {
                        opcionesCategoria.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { categoriaSeleccionada = cat; expandidoCategoria = false })
                        }
                    }
                }

                if (categoriaSeleccionada == "Otro") {
                    OutlinedTextField(
                        value = categoriaPersonalizada,
                        onValueChange = {
                            categoriaPersonalizada = it
                            if (it.isNotBlank()) errorCategoriaPersonalizada = null
                        },
                        label = { Text("Escribe la categoría *") },
                        singleLine = true,
                        isError = errorCategoriaPersonalizada != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorCategoriaPersonalizada?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                // Método de Pago
                ExposedDropdownMenuBox(expanded = expandidoMetodo, onExpandedChange = { expandidoMetodo = !expandidoMetodo }) {
                    OutlinedTextField(
                        value = metodoPago,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago / Cuenta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoMetodo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = expandidoMetodo, onDismissRequest = { expandidoMetodo = false }) {
                        opcionesMetodo.forEach { met ->
                            DropdownMenuItem(text = { Text(met) }, onClick = { metodoPago = met; expandidoMetodo = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = monto.toDoubleOrNull() ?: 0.0
                    val d = diaVencimiento.toIntOrNull() ?: 0
                    var esValido = true

                    // Validaciones al dar clic
                    if (descripcion.isBlank()) {
                        errorDescripcion = "La descripción no puede estar vacía"
                        esValido = false
                    }
                    if (m <= 0.0) {
                        errorMonto = "Ingresa un monto válido mayor a 0"
                        esValido = false
                    }
                    if (d !in 1..31) {
                        errorDia = "Ingresa un día válido del 1 al 31"
                        esValido = false
                    }

                    val catFinal = if (categoriaSeleccionada == "Otro") {
                        if (categoriaPersonalizada.isBlank()) {
                            errorCategoriaPersonalizada = "Especifica el nombre de la categoría"
                            esValido = false
                            "Otro"
                        } else {
                            categoriaPersonalizada
                        }
                    } else {
                        categoriaSeleccionada
                    }

                    if (esValido) {
                        alGuardar(descripcion.trim(), m, catFinal, d, metodoPago)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) {
                Text("Cancelar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}