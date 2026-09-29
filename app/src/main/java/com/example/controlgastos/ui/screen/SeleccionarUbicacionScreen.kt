package com.example.controlgastos.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarUbicacionScreen(
    latitudInicial: Double?,
    longitudInicial: Double?,
    onUbicacionSeleccionada: (Double, LatLng) -> Unit, // (zoom o dummy, LatLng)
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }

    // Coordenada por defecto (Ej. Centro de Lima por defecto si no hay previa)
    val ubicacionPorDefecto = LatLng(-12.0464, -77.0428)
    val posicionInicial = if (latitudInicial != null && longitudInicial != null) {
        LatLng(latitudInicial, longitudInicial)
    } else {
        ubicacionPorDefecto
    }

    var puntoSeleccionado by remember { mutableStateOf<LatLng?>(posicionInicial) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(posicionInicial, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecciona la Tienda en el Mapa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            puntoSeleccionado?.let {
                                onUbicacionSeleccionada(0.0, it)
                                onVolver()
                            }
                        },
                        enabled = puntoSeleccionado != null
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar ubicación", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
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
                    puntoSeleccionado = latLng
                }
            ) {
                puntoSeleccionado?.let { pos ->
                    Marker(
                        state = MarkerState(position = pos),
                        title = "Ubicación de la Meta",
                        snippet = "Toca el mapa para cambiar el pin"
                    )
                }
            }

            // Instrucción flotante abajo
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "Toca cualquier punto del mapa para colocar el marcador de la tienda.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}