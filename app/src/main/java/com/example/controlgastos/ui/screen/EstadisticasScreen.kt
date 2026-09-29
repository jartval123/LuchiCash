package com.example.controlgastos.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.viewmodel.GastosViewModel

@Composable
fun EstadisticasScreen(
    viewModel: GastosViewModel,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }

    val gastosPorCat by viewModel.gastosPorCategoria.collectAsState()
    val gastosPorMetodo by viewModel.gastosPorMetodoPago.collectAsState()
    val totalGastos by viewModel.totalGastos.collectAsState()
    val totalIngresos by viewModel.totalIngresos.collectAsState()

    val ingresosReales = totalIngresos ?: 0.0
    val gastosReales = totalGastos ?: 0.0

    val coloresGrafico = listOf(
        Color(0xFF5E00F5), Color(0xFFFFC107), Color(0xFFE91E63),
        Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF4CAF50),
        Color(0xFF3F51B5), Color(0xFF9C27B0)
    )

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
                Text("Estadísticas Financieras", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- 1. BALANCE GENERAL ---
            item {
                Text("Balance General", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val maxBalance = maxOf(ingresosReales, gastosReales).coerceAtLeast(1.0)

                        BarraComparativa(
                            titulo = "Ingresos",
                            monto = ingresosReales,
                            color = AmountGreen,
                            porcentaje = (ingresosReales / maxBalance).toFloat()
                        )
                        BarraComparativa(
                            titulo = "Gastos",
                            monto = gastosReales,
                            color = AmountRed,
                            porcentaje = (gastosReales / maxBalance).toFloat()
                        )
                    }
                }
            }

            // --- 2. GASTOS POR CATEGORÍA (ANILLO) ---
            item {
                Text("Gastos por Categoría", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))

                if (gastosPorCat.isEmpty()) {
                    Text("No hay datos suficientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalGastoCalculado = gastosPorCat.values.sum()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GraficoAnillo(valores = gastosPorCat.values.toList(), colores = coloresGrafico, total = totalGastoCalculado)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    Text(
                                        "S/ ${String.format("%.2f", totalGastoCalculado)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val categoriasLista = gastosPorCat.toList().sortedByDescending { it.second }
                            categoriasLista.forEachIndexed { index, categoria ->
                                val color = coloresGrafico[index % coloresGrafico.size]
                                val porcentaje = (categoria.second / totalGastoCalculado) * 100

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(categoria.first, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        "S/ ${String.format("%.2f", categoria.second)} (${String.format("%.1f", porcentaje)}%)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. MÉTODOS DE PAGO MÁS USADOS ---
            item {
                Text("Uso de Métodos de Pago", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))

                if (gastosPorMetodo.isEmpty()) {
                    Text("No hay datos suficientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val maxMetodo = gastosPorMetodo.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                            val metodosLista = gastosPorMetodo.toList().sortedByDescending { it.second }

                            metodosLista.forEach { metodo ->
                                BarraComparativa(
                                    titulo = metodo.first,
                                    monto = metodo.second,
                                    color = Color(0xFF2196F3), // Azul uniforme para los métodos de pago
                                    porcentaje = (metodo.second / maxMetodo).toFloat()
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) } // Margen inferior
        }
    }
}

@Composable
fun BarraComparativa(titulo: String, monto: Double, color: Color, porcentaje: Float) {
    // Animación suave de llenado para las barras
    var animar by remember { mutableStateOf(false) }
    val anchoAnimado by animateFloatAsState(
        targetValue = if (animar) porcentaje else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) { animar = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = titulo, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "S/ ${String.format("%.2f", monto)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anchoAnimado.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
fun GraficoAnillo(valores: List<Double>, colores: List<Color>, total: Double) {
    var anguloAnimado by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f, targetValue = 360f, animationSpec = tween(durationMillis = 1000)
        ) { value, _ -> anguloAnimado = value }
    }

    Canvas(modifier = Modifier.size(160.dp)) {
        var anguloInicio = -90f
        val grosorTrazo = 40f

        valores.forEachIndexed { index, valor ->
            val anguloBarrido = (valor / total).toFloat() * anguloAnimado
            val color = colores[index % colores.size]

            drawArc(
                color = color,
                startAngle = anguloInicio,
                sweepAngle = anguloBarrido,
                useCenter = false,
                style = Stroke(width = grosorTrazo, cap = StrokeCap.Butt),
                size = Size(size.width, size.height),
                topLeft = Offset(0f, 0f)
            )
            anguloInicio += anguloBarrido
        }
    }
}