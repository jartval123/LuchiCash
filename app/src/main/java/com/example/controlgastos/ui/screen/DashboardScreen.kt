package com.example.controlgastos.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.data.Cuota
import com.example.controlgastos.data.Deuda
import com.example.controlgastos.data.Movimiento
import com.example.controlgastos.viewmodel.GastosViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val AmountRed = Color(0xFFE53935)
val AmountGreen = Color(0xFF4CAF50)
val TagRedBg = Color(0xFFFFEBEE)
val TagRedText = Color(0xFFEF5350)
val TagGreenBg = Color(0xFFE8F5E9)
val TagGreenText = Color(0xFF2E7D32)

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: GastosViewModel,
    onNavegarCuentas: () -> Unit = {},
    onNavegarCalendario: () -> Unit = {},
    onNavegarPresupuesto: () -> Unit = {},
    onNavegarDeudas: () -> Unit = {},
    onNavegarMetas: () -> Unit = {},
    onNavegarEstadisticas: () -> Unit = {},
    onNavegarConfiguracion: () -> Unit = {},
    onNavegarReportes: () -> Unit = {}
) {
    val totalIngresos by viewModel.totalIngresos.collectAsState()
    val totalGastos by viewModel.totalGastos.collectAsState()
    val saldoDisponible by viewModel.saldoDisponible.collectAsState()
    val transacciones by viewModel.movimientosRecientes.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    val deudas by viewModel.deudas.collectAsState()
    val proximaCuota by viewModel.proximaCuotaPendiente.collectAsState()

    val deudaAsociada = proximaCuota?.let { cuota ->
        deudas.find { it.id == cuota.deudaId }
    }

    val ingresosReales = totalIngresos ?: 0.0
    val gastosReales = totalGastos ?: 0.0

    var mostrarDialogo by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoInfo by remember { mutableStateOf(false) }

    var movimientoSeleccionado by remember { mutableStateOf<Movimiento?>(null) }
    var mostrarMenuAcciones by remember { mutableStateOf(false) }
    var mostrarDetalles by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }

    if (mostrarDialogoInfo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoInfo = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Información",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "DESARROLLADO POR:\n\nGrupo: LuchiCode\n- Gian Valencia\n- Carlos Rocano\n- Melissa Torres\n- Luis Salazar\n- Hans Castro\n\n2026",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { mostrarDialogoInfo = false }) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (mostrarDialogo != null) {
        DialogoAgregarTransaccion(
            tipo = mostrarDialogo!!,
            cuentasDisponibles = cuentas,
            alDescartar = { mostrarDialogo = null },
            alGuardar = { desc, monto, cat, metodo, notas, uri ->
                viewModel.agregarTransaccion(
                    tipo = mostrarDialogo!!,
                    descripcion = desc,
                    monto = monto,
                    categoria = cat,
                    metodoPago = metodo,
                    notas = notas,
                    comprobanteUri = uri
                )
                mostrarDialogo = null
            }
        )
    }

    if (mostrarMenuAcciones && movimientoSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarMenuAcciones = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = movimientoSeleccionado!!.descripcion, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            mostrarMenuAcciones = false
                            mostrarDetalles = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver detalles", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            mostrarMenuAcciones = false
                            mostrarEditar = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar", color = Color(0xFF2196F3), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            viewModel.eliminarMovimiento(movimientoSeleccionado!!)
                            mostrarMenuAcciones = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar", color = AmountRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarMenuAcciones = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (mostrarDetalles && movimientoSeleccionado != null) {
        val mov = movimientoSeleccionado!!
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { mostrarDetalles = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Detalles del Movimiento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tipo: ${mov.tipo}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Descripción: ${mov.descripcion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Monto: S/ ${String.format("%.2f", mov.monto)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Categoría: ${mov.categoria}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Método de Pago: ${mov.metodoPago}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fecha: ${formatoFecha.format(Date(mov.fecha))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Notas: ${if (mov.notas.isNotBlank()) mov.notas else "Sin notas"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { mostrarDetalles = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("OK", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        )
    }

    if (mostrarEditar && movimientoSeleccionado != null) {
        DialogoEditarTransaccion(
            movimiento = movimientoSeleccionado!!,
            cuentasDisponibles = cuentas,
            alDescartar = { mostrarEditar = false },
            alGuardar = { desc, monto, cat, metodo, notas ->
                viewModel.editarMovimiento(movimientoSeleccionado!!, desc, monto, cat, metodo, notas)
                mostrarEditar = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TarjetaSaldo(saldoDisponible, ingresosReales, gastosReales, onInfoClick = { mostrarDialogoInfo = true }) }

        item { TarjetaAlerta(proximaCuota, deudaAsociada, onVerClick = { onNavegarDeudas() }) }

        item {
            MenuBotones(
                alPresionarIngreso = { mostrarDialogo = "INGRESO" },
                alPresionarGasto = { mostrarDialogo = "GASTO" },
                alPresionarCuentas = { onNavegarCuentas() },
                alPresionarCalendario = { onNavegarCalendario() },
                alPresionarPresupuesto = { onNavegarPresupuesto() },
                alPresionarDeudas = { onNavegarDeudas() },
                alPresionarMetas = { onNavegarMetas() },
                alPresionarEstadisticas = { onNavegarEstadisticas() },
                alPresionarConfiguracion = { onNavegarConfiguracion() },
                alPresionarReportes = { onNavegarReportes() }
            )
        }
        item {
            Text(
                text = "Movimientos Recientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transacciones.isEmpty()) {
            item { Text("No hay movimientos recientes.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            items(transacciones) { transaccion ->
                ItemTransaccion(
                    movimiento = transaccion,
                    alHacerClic = {
                        movimientoSeleccionado = transaccion
                        mostrarMenuAcciones = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoEditarTransaccion(
    movimiento: Movimiento,
    cuentasDisponibles: List<Cuenta>,
    alDescartar: () -> Unit,
    alGuardar: (String, Double, String, String, String) -> Unit
) {
    var descripcion by remember { mutableStateOf(movimiento.descripcion) }
    var monto by remember { mutableStateOf(movimiento.monto.toString()) }
    var notas by remember { mutableStateOf(movimiento.notas) }

    val opcionesCategoria = if (movimiento.tipo == "GASTO") {
        listOf("Alimentación", "Transporte", "Educación", "Salud", "Entretenimiento", "Servicios", "Otro")
    } else {
        listOf("Sueldo", "Negocio", "Inversiones", "Regalos", "Otro")
    }

    val opcionesMetodo = if (cuentasDisponibles.isNotEmpty()) {
        cuentasDisponibles.map { it.nombre }
    } else {
        listOf("Efectivo")
    }

    var categoriaSeleccionada by remember { mutableStateOf(if (opcionesCategoria.contains(movimiento.categoria)) movimiento.categoria else "Otro") }
    var metodoPago by remember { mutableStateOf(if (opcionesMetodo.contains(movimiento.metodoPago)) movimiento.metodoPago else opcionesMetodo.first()) }

    var expandidoCategoria by remember { mutableStateOf(false) }
    var expandidoMetodo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = alDescartar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = "Editar ${if (movimiento.tipo == "GASTO") "Gasto" else "Ingreso"}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto (S/)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

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

                ExposedDropdownMenuBox(expanded = expandidoMetodo, onExpandedChange = { expandidoMetodo = !expandidoMetodo }) {
                    OutlinedTextField(
                        value = metodoPago,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoMetodo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = expandidoMetodo, onDismissRequest = { expandidoMetodo = false }) {
                        opcionesMetodo.forEach { met ->
                            DropdownMenuItem(text = { Text(met) }, onClick = { metodoPago = met; expandidoMetodo = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    if (descripcion.isNotBlank() && montoDouble > 0) {
                        alGuardar(descripcion, montoDouble, categoriaSeleccionada, metodoPago, notas)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoAgregarTransaccion(
    tipo: String,
    cuentasDisponibles: List<Cuenta>,
    alDescartar: () -> Unit,
    alGuardar: (String, Double, String, String, String, String?) -> Unit
) {
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var comprobanteUri by remember { mutableStateOf<String?>(null) }
    var mostrarOpcionesFoto by remember { mutableStateOf(false) }

    // Estados para mensajes de error de validación
    var errorDescripcion by remember { mutableStateOf<String?>(null) }
    var errorMonto by remember { mutableStateOf<String?>(null) }
    var errorCategoriaPersonalizada by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galeriaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { comprobanteUri = it.toString() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { tempCameraUri?.let { comprobanteUri = it.toString() } }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "comprobante_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para adjuntar comprobantes", Toast.LENGTH_SHORT).show()
        }
    }

    val opcionesCategoria = if (tipo == "GASTO") {
        listOf("Alimentación", "Transporte", "Educación", "Salud", "Entretenimiento", "Servicios", "Otro")
    } else {
        listOf("Sueldo", "Negocio", "Inversiones", "Regalos", "Otro")
    }

    val opcionesMetodo = if (cuentasDisponibles.isNotEmpty()) cuentasDisponibles.map { it.nombre } else listOf("Efectivo")

    var categoriaSeleccionada by remember { mutableStateOf(opcionesCategoria[0]) }
    var categoriaPersonalizada by remember { mutableStateOf("") }
    var metodoPago by remember { mutableStateOf(opcionesMetodo.firstOrNull() ?: "Efectivo") }

    var expandidoCategoria by remember { mutableStateOf(false) }
    var expandidoMetodo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = alDescartar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = if (tipo == "GASTO") "Agregar Gasto" else "Agregar Ingreso", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        if (it.isNotBlank()) errorDescripcion = null
                    },
                    label = { Text("Descripción *") },
                    singleLine = true,
                    isError = errorDescripcion != null,
                    modifier = Modifier.fillMaxWidth()
                )
                errorDescripcion?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = monto,
                    onValueChange = {
                        monto = it
                        if ((it.toDoubleOrNull() ?: 0.0) > 0.0) errorMonto = null
                    },
                    label = { Text("Monto (S/) *") },
                    singleLine = true,
                    isError = errorMonto != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMonto?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

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
                        opcionesCategoria.forEach { seleccion ->
                            DropdownMenuItem(text = { Text(seleccion) }, onClick = { categoriaSeleccionada = seleccion; expandidoCategoria = false })
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
                        opcionesMetodo.forEach { seleccion ->
                            DropdownMenuItem(text = { Text(seleccion) }, onClick = { metodoPago = seleccion; expandidoMetodo = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = { mostrarOpcionesFoto = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (comprobanteUri == null) "Adjuntar Comprobante (Foto)" else "¡Comprobante adjuntado! ✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    var esValido = true

                    if (descripcion.isBlank()) {
                        errorDescripcion = "La descripción no puede estar vacía"
                        esValido = false
                    }
                    if (montoDouble <= 0.0) {
                        errorMonto = "Ingresa un monto válido mayor a 0"
                        esValido = false
                    }
                    val categoriaFinal = if (categoriaSeleccionada == "Otro") {
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
                        alGuardar(descripcion.trim(), montoDouble, categoriaFinal, metodoPago, notas.trim(), comprobanteUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesFoto = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Seleccionar Comprobante", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            mostrarOpcionesFoto = false
                            galeriaLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Subir desde Galería", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(
                        onClick = {
                            mostrarOpcionesFoto = false
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                val photoFile = File(context.cacheDir, "comprobante_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Tomar Foto con Cámara", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarOpcionesFoto = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun MenuBotones(
    alPresionarIngreso: () -> Unit,
    alPresionarGasto: () -> Unit,
    alPresionarCuentas: () -> Unit,
    alPresionarCalendario: () -> Unit,
    alPresionarPresupuesto: () -> Unit,
    alPresionarDeudas: () -> Unit,
    alPresionarMetas: () -> Unit,
    alPresionarEstadisticas: () -> Unit,
    alPresionarConfiguracion: () -> Unit,
    alPresionarReportes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonMenu("Ingresos", Color(0xFF4CAF50), Icons.Default.Add, Modifier.weight(1f)) { alPresionarIngreso() }
            BotonMenu("Gastos", Color(0xFFF44336), Icons.Default.Clear, Modifier.weight(1f)) { alPresionarGasto() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonMenu("Calendario", Color(0xFF2196F3), Icons.Default.DateRange, Modifier.weight(1f)) { alPresionarCalendario() }
            BotonMenu("Presupuesto", Color(0xFF607D8B), Icons.Default.Build, Modifier.weight(1f)) { alPresionarPresupuesto() }
        }
        FilaBotones(
            Pair("Metas", Color(0xFFE91E63) to Icons.Default.Star) to { alPresionarMetas() },
            Pair("Estadísticas", Color(0xFF009688) to Icons.AutoMirrored.Filled.List) to { alPresionarEstadisticas() }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonMenu("Cuentas", Color(0xFFFF9800), Icons.Default.Build, Modifier.weight(1f)) { alPresionarCuentas() }
            BotonMenu("Deudas", Color(0xFF9C27B0), Icons.Default.DateRange, Modifier.weight(1f)) { alPresionarDeudas() }
        }
        FilaBotones(
            Pair("Reportes", Color(0xFF3F51B5) to Icons.Default.Menu) to { alPresionarReportes() },
            Pair("Configuración", Color(0xFF795548) to Icons.Default.Settings) to { alPresionarConfiguracion() }
        )
    }
}

@Composable
fun FilaBotones(btn1: Pair<Pair<String, Pair<Color, ImageVector>>, () -> Unit>, btn2: Pair<Pair<String, Pair<Color, ImageVector>>, () -> Unit>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonMenu(text = btn1.first.first, color = btn1.first.second.first, icon = btn1.first.second.second, modifier = Modifier.weight(1f), onClick = btn1.second)
        BotonMenu(text = btn2.first.first, color = btn2.first.second.first, icon = btn2.first.second.second, modifier = Modifier.weight(1f), onClick = btn2.second)
    }
}

@Composable
fun BotonMenu(text: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(50),
        modifier = modifier.height(48.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ItemTransaccion(movimiento: Movimiento, alHacerClic: () -> Unit) {
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fechaString = formatoFecha.format(Date(movimiento.fecha))
    val esGasto = movimiento.tipo == "GASTO"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alHacerClic() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = movimiento.descripcion, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = movimiento.categoria, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(text = movimiento.metodoPago, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(text = fechaString, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                if (!movimiento.comprobanteUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "📎 Con comprobante",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (esGasto) TagRedBg else TagGreenBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (esGasto) "Gasto" else "Ingreso",
                        color = if (esGasto) TagRedText else TagGreenText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${if (esGasto) "-" else "+"} S/ ${String.format("%.2f", movimiento.monto)}",
                    color = if (esGasto) AmountRed else AmountGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun TarjetaSaldo(saldo: Double, ingresos: Double, gastos: Double, onInfoClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Saldo Disponible", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
                IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text(
                text = "S/ ${String.format("%.2f", saldo)}",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Ingresos", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(text = "S/ ${String.format("%.2f", ingresos)}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Gastos", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(text = "S/ ${String.format("%.2f", gastos)}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TarjetaAlerta(proximaCuota: Cuota?, deudaAsociada: Deuda?, onVerClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (proximaCuota != null && deudaAsociada != null) {
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaStr = formatoFecha.format(Date(proximaCuota.fechaVencimiento))

                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Próximo pago: ${deudaAsociada.entidad}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Cuota ${proximaCuota.nroCuota} - S/ ${String.format("%.2f", proximaCuota.montoCuota)} el $fechaStr", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 14.sp)
                }
                TextButton(
                    onClick = onVerClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text("VER", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Todo al día", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "No tienes pagos pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }
    }
}