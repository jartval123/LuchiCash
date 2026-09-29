package com.example.controlgastos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.controlgastos.data.AppDatabase
import com.example.controlgastos.data.GastosRepository
import com.example.controlgastos.network.RetrofitClient
import com.example.controlgastos.network.SupabaseApiService
import com.example.controlgastos.ui.screen.AuthScreen
import com.example.controlgastos.ui.screen.CalendarioScreen
import com.example.controlgastos.ui.screen.ConfiguracionScreen
import com.example.controlgastos.ui.screen.CuentasScreen
import com.example.controlgastos.ui.screen.DashboardScreen
import com.example.controlgastos.ui.screen.DeudasScreen
import com.example.controlgastos.ui.screen.EstadisticasScreen
import com.example.controlgastos.ui.screen.MetasScreen
import com.example.controlgastos.ui.screen.PresupuestoScreen
import com.example.controlgastos.ui.screen.ReportesScreen
import com.example.controlgastos.ui.screen.hashPin
import com.example.controlgastos.ui.theme.ControlGastosTheme
import com.example.controlgastos.viewmodel.GastosViewModel
import com.example.controlgastos.viewmodel.GastosViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIALIZACIÓN DE BASE DE DATOS Y RED (RETROFIT) ---
        val database = AppDatabase.obtenerBaseDatos(applicationContext)
        val dao = database.gastosDao()

        // Creamos la instancia del Web Service de Supabase
        val supabaseApiService = RetrofitClient.instance.create(SupabaseApiService::class.java)

        // Pasamos el DAO y el servicio web al repositorio
        val repositorio = GastosRepository(dao, supabaseApiService)
        val factory = GastosViewModelFactory(repositorio)
        val viewModel = ViewModelProvider(this, factory)[GastosViewModel::class.java]

        lifecycleScope.launch {
            try {
                val cantidad = database.query("SELECT COUNT(*) FROM movimientos", null).use { c ->
                    c.moveToFirst()
                    c.getInt(0)
                }
                android.util.Log.d("RestaurarBackup", "MainActivity.onCreate: Room ve $cantidad movimientos")
            } catch (e: Exception) {
                android.util.Log.e("RestaurarBackup", "No se pudo contar movimientos vía Room", e)
            }
        }

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("LuchiCashPrefs", Context.MODE_PRIVATE) }

            // Estado del usuario logueado y su UUID real
            var usuarioLogueado by remember { mutableStateOf(sharedPreferences.getString("usuarioLogueado", null)) }
            val idGuardado = sharedPreferences.getString("usuarioId", null)

            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("modoOscuro", false)) }
            var pantallaActual by remember { mutableStateOf("dashboard") }

            val biometriaActivada = remember { sharedPreferences.getBoolean("biometriaActivada", false) }
            val pinHash = remember { sharedPreferences.getString("pinHash", null) }

            var autenticado by remember { mutableStateOf(!biometriaActivada) }
            var pinIngresado by remember { mutableStateOf("") }
            var errorPin by remember { mutableStateOf<String?>(null) }

            // Si hay sesión iniciada desde antes, inyectamos el ID al ViewModel de inmediato
            LaunchedEffect(idGuardado) {
                if (idGuardado != null) {
                    viewModel.setUsuarioIdActivo(idGuardado)
                }
            }

            val ejecutarBiometria = {
                mostrarBiometria { exito ->
                    autenticado = exito
                }
            }

            // Solo ejecutamos biometría automática si YA hay un usuario logueado al abrir la app
            LaunchedEffect(biometriaActivada, usuarioLogueado) {
                if (usuarioLogueado != null && biometriaActivada && !autenticado) {
                    ejecutarBiometria()
                }
            }

            DisposableEffect(sharedPreferences) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "modoOscuro") isDarkMode = prefs.getBoolean("modoOscuro", false)
                }
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            ControlGastosTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- ÁRBOL DE NAVEGACIÓN ---
                    if (usuarioLogueado == null) {
                        // 1. Si no hay sesión iniciada, obligamos a pasar por AuthScreen
                        AuthScreen(
                            viewModel = viewModel,
                            onLoginExitoso = { correo, uuid ->
                                // Guardamos la sesión y el UUID localmente
                                sharedPreferences.edit()
                                    .putString("usuarioLogueado", correo)
                                    .putString("usuarioId", uuid)
                                    .apply()

                                usuarioLogueado = correo
                                viewModel.setUsuarioIdActivo(uuid) // Inyectamos el ID
                                autenticado = true // Paso directo sin pedir huella
                            }
                        )
                    } else {
                        // 2. Si ya hay sesión iniciada, pasamos a la capa de seguridad (Pin/Biometría)
                        if (autenticado) {
                            // 3. App Desbloqueada -> Mostramos las pantallas regulares
                            when (pantallaActual) {
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavegarCuentas = { pantallaActual = "cuentas" },
                                    onNavegarCalendario = { pantallaActual = "calendario" },
                                    onNavegarPresupuesto = { pantallaActual = "presupuesto" },
                                    onNavegarDeudas = { pantallaActual = "deudas" },
                                    onNavegarMetas = { pantallaActual = "metas" },
                                    onNavegarEstadisticas = { pantallaActual = "estadisticas" },
                                    onNavegarConfiguracion = { pantallaActual = "configuracion" },
                                    onNavegarReportes = { pantallaActual = "reportes" }
                                )
                                "cuentas" -> CuentasScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "calendario" -> CalendarioScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "presupuesto" -> PresupuestoScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "deudas" -> DeudasScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "metas" -> MetasScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "estadisticas" -> EstadisticasScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                                "configuracion" -> ConfiguracionScreen(
                                    viewModel = viewModel,
                                    onVolver = { pantallaActual = "dashboard" },
                                    onCerrarSesion = {
                                        // Limpiamos todo el rastro de la sesión al salir
                                        sharedPreferences.edit()
                                            .remove("usuarioLogueado")
                                            .remove("usuarioId")
                                            .apply()

                                        usuarioLogueado = null
                                        pantallaActual = "dashboard"
                                        autenticado = !sharedPreferences.getBoolean(
                                            "biometriaActivada",
                                            false
                                        )
                                    }
                                )
                                "reportes" -> ReportesScreen(
                                    viewModel,
                                    onVolver = { pantallaActual = "dashboard" })
                            }
                        } else {
                            // Pantalla de bloqueo (PIN / Biometría)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // --- LOGO DE LUCHICASH EN PANTALLA DE BLOQUEO ---
                                    Image(
                                        painter = painterResource(id = R.drawable.luchicash),
                                        contentDescription = "Logo LuchiCash",
                                        modifier = Modifier
                                            .size(110.dp)
                                            .padding(bottom = 4.dp)
                                    )

                                    Text(
                                        text = "LuchiCash está bloqueada",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Usa tu huella dactilar, reconocimiento facial o el PIN de tu dispositivo para ingresar.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { ejecutarBiometria() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    ) {
                                        Text("Desbloquear con huella / rostro", fontSize = 16.sp)
                                    }

                                    if (pinHash != null) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text(
                                            text = "O ingresa tu PIN",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedTextField(
                                            value = pinIngresado,
                                            onValueChange = {
                                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                                    pinIngresado = it
                                                    errorPin = null
                                                }
                                            },
                                            label = { Text("PIN") },
                                            singleLine = true,
                                            isError = errorPin != null,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth(0.7f)
                                        )
                                        errorPin?.let {
                                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                if (hashPin(pinIngresado) == pinHash) {
                                                    autenticado = true
                                                } else {
                                                    errorPin = "PIN incorrecto"
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(0.7f)
                                        ) {
                                            Text("Ingresar con PIN", fontSize = 16.sp)
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

    private fun mostrarBiometria(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Autenticación requerida: $errString", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Huella o PIN no reconocido", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloqueo de LuchiCash")
            .setSubtitle("Usa tu huella, rostro o PIN del dispositivo")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar biometría: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(false)
        }
    }
}