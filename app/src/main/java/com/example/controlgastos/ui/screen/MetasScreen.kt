package com.example.controlgastos.ui.screen

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import com.example.controlgastos.data.Meta
import com.example.controlgastos.viewmodel.GastosViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

val MetaProgressBg = Color(0xFF757575)
val MetaProgressFill = Color(0xFFFFC107)
val MetaPinkText = Color(0xFFE91E63)
val MetaBlueIcon = Color(0xFF64B5F6)

@Composable
fun MetasScreen(
    viewModel: GastosViewModel,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }
    val metas by viewModel.metas.collectAsState()
    var mostrarDialogoCrear by remember { mutableStateOf(false) }

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
                Text("Mis Metas de Ahorro", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoCrear = true },
                containerColor = MetaPinkText,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Meta")
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (metas.isEmpty()) {
                    item {
                        Text(
                            text = "No tienes metas registradas aún.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                } else {
                    items(metas) { meta ->
                        ItemMetaCard(meta = meta, viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (mostrarDialogoCrear) {
        DialogoFormularioMeta(
            meta = null,
            onDismiss = { mostrarDialogoCrear = false },
            onGuardar = { nombre, monto, fecha, tienda, lat, lng ->
                viewModel.guardarMeta(nombre, monto, fecha, tienda, lat, lng)
                mostrarDialogoCrear = false
            }
        )
    }
}

@Composable
fun ItemMetaCard(meta: Meta, viewModel: GastosViewModel) {
    var mostrarDialogoAporte by remember { mutableStateOf(false) }
    var mostrarHistorial by remember { mutableStateOf(false) }
    var expandirMenu by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaStr = dateFormat.format(Date(meta.fechaLimite))

    val porcentaje = if (meta.montoObjetivo > 0) (meta.montoActual / meta.montoObjetivo) else 0.0
    val porcentajeInt = (porcentaje * 100).toInt().coerceAtMost(100)
    val faltante = (meta.montoObjetivo - meta.montoActual).coerceAtLeast(0.0)

    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meta.nombre, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Fecha límite: $fechaStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    if (meta.tiendaNombre.isNotBlank() || (meta.latitud != null && meta.longitud != null)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp).clickable {
                                val uriStr = if (meta.latitud != null && meta.longitud != null) {
                                    "geo:${meta.latitud},${meta.longitud}?q=${meta.latitud},${meta.longitud}(${Uri.encode(meta.tiendaNombre)})"
                                } else {
                                    "geo:0,0?q=${Uri.encode(meta.tiendaNombre)}"
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MetaBlueIcon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (meta.tiendaNombre.isNotBlank()) meta.tiendaNombre else "Ver ubicación exacta",
                                color = MetaBlueIcon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { mostrarHistorial = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Historial", tint = MetaBlueIcon)
                    }
                    Box {
                        IconButton(onClick = { expandirMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = expandirMenu, onDismissRequest = { expandirMenu = false }) {
                            DropdownMenuItem(text = { Text("Editar Meta") }, onClick = { expandirMenu = false; mostrarEditar = true })
                            DropdownMenuItem(text = { Text("Eliminar Meta") }, onClick = { expandirMenu = false; viewModel.eliminarMeta(meta) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(12.dp).background(MetaProgressBg, RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(porcentaje.toFloat().coerceIn(0f, 1f))
                        .height(12.dp)
                        .background(MetaProgressFill, RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "S/ ${String.format("%.2f", meta.montoActual)} de S/ ${String.format("%.2f", meta.montoObjetivo)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(text = "$porcentajeInt%", color = MetaPinkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Faltan: S/ ${String.format("%.2f", faltante)}", color = AmountRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (faltante > 0) {
                    Text(
                        text = "+ ABONAR",
                        color = MetaPinkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { mostrarDialogoAporte = true }.padding(4.dp)
                    )
                } else {
                    Text(text = "¡COMPLETADA!", color = AmountGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (mostrarDialogoAporte) {
        DialogoRegistrarAporte(
            onDismiss = { mostrarDialogoAporte = false },
            onGuardar = { monto, nota ->
                viewModel.registrarAporte(meta, monto, nota)
                mostrarDialogoAporte = false
            }
        )
    }

    if (mostrarHistorial) {
        DialogoHistorialAportes(meta = meta, viewModel = viewModel, onDismiss = { mostrarHistorial = false })
    }

    if (mostrarEditar) {
        DialogoFormularioMeta(
            meta = meta,
            onDismiss = { mostrarEditar = false },
            onGuardar = { nombre, monto, fecha, tienda, lat, lng ->
                viewModel.editarMeta(meta, nombre, monto, fecha, tienda, lat, lng)
                mostrarEditar = false
            }
        )
    }
}

@Composable
fun DialogoRegistrarAporte(onDismiss: () -> Unit, onGuardar: (Double, String) -> Unit) {
    var montoStr by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Registrar Aporte a la Meta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto a aportar (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota u observaciones (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoStr.toDoubleOrNull() ?: 0.0
                    if (monto > 0) onGuardar(monto, nota)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Registrar Aporte", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

@Composable
fun DialogoHistorialAportes(meta: Meta, viewModel: GastosViewModel, onDismiss: () -> Unit) {
    val aportes by viewModel.obtenerAportesDeMeta(meta.id).collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Historial de Aportes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            if (aportes.isEmpty()) {
                Text("No hay aportes registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(aportes) { aporte ->
                        val fecha = dateFormat.format(Date(aporte.fecha))
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(text = "• S/ ${String.format("%.2f", aporte.monto)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = fecha, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (aporte.nota.isNotBlank()) {
                                Text(text = aporte.nota, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFormularioMeta(
    meta: Meta?,
    onDismiss: () -> Unit,
    onGuardar: (String, Double, Long, String, Double?, Double?) -> Unit
) {
    var nombre by remember { mutableStateOf(meta?.nombre ?: "") }
    var montoStr by remember { mutableStateOf(if (meta != null) meta.montoObjetivo.toString() else "") }
    var tienda by remember { mutableStateOf(meta?.tiendaNombre ?: "") }
    var fechaMillis by remember { mutableStateOf(meta?.fechaLimite ?: System.currentTimeMillis()) }

    var latitudSeleccionada by remember { mutableStateOf(meta?.latitud) }
    var longitudSeleccionada by remember { mutableStateOf(meta?.longitud) }
    var mostrarMapaSelector by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (mostrarMapaSelector) {
        SeleccionarUbicacionScreen(
            latitudInicial = latitudSeleccionada,
            longitudInicial = longitudSeleccionada,
            onUbicacionSeleccionada = { nombreLugar: String, latLng: LatLng ->
                latitudSeleccionada = latLng.latitude
                longitudSeleccionada = latLng.longitude
                if (tienda.isBlank() || tienda.startsWith("Ubicación en Mapa")) {
                    tienda = nombreLugar
                }
            },
            onVolver = { mostrarMapaSelector = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (meta == null) "Nueva Meta" else "Editar Meta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre de la meta (Ej. Bicicleta)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = montoStr,
                        onValueChange = { montoStr = it },
                        label = { Text("Monto Objetivo (S/)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tienda,
                        onValueChange = { tienda = it },
                        label = { Text("Nombre de la Tienda / Referencia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { mostrarMapaSelector = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MetaBlueIcon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (latitudSeleccionada != null) "Ubicación seleccionada ✓" else "Elegir ubicación exacta en el mapa",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = dateFormat.format(Date(fechaMillis)),
                        onValueChange = {},
                        label = { Text("Fecha Límite") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = fechaMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    fechaMillis = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar Fecha", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val monto = montoStr.toDoubleOrNull() ?: 0.0
                        if (nombre.isNotBlank() && monto > 0) {
                            onGuardar(nombre, monto, fechaMillis, tienda, latitudSeleccionada, longitudSeleccionada)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

// --- PANTALLA DE SELECCIÓN DE UBICACIÓN CON BUSCADOR Y GPS ---
@Composable
fun SeleccionarUbicacionScreen(
    latitudInicial: Double? = null,
    longitudInicial: Double? = null,
    onUbicacionSeleccionada: (String, LatLng) -> Unit,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var ubicacionSeleccionada by remember {
        mutableStateOf(
            LatLng(latitudInicial ?: -12.0464, longitudInicial ?: -77.0428)
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacionSeleccionada, 15f)
    }

    var textoBusqueda by remember { mutableStateOf("") }
    var nombreLugarDetectado by remember { mutableStateOf("Ubicación en Mapa") }

    val buscarLugarPorTexto = { query: String ->
        if (query.isNotBlank()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val nuevaLatLon = LatLng(address.latitude, address.longitude)
                    ubicacionSeleccionada = nuevaLatLon
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(nuevaLatLon, 16f)
                    nombreLugarDetectado = address.featureName ?: query
                    Toast.makeText(context, "Lugar encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se encontró el lugar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val obtenerUbicacionActual = {
        val tienePermiso = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    ubicacionSeleccionada = currentLatLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 16f)
                    nombreLugarDetectado = "Mi ubicación actual"
                    Toast.makeText(context, "Ubicación actual obtenida", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se pudo obtener la ubicación GPS", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(context, "Se requiere permiso de ubicación para usar el GPS", Toast.LENGTH_SHORT).show()
        }
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
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar Tienda / Lugar", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val tienePermiso = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (tienePermiso) {
                        obtenerUbicacionActual()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Mi ubicación actual")
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        onUbicacionSeleccionada(nombreLugarDetectado, ubicacionSeleccionada)
                        onVolver()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirmar Ubicación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    ubicacionSeleccionada = latLng
                    nombreLugarDetectado = "Ubicación en Mapa"
                }
            ) {
                Marker(
                    state = MarkerState(position = ubicacionSeleccionada),
                    title = "Lugar seleccionado",
                    snippet = nombreLugarDetectado
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoBusqueda,
                        onValueChange = { textoBusqueda = it },
                        placeholder = { Text("Buscar tienda, centro comercial...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { buscarLugarPorTexto(textoBusqueda) }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            }
        }
    }
}