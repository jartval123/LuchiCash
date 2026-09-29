package com.example.controlgastos.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.MainActivity
import com.example.controlgastos.data.AppDatabase
import com.example.controlgastos.viewmodel.GastosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

// Función de Hasheo única (resuelve el error de Conflicting overloads)
fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(viewModel: GastosViewModel, onVolver: () -> Unit, onCerrarSesion: () -> Unit) {
    BackHandler { onVolver() }
    val context = LocalContext.current

    val sharedPreferences = remember { context.getSharedPreferences("LuchiCashPrefs", Context.MODE_PRIVATE) }
    val correoUsuario = sharedPreferences.getString("usuarioLogueado", "") ?: ""

    var modoOscuro by remember { mutableStateOf(sharedPreferences.getBoolean("modoOscuro", false)) }
    var notificaciones by remember { mutableStateOf(sharedPreferences.getBoolean("notificaciones", true)) }
    var biometriaActivada by remember { mutableStateOf(sharedPreferences.getBoolean("biometriaActivada", false)) }

    val opcionesMoneda = listOf("S/ (Soles)", "$ (Dólares)", "€ (Euros)")
    var monedaSeleccionada by remember {
        mutableStateOf(sharedPreferences.getString("moneda", opcionesMoneda[0]) ?: opcionesMoneda[0])
    }
    var expandidoMoneda by remember { mutableStateOf(false) }

    var mostrarConfirmacionBorrado by remember { mutableStateOf(false) }
    var mostrarConfirmacionLogout by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var mostrarDialogoPin by remember { mutableStateOf(false) }
    var pinTemp by remember { mutableStateOf("") }
    var pinConfirmTemp by remember { mutableStateOf("") }
    var errorPin by remember { mutableStateOf<String?>(null) }

    // --- NUEVAS VARIABLES PARA PREGUNTA DE SEGURIDAD ---
    var mostrarDialogoPregunta by remember { mutableStateOf(false) }
    var preguntaSeguridad by remember { mutableStateOf("") }
    var respuestaSeguridad by remember { mutableStateOf("") }

    // --- BACKUP SEGURO ---
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.obtenerBaseDatos(context.applicationContext)
                        val backupTemporal = File(context.cacheDir, "backup_luchicash.db")
                        if (backupTemporal.exists()) backupTemporal.delete()
                        val ruta = backupTemporal.absolutePath.replace("'", "''")
                        db.openHelper.writableDatabase.execSQL("VACUUM INTO '$ruta'")

                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            backupTemporal.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        backupTemporal.delete()
                    }
                    Toast.makeText(context, "Backup guardado exitosamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al crear backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- RESTAURAR BACKUP ---
    val restaurarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val archivoTemporal = File(context.cacheDir, "restore_temp.db")
            try {
                val copiaExitosa = context.contentResolver.openInputStream(uri)?.use { input ->
                    archivoTemporal.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false

                if (!copiaExitosa) throw Exception("No se pudo leer el archivo seleccionado")

                val firmaSQLite = "SQLite format 3\u0000".toByteArray()
                if (archivoTemporal.length() < firmaSQLite.size) throw Exception("El archivo no es un backup válido")

                val cabecera = ByteArray(firmaSQLite.size)
                archivoTemporal.inputStream().use { it.read(cabecera) }
                if (!cabecera.contentEquals(firmaSQLite)) throw Exception("El archivo no es una base de datos SQLite válida")

                val dbTemporal = SQLiteDatabase.openDatabase(
                    archivoTemporal.absolutePath, null, SQLiteDatabase.OPEN_READONLY
                )

                try {
                    val tablasEsperadas = listOf("cuentas", "movimientos", "deudas", "cuotas", "presupuestos_fijos", "metas", "aportes_meta")
                    for (tabla in tablasEsperadas) {
                        val cursor = dbTemporal.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tabla))
                        val existe = cursor.use { it.moveToFirst() }
                        if (!existe) throw Exception("El archivo no pertenece a LuchiCash. Falta la tabla: $tabla")
                    }
                } finally {
                    dbTemporal.close()
                }

                AppDatabase.cerrarBaseDatos()
                val dbFile = context.getDatabasePath("control_gastos_db")
                archivoTemporal.copyTo(dbFile, overwrite = true)
                context.getDatabasePath("control_gastos_db-wal").delete()
                context.getDatabasePath("control_gastos_db-shm").delete()

                Toast.makeText(context, "Backup restaurado. Reiniciando...", Toast.LENGTH_SHORT).show()

                val restartIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(restartIntent)
                (context as? Activity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                archivoTemporal.delete()
            }
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
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuración", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Text("Parámetros del Sistema", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Modo Oscuro", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(checked = modoOscuro, onCheckedChange = {
                            modoOscuro = it
                            sharedPreferences.edit().putBoolean("modoOscuro", it).apply()
                        })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Alertas y Notificaciones", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(checked = notificaciones, onCheckedChange = {
                            notificaciones = it
                            sharedPreferences.edit().putBoolean("notificaciones", it).apply()
                        })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    ExposedDropdownMenuBox(
                        expanded = expandidoMoneda,
                        onExpandedChange = { expandidoMoneda = !expandidoMoneda }
                    ) {
                        OutlinedTextField(
                            value = monedaSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Moneda Principal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoMoneda) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(expanded = expandidoMoneda, onDismissRequest = { expandidoMoneda = false }) {
                            opcionesMoneda.forEach { moneda ->
                                DropdownMenuItem(
                                    text = { Text(moneda) },
                                    onClick = {
                                        monedaSeleccionada = moneda
                                        sharedPreferences.edit().putString("moneda", moneda).apply()
                                        expandidoMoneda = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Text("Seguridad y Privacidad", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Bloqueo Biométrico", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Pedir Huella, Rostro o PIN al entrar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = biometriaActivada, onCheckedChange = { activado ->
                            if (activado) {
                                errorPin = null
                                pinTemp = ""
                                pinConfirmTemp = ""
                                mostrarDialogoPin = true
                            } else {
                                biometriaActivada = false
                                sharedPreferences.edit()
                                    .putBoolean("biometriaActivada", false)
                                    .remove("pinHash")
                                    .apply()
                            }
                        })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // --- NUEVO BOTÓN PARA CONFIGURAR PREGUNTA DE SEGURIDAD ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarDialogoPregunta = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pregunta de Seguridad", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Configurar recuperación de contraseña", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text("Cuenta de Usuario", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { mostrarConfirmacionLogout = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Gestión de Datos (Backup)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedButton(
                        onClick = {
                            try {
                                val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                backupLauncher.launch("LuchiCash_Backup_$fecha.db")
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el selector: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear Backup (Drive / Local)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                restaurarLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el selector: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar Backup", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Button(
                        onClick = { mostrarConfirmacionBorrado = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar toda la información", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (mostrarConfirmacionLogout) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionLogout = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("¿Cerrar Sesión?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Tendrás que volver a ingresar tu correo y contraseña la próxima vez que abras la aplicación.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionLogout = false
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Cerrar Sesión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionLogout = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // --- DIÁLOGO PARA CONFIGURAR PREGUNTA DE SEGURIDAD ---
    if (mostrarDialogoPregunta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPregunta = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Configurar Seguridad", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Protege tu cuenta agregando una pregunta secreta en caso de que olvides tu contraseña.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = preguntaSeguridad,
                        onValueChange = { preguntaSeguridad = it },
                        label = { Text("Pregunta (Ej: Mi mascota)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = respuestaSeguridad,
                        onValueChange = { respuestaSeguridad = it },
                        label = { Text("Respuesta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (preguntaSeguridad.isNotBlank() && respuestaSeguridad.isNotBlank() && correoUsuario.isNotBlank()) {
                            coroutineScope.launch {
                                val exito = viewModel.configurarPreguntaSeguridad(correoUsuario, preguntaSeguridad.trim(), respuestaSeguridad.trim())
                                if (exito) {
                                    Toast.makeText(context, "Pregunta de seguridad guardada con éxito", Toast.LENGTH_SHORT).show()
                                    mostrarDialogoPregunta = false
                                    preguntaSeguridad = ""
                                    respuestaSeguridad = ""
                                } else {
                                    Toast.makeText(context, "Error al guardar en la nube. Revisa tu conexión.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoPregunta = false
                    preguntaSeguridad = ""
                    respuestaSeguridad = ""
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (mostrarDialogoPin) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPin = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Crear PIN de acceso", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Este PIN se usará como alternativa a la huella o el rostro para desbloquear la app.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = pinTemp,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinTemp = it },
                        label = { Text("PIN (4 a 6 dígitos)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinConfirmTemp,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinConfirmTemp = it },
                        label = { Text("Confirmar PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorPin?.let {
                        Text(it, color = Color(0xFFE53935), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            pinTemp.length !in 4..6 -> errorPin = "El PIN debe tener entre 4 y 6 dígitos."
                            pinTemp != pinConfirmTemp -> errorPin = "Los PIN no coinciden."
                            else -> {
                                sharedPreferences.edit()
                                    .putString("pinHash", hashPin(pinTemp))
                                    .putBoolean("biometriaActivada", true)
                                    .apply()
                                biometriaActivada = true
                                mostrarDialogoPin = false
                                pinTemp = ""
                                pinConfirmTemp = ""
                                errorPin = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar PIN", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoPin = false
                    pinTemp = ""
                    pinConfirmTemp = ""
                    errorPin = null
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (mostrarConfirmacionBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrado = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("¿Eliminar todos los datos?", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción es irreversible. Se eliminarán todas tus cuentas, movimientos, metas y deudas actuales.\n\n¿Estás seguro de continuar?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionBorrado = false
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    AppDatabase.obtenerBaseDatos(context.applicationContext).clearAllTables()
                                }
                                Toast.makeText(context, "Toda la información fue eliminada", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al borrar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Sí, ELIMINAR TODO", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBorrado = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}