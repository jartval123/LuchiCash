package com.example.controlgastos.ui.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.data.Movimiento
import com.example.controlgastos.ui.theme.*
import com.example.controlgastos.viewmodel.GastosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarioScreen(viewModel: GastosViewModel, onVolver: () -> Unit) {
    BackHandler { onVolver() }

    // Obtenemos las cuentas para poder pasarlas al diálogo de edición
    val cuentas by viewModel.cuentas.collectAsState()

    var fechaSeleccionadaCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var diaSeleccionado by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    // Rango seguro de milisegundos para el día seleccionado
    val inicioDiaMs = remember(fechaSeleccionadaCalendar, diaSeleccionado) {
        try {
            val cal = fechaSeleccionadaCalendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, diaSeleccionado)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    val finDiaMs = remember(fechaSeleccionadaCalendar, diaSeleccionado) {
        try {
            val cal = fechaSeleccionadaCalendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, diaSeleccionado)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    val movimientosDelDia by viewModel.obtenerMovimientosPorDia(inicioDiaMs, finDiaMs).collectAsState(initial = emptyList())

    // Estados para controlar los menús interactivos
    var movimientoSeleccionado by remember { mutableStateOf<Movimiento?>(null) }
    var mostrarMenuAcciones by remember { mutableStateOf(false) }
    var mostrarDetalles by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }

    // --- MENÚ DE OPCIONES ---
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

    // --- DIÁLOGO DE DETALLES ---
    if (mostrarDetalles && movimientoSeleccionado != null) {
        DialogoDetalleMovimiento(
            movimiento = movimientoSeleccionado!!,
            alCerrar = { mostrarDetalles = false }
        )
    }

    // --- DIÁLOGO DE EDICIÓN ---
    if (mostrarEditar && movimientoSeleccionado != null) {
        DialogoEditarTransaccion(
            movimiento = movimientoSeleccionado!!,
            cuentasDisponibles = cuentas,
            alDescartar = { mostrarEditar = false },
            alGuardar = { desc, monto, cat, metodo, notas ->
                viewModel.editarMovimiento(
                    movimientoSeleccionado!!,
                    desc,
                    monto,
                    cat,
                    metodo,
                    notas
                )
                mostrarEditar = false
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardPurple)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calendario", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            // Cabecera con Mes y Botones de Navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = fechaSeleccionadaCalendar.clone() as Calendar
                    cal.add(Calendar.MONTH, -1)
                    fechaSeleccionadaCalendar = cal
                    diaSeleccionado = 1
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.onBackground)
                }

                val formatoMes = SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "ES"))
                Text(
                    text = formatoMes.format(fechaSeleccionadaCalendar.time).replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = {
                    val cal = fechaSeleccionadaCalendar.clone() as Calendar
                    cal.add(Calendar.MONTH, 1)
                    fechaSeleccionadaCalendar = cal
                    diaSeleccionado = 1
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Días de la semana
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("D", "L", "M", "M", "J", "V", "S").forEach { dia ->
                    Text(text = dia, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cuadrícula del Calendario
            val diasDelMes = obtenerDiasDelMesCalendario(fechaSeleccionadaCalendar)

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(diasDelMes) { dia ->
                    if (dia == 0) {
                        Box(modifier = Modifier.size(40.dp))
                    } else {
                        val esSeleccionado = dia == diaSeleccionado
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (esSeleccionado) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { diaSeleccionado = dia },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dia.toString(),
                                color = if (esSeleccionado) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val formatoFechaTitulo = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaTituloStr = formatoFechaTitulo.format(Date(inicioDiaMs))
            Text(
                text = "Movimientos del $fechaTituloStr",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (movimientosDelDia.isEmpty()) {
                    item {
                        Text(
                            text = "No hay movimientos en este día.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(movimientosDelDia) { mov ->
                        ItemTransaccion(
                            movimiento = mov,
                            alHacerClic = {
                                movimientoSeleccionado = mov
                                mostrarMenuAcciones = true
                            }
                        )
                    }
                }
            }
        }
    }
}

fun obtenerDiasDelMesCalendario(calendar: Calendar): List<Int> {
    return try {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val primerDiaSemana = cal.get(Calendar.DAY_OF_WEEK) - 1
        val totalDias = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val lista = mutableListOf<Int>()
        repeat(primerDiaSemana) { lista.add(0) }
        for (i in 1..totalDias) { lista.add(i) }
        lista
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun DialogoDetalleMovimiento(movimiento: Movimiento, alCerrar: () -> Unit) {
    val context = LocalContext.current
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fechaStr = formatoFecha.format(Date(movimiento.fecha))

    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = "Detalles del Movimiento", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Tipo: ${if (movimiento.tipo == "GASTO") "Gasto" else "Ingreso"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Descripción: ${movimiento.descripcion}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Monto: S/ ${String.format("%.2f", movimiento.monto)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Categoría: ${movimiento.categoria}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Método de Pago: ${movimiento.metodoPago}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Fecha: $fechaStr", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Notas: ${if (movimiento.notas.isBlank()) "Sin notas" else movimiento.notas}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (!movimiento.comprobanteUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(movimiento.comprobanteUri), "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("CALENDARIO_ERROR", "No se pudo abrir el comprobante: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Comprobante Adjunto \uD83D\uDCCE", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = alCerrar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("OK", color = Color.White)
            }
        }
    )
}