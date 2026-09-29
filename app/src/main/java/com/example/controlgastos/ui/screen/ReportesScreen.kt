package com.example.controlgastos.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.controlgastos.viewmodel.GastosViewModel
import java.io.File
import java.io.FileWriter

@Composable
fun ReportesScreen(viewModel: GastosViewModel, onVolver: () -> Unit) {
    BackHandler { onVolver() }
    val context = LocalContext.current

    // Usamos las variables específicas de Reporte (que reaccionan al filtro)
    val ingresos by viewModel.ingresosReporte.collectAsState()
    val gastos by viewModel.gastosReporte.collectAsState()
    val gastosPorCat by viewModel.categoriasReporte.collectAsState()
    val filtroSeleccionado by viewModel.filtroReporte.collectAsState()

    val ahorroNeto = ingresos - gastos
    val porcentajeAhorro = if (ingresos > 0) ((ahorroNeto / ingresos) * 100).coerceAtLeast(0.0) else 0.0

    // Filtros de tiempo
    val filtros = listOf("Este Mes", "Mes Pasado", "Últimos 3 Meses", "Este Año")

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
                Text("Reportes Financieros", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    exportarReporteCSV(context, ingresos, gastos, ahorroNeto, porcentajeAhorro, gastosPorCat, filtroSeleccionado)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Share, contentDescription = "Exportar") },
                text = { Text("Exportar", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- 1. FILTROS TEMPORALES ---
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtros) { filtro ->
                        FilterChip(
                            selected = filtroSeleccionado == filtro,
                            onClick = { viewModel.actualizarFiltroReporte(filtro) },
                            label = { Text(filtro) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // --- 2. RESUMEN DE FLUJO DE CAJA ---
            item {
                Text("Flujo de Caja", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Ingresos", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("S/ ${String.format("%.2f", ingresos)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmountGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gastos", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("S/ ${String.format("%.2f", gastos)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmountRed)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ahorro Neto", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("S/ ${String.format("%.2f", ahorroNeto)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (ahorroNeto >= 0) MaterialTheme.colorScheme.primary else AmountRed)
                                Text("Tasa de retención: ${String.format("%.1f", porcentajeAhorro)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // --- 3. TOP FUGAS DE CAPITAL ---
            item {
                Text("Top 3 Fugas de Capital", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (gastosPorCat.isEmpty()) {
                            Text("No hay suficientes gastos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val topCategorias = gastosPorCat.toList().sortedByDescending { it.second }.take(3)
                            val maxGasto = topCategorias.firstOrNull()?.second?.coerceAtLeast(1.0) ?: 1.0

                            topCategorias.forEachIndexed { index, categoria ->
                                val porcentaje = (categoria.second / maxGasto).toFloat()
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${index + 1}. ${categoria.first}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("S/ ${String.format("%.2f", categoria.second)}", fontWeight = FontWeight.Bold, color = AmountRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { porcentaje },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = AmountRed.copy(alpha = 1f - (index * 0.2f)),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. SALUD FINANCIERA (DEUDAS Y METAS) ---
            item {
                Text("Salud Financiera", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Alerta", tint = Color(0xFFF57C00), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Carga de Deuda: Estable", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Actualmente dedicas menos del 30% de tus ingresos a pagar deudas. ¡Buen trabajo!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

// Función para generar y compartir el archivo CSV
fun exportarReporteCSV(
    context: Context,
    ingresos: Double,
    gastos: Double,
    ahorroNeto: Double,
    porcentajeAhorro: Double,
    gastosPorCat: Map<String, Double>,
    filtro: String
) {
    try {
        // Crear un archivo temporal en caché
        val fileName = "Reporte_LuchiCash_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        // Escribir cabeceras y datos
        writer.append("REPORTE FINANCIERO - LUCHICASH\n")
        writer.append("Filtro aplicado: $filtro\n\n")

        writer.append("RESUMEN DE FLUJO DE CAJA\n")
        writer.append("Ingresos (S/),Gastos (S/),Ahorro Neto (S/),Tasa Retencion (%)\n")
        writer.append("${String.format("%.2f", ingresos)},${String.format("%.2f", gastos)},${String.format("%.2f", ahorroNeto)},${String.format("%.1f", porcentajeAhorro)}\n\n")

        writer.append("DESGLOSE DE GASTOS POR CATEGORIA\n")
        writer.append("Categoria,Monto (S/)\n")

        gastosPorCat.toList().sortedByDescending { it.second }.forEach { (cat, monto) ->
            writer.append("$cat,${String.format("%.2f", monto)}\n")
        }

        writer.flush()
        writer.close()

        // Obtener URI segura usando FileProvider
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        // Crear Intent para compartir
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Mi Reporte Financiero LuchiCash")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Lanzar el menú nativo para elegir la app (WhatsApp, Gmail, Drive, etc.)
        context.startActivity(Intent.createChooser(shareIntent, "Exportar Reporte Financiero"))

    } catch (e: Exception) {
        Toast.makeText(context, "Error al generar el reporte: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}