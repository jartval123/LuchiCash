package com.example.controlgastos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.data.Cuota
import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.data.Deuda
import com.example.controlgastos.data.GastosRepository
import com.example.controlgastos.data.Movimiento
import com.example.controlgastos.data.PresupuestoFijo
import com.example.controlgastos.data.Meta
import com.example.controlgastos.data.AporteMeta
import com.example.controlgastos.data.Usuario
import com.example.controlgastos.network.RetrofitClient
import com.example.controlgastos.network.SupabaseApiService
import com.example.controlgastos.network.UsuarioSupabase
import com.example.controlgastos.ui.screen.hashPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModel(private val repository: GastosRepository) : ViewModel() {

    // --- ESTADO DINÁMICO DEL USUARIO ACTIVO ---
    private val _usuarioIdActual = MutableStateFlow<String?>(null)
    val usuarioIdActual: StateFlow<String?> = _usuarioIdActual

    fun setUsuarioIdActivo(id: String) {
        _usuarioIdActual.value = id
    }

    // --- LECTURA REACTIVA (Se actualiza automáticamente al cambiar el usuario) ---

    val cuentas: StateFlow<List<Cuenta>> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTodasLasCuentas(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIngresos: StateFlow<Double> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTotalIngresos(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalGastos: StateFlow<Double> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTotalGastos(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val saldoDisponible: StateFlow<Double> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                repository.obtenerTotalSaldoInicialCuentas(id),
                repository.obtenerTotalIngresos(id),
                repository.obtenerTotalGastos(id)
            ) { inicial, ing, gas ->
                inicial + ing - gas
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val movimientosRecientes: StateFlow<List<Movimiento>> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerMovimientosRecientes(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gastosPorCategoria: StateFlow<Map<String, Double>> = movimientosRecientes
        .map { movimientos ->
            movimientos.filter { it.tipo == "GASTO" }
                .groupBy { it.categoria }
                .mapValues { entry -> entry.value.sumOf { it.monto } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val gastosPorMetodoPago: StateFlow<Map<String, Double>> = movimientosRecientes
        .map { movimientos ->
            movimientos.filter { it.tipo == "GASTO" }
                .groupBy { it.metodoPago.ifBlank { "Otros" } }
                .mapValues { entry -> entry.value.sumOf { it.monto } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val presupuestos: StateFlow<List<PresupuestoFijo>> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerPresupuestos(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPresupuestado: StateFlow<Double> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTotalPresupuestado(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deudas: StateFlow<List<Deuda>> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTodasLasDeudas(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proximaCuotaPendiente: StateFlow<Cuota?> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerProximaCuotaPendiente(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val metas: StateFlow<List<Meta>> = _usuarioIdActual
        .filterNotNull()
        .flatMapLatest { id -> repository.obtenerTodasLasMetas(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- LÓGICA DE REPORTES FINANCIEROS ---
    private val _filtroReporte = MutableStateFlow("Este Mes")
    val filtroReporte: StateFlow<String> = _filtroReporte

    fun actualizarFiltroReporte(filtro: String) {
        _filtroReporte.value = filtro
    }

    private val movimientosReporte: Flow<List<Movimiento>> = combine(
        movimientosRecientes, // Usa la lista ya filtrada por el usuario activo
        _filtroReporte
    ) { movimientos, filtro ->
        val cal = Calendar.getInstance()
        val fin = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (filtro) {
            "Este Mes" -> cal.set(Calendar.DAY_OF_MONTH, 1)
            "Mes Pasado" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                val inicio = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                return@combine movimientos.filter { it.fecha in inicio..cal.timeInMillis }
            }
            "Últimos 3 Meses" -> cal.add(Calendar.MONTH, -3)
            "Este Año" -> cal.set(Calendar.DAY_OF_YEAR, 1)
            else -> cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        movimientos.filter { it.fecha in cal.timeInMillis..fin }
    }

    val ingresosReporte: StateFlow<Double> = movimientosReporte.map { lista ->
        lista.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val gastosReporte: StateFlow<Double> = movimientosReporte.map { lista ->
        lista.filter { it.tipo == "GASTO" }.sumOf { it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoriasReporte: StateFlow<Map<String, Double>> = movimientosReporte.map { lista ->
        lista.filter { it.tipo == "GASTO" }
            .groupBy { it.categoria }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // --- MÉTODOS SIN NECESIDAD DEL ID GLOBAL (Filtros por Entidad hija) ---
    fun obtenerCuotasDeDeuda(deudaId: Long): Flow<List<Cuota>> = repository.obtenerCuotasPorDeuda(deudaId)
    fun obtenerAportesDeMeta(metaId: Int): Flow<List<AporteMeta>> = repository.obtenerAportesPorMeta(metaId)

    // --- OBTENER RANGOS Y SALDOS ESPECÍFICOS ---
    fun obtenerMovimientosPorDia(inicioDia: Long, finDia: Long): Flow<List<Movimiento>> {
        val userId = _usuarioIdActual.value ?: return flowOf(emptyList())
        return repository.obtenerMovimientosPorRango(inicioDia, finDia, userId)
    }

    fun obtenerSaldoActualCuentaFlow(nombreCuenta: String, saldoInicial: Double): Flow<Double> {
        val userId = _usuarioIdActual.value ?: return flowOf(saldoInicial)
        val ingresosFlow = repository.obtenerIngresosPorCuenta(nombreCuenta, userId)
        val gastosFlow = repository.obtenerGastosPorCuenta(nombreCuenta, userId)

        return ingresosFlow.combine(gastosFlow) { ing, gas ->
            saldoInicial + ing - gas
        }
    }


    // --- MÉTODOS DE ESCRITURA (Todos inyectan el ID dinámico) ---

    fun agregarCuenta(nombre: String, saldoInicial: Double) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarCuenta(
                    Cuenta(usuarioId = userId, nombre = nombre, saldoInicial = saldoInicial, activa = true),
                    usuarioId = userId
                )
            } catch (e: Exception) {
                Log.e("ERROR_GUARDAR", "Error al insertar cuenta: ${e.message}", e)
            }
        }
    }

    fun editarCuenta(cuentaOriginal: Cuenta, nuevoNombre: String, nuevoSaldo: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cuentaActualizada = cuentaOriginal.copy(
                    nombre = nuevoNombre,
                    saldoInicial = nuevoSaldo
                )
                repository.actualizarCuenta(
                    cuentaActualizada,
                    nombreOriginal = cuentaOriginal.nombre,
                    usuarioId = _usuarioIdActual.value
                )
            } catch (e: Exception) {
                Log.e("ERROR_CUENTA", "Error al actualizar cuenta: ${e.message}", e)
            }
        }
    }

    fun eliminarCuenta(cuenta: Cuenta) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.eliminarCuenta(cuenta)
            } catch (e: Exception) {
                Log.e("ERROR_CUENTA", "Error al eliminar cuenta: ${e.message}", e)
            }
        }
    }

    fun agregarTransaccion(tipo: String, descripcion: String, monto: Double, categoria: String, metodoPago: String, notas: String, comprobanteUri: String? = null) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarMovimiento(
                    Movimiento(usuarioId = userId, tipo = tipo, descripcion = descripcion, monto = monto, categoria = categoria, metodoPago = metodoPago, fecha = System.currentTimeMillis(), notas = notas, comprobanteUri = comprobanteUri),
                    usuarioId = userId
                )
            } catch (e: Exception) {
                Log.e("ERROR_GUARDAR", "Error al insertar movimiento: ${e.message}", e)
            }
        }
    }

    fun guardarDeudaConCronograma(entidad: String, tipo: String, montoPrestado: Double, numeroCuotas: Int, cuotasEditadas: List<Pair<Double, Long>>) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Guardamos la deuda principal pasando el usuarioId para capturar su ID remoto
                val deudaId = repository.insertarDeuda(
                    Deuda(usuarioId = userId, entidad = entidad, tipo = tipo, montoTotal = montoPrestado, numeroCuotas = numeroCuotas),
                    usuarioId = userId
                )

                // 2. Mapeamos las cuotas generadas y editadas por el usuario usando el id unificado
                val listaCuotas = cuotasEditadas.mapIndexed { index, pair ->
                    Cuota(deudaId = deudaId, nroCuota = index + 1, montoCuota = pair.first, fechaVencimiento = pair.second, pagada = false)
                }

                // 3. Guardamos localmente y enviamos a Supabase pasando el userId actual
                repository.insertarCuotas(listaCuotas, userId)
            } catch (e: Exception) {
                Log.e("ERROR_DEUDA", "Error al guardar deuda y cuotas: ${e.message}", e)
            }
        }
    }

    fun pagarCuotaDeuda(cuota: Cuota, entidadDeuda: String, metodoPagoCuenta: String) {
        val userId = _usuarioIdActual.value ?: return
        if (cuota.pagada) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarMovimiento(
                    Movimiento(usuarioId = userId, tipo = "GASTO", descripcion = "Cuota ${cuota.nroCuota} - $entidadDeuda", monto = cuota.montoCuota, categoria = "Deudas / Préstamos", metodoPago = metodoPagoCuenta, fecha = System.currentTimeMillis(), notas = "Pago de cuota"),
                    usuarioId = userId
                )
                // Actualiza tanto local como de forma remota vía PATCH en Supabase
                repository.actualizarCuota(cuota.copy(pagada = true))
            } catch (e: Exception) {
                Log.e("ERROR_PAGO_CUOTA", "Error al pagar cuota: ${e.message}", e)
            }
        }
    }

    fun agregarPresupuesto(descripcion: String, monto: Double, categoria: String, diaVencimiento: Int, metodoPago: String) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarPresupuesto(
                    PresupuestoFijo(usuarioId = userId, descripcion = descripcion, monto = monto, categoria = categoria, diaVencimiento = diaVencimiento, metodoPago = metodoPago, ultimoMesPagado = ""),
                    usuarioId = userId
                )
            } catch (e: Exception) {
                Log.e("ERROR_PRESUPUESTO", "Error al insertar presupuesto: ${e.message}", e)
            }
        }
    }

    fun pagarPresupuesto(presupuesto: PresupuestoFijo) {
        val userId = _usuarioIdActual.value ?: return
        val mesActual = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        if (presupuesto.ultimoMesPagado == mesActual) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarMovimiento(
                    Movimiento(usuarioId = userId, tipo = "GASTO", descripcion = "Pago: ${presupuesto.descripcion}", monto = presupuesto.monto, categoria = presupuesto.categoria, metodoPago = presupuesto.metodoPago, fecha = System.currentTimeMillis(), notas = "Pago automático"),
                    usuarioId = userId
                )
                repository.actualizarPresupuesto(presupuesto.copy(ultimoMesPagado = mesActual))
            } catch (e: Exception) {
                Log.e("ERROR_PRESUPUESTO", "Error al pagar presupuesto: ${e.message}", e)
            }
        }
    }

    fun guardarMeta(nombre: String, montoObjetivo: Double, fechaLimite: Long, tiendaNombre: String, latitud: Double? = null, longitud: Double? = null) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarMeta(
                    Meta(usuarioId = userId, nombre = nombre, montoObjetivo = montoObjetivo, montoActual = 0.0, fechaLimite = fechaLimite, tiendaNombre = tiendaNombre, latitud = latitud, longitud = longitud, completada = false),
                    usuarioId = userId
                )
            } catch (e: Exception) {
                Log.e("ERROR_META", "Error al guardar meta: ${e.message}", e)
            }
        }
    }

    // --- MÉTODOS QUE NO REQUIEREN INYECTAR EL ID PORQUE ACTUALIZAN SOBRE EL REGISTRO EXISTENTE ---
    fun editarMovimiento(movimientoOriginal: Movimiento, nuevaDescripcion: String, nuevoMonto: Double, nuevaCategoria: String, nuevoMetodoPago: String, nuevasNotas: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.actualizarMovimiento(movimientoOriginal.copy(descripcion = nuevaDescripcion, monto = nuevoMonto, categoria = nuevaCategoria, metodoPago = nuevoMetodoPago, notas = nuevasNotas)) }
    }
    fun eliminarMovimiento(movimiento: Movimiento) { viewModelScope.launch(Dispatchers.IO) { repository.eliminarMovimiento(movimiento) } }
    fun eliminarDeudaCompleta(deuda: Deuda) { viewModelScope.launch(Dispatchers.IO) { repository.eliminarDeuda(deuda) } }
    fun eliminarPresupuesto(presupuesto: PresupuestoFijo) { viewModelScope.launch(Dispatchers.IO) { repository.eliminarPresupuesto(presupuesto) } }
    fun editarMeta(metaOriginal: Meta, nombre: String, montoObjetivo: Double, fechaLimite: Long, tiendaNombre: String, latitud: Double? = null, longitud: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val completada = metaOriginal.montoActual >= montoObjetivo
            repository.actualizarMeta(metaOriginal.copy(nombre = nombre, montoObjetivo = montoObjetivo, fechaLimite = fechaLimite, tiendaNombre = tiendaNombre, latitud = latitud ?: metaOriginal.latitud, longitud = longitud ?: metaOriginal.longitud, completada = completada))
        }
    }
    fun eliminarMeta(meta: Meta) { viewModelScope.launch(Dispatchers.IO) { repository.eliminarMeta(meta) } }

    fun registrarAporte(meta: Meta, montoAporte: Double, nota: String) {
        val userId = _usuarioIdActual.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertarAporte(
                    AporteMeta(metaId = meta.id.toInt(), monto = montoAporte, fecha = System.currentTimeMillis(), nota = nota),
                    usuarioId = userId
                )
                val nuevoMonto = meta.montoActual + montoAporte
                repository.actualizarMeta(meta.copy(montoActual = nuevoMonto, completada = nuevoMonto >= meta.montoObjetivo))
            } catch (e: Exception) {
                Log.e("ERROR_APORTE", "Error al registrar aporte: ${e.message}", e)
            }
        }
    }

    // --- GESTIÓN DE USUARIOS CON RESTAURACIÓN Y DESCARGA DESDE SUPABASE ---

    suspend fun registrarUsuario(nombre: String, correo: String, contrasena: String, preguntaSeguridad: String, respuestaSeguridad: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (repository.verificarCorreoExistente(correo)) return@withContext null

                val uuidReal = UUID.randomUUID().toString()

                // Encriptamos la contraseña con SHA-256 (hashPin)
                val contrasenaHasheada = hashPin(contrasena)

                // 1. Guardamos localmente con la clave encriptada
                repository.registrarUsuario(Usuario(id = uuidReal, nombre = nombre, correo = correo, contrasena = contrasenaHasheada, preguntaSeguridad = preguntaSeguridad, respuestaSeguridad = respuestaSeguridad))
                _usuarioIdActual.value = uuidReal

                // 2. Enviamos la contraseña ya encriptada a Supabase
                try {
                    val apiService = RetrofitClient.instance.create(SupabaseApiService::class.java)
                    apiService.insertarUsuario(
                        UsuarioSupabase(
                            id = uuidReal,
                            nombre = nombre,
                            correo = correo,
                            contrasena = contrasenaHasheada, // <--- Aquí va el hash, no el texto plano
                            pregunta_seguridad = preguntaSeguridad,
                            respuesta_seguridad = respuestaSeguridad
                        )
                    )
                } catch (netEx: Exception) {
                    Log.e("SYNC_USUARIO", "No sincronizó a nube: ${netEx.message}")
                }
                uuidReal
            } catch (e: Exception) {
                Log.e("REGISTRO_ERROR", "Error al registrar usuario: ${e.message}", e)
                null
            }
        }
    }

    suspend fun loginUsuario(correo: String, contrasena: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Encriptamos la clave ingresada para compararla de forma segura
                val contrasenaHasheada = hashPin(contrasena)

                // 1. Intento de login local en SQLite (Room) con el hash
                val usuarioLocal = repository.loginUsuario(correo, contrasenaHasheada)
                if (usuarioLocal != null) {
                    repository.sincronizarDatosDesdeNube(usuarioLocal.id)
                    _usuarioIdActual.value = usuarioLocal.id
                    return@withContext usuarioLocal.id
                }

                // 2. Si Room está vacío (celular nuevo o reinstalación), consultamos a Supabase por correo
                try {
                    val apiService = RetrofitClient.instance.create(
                        SupabaseApiService::class.java
                    )

                    val response = apiService.obtenerUsuarioPorCorreo("eq.$correo")

                    if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                        val usuarioNube = response.body()!![0]

                        // 3. Verificamos si la contraseña encriptada de la nube coincide con la ingresada
                        val matchHash = usuarioNube.contrasena == contrasenaHasheada
                        val matchTextoPlano = usuarioNube.contrasena == contrasena

                        if (matchHash || matchTextoPlano) {
                            val claveFinalEnLocal = if (matchHash) contrasenaHasheada else contrasenaHasheada

                            // 4. Restauramos el usuario en el SQLite local con su ID real y su contraseña encriptada
                            repository.registrarUsuario(
                                Usuario(
                                    id = usuarioNube.id,
                                    nombre = usuarioNube.nombre,
                                    correo = usuarioNube.correo,
                                    contrasena = claveFinalEnLocal,
                                    preguntaSeguridad = usuarioNube.pregunta_seguridad ?: "",
                                    respuestaSeguridad = usuarioNube.respuesta_seguridad ?: ""
                                )
                            )

                            // 5. Descargamos cuentas y datos financieros desde la nube
                            repository.sincronizarDatosDesdeNube(usuarioNube.id)

                            _usuarioIdActual.value = usuarioNube.id
                            return@withContext usuarioNube.id
                        }
                    }
                } catch (netEx: Exception) {
                    Log.e("SYNC_USUARIO", "Error al buscar el usuario en la nube: ${netEx.message}")
                }

                // Si la contraseña no coincide o no existe
                null
            } catch (e: Exception) {
                Log.e("ERROR_USUARIO", "Error general en login: ${e.message}", e)
                null
            }
        }
    }

    // --- RECUPERACIÓN Y CONFIGURACIÓN DE CONTRASEÑA/SEGURIDAD ---

    suspend fun obtenerPreguntaSeguridad(correo: String): Usuario? {
        return withContext(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.instance.create(SupabaseApiService::class.java)
                val response = apiService.obtenerUsuarioPorCorreo("eq.$correo")

                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val userNube = response.body()!![0]
                    return@withContext Usuario(
                        id = userNube.id,
                        nombre = userNube.nombre,
                        correo = userNube.correo,
                        contrasena = "", // La contraseña local se asignará al restablecer
                        preguntaSeguridad = userNube.pregunta_seguridad ?: "",
                        respuestaSeguridad = userNube.respuesta_seguridad ?: ""
                    )
                }
                null
            } catch (e: Exception) {
                Log.e("RECUPERAR_PASS", "Error al obtener pregunta: ${e.message}")
                null
            }
        }
    }

    suspend fun restablecerContrasena(usuario: Usuario, nuevaContrasena: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Hasheamos la contraseña de forma segura
                val hash = hashPin(nuevaContrasena)
                val apiService = RetrofitClient.instance.create(SupabaseApiService::class.java)

                // 2. Ejecutamos el PATCH en Supabase con el formato de filtro correcto
                val response = apiService.actualizarUsuario("eq.${usuario.id}", mapOf("contrasena" to hash))

                // 3. Validamos únicamente si la petición HTTP fue exitosa
                if (response.isSuccessful) {
                    repository.actualizarContrasenaLocal(usuario.id, hash)
                    _usuarioIdActual.value = usuario.id
                    return@withContext true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Desconocido"
                    Log.e("SUPABASE_PATCH_ERROR", "Código: ${response.code()}, Error: $errorBody")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("RECUPERAR_PASS", "Excepción al restablecer contraseña: ${e.message}", e)
                false
            }
        }
    }

    // --- PERMITE A LOS USUARIOS ANTIGUOS CONFIGURAR SU PREGUNTA DESDE AJUSTES ---
    suspend fun configurarPreguntaSeguridad(correo: String, pregunta: String, respuesta: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.instance.create(SupabaseApiService::class.java)
                val response = apiService.obtenerUsuarioPorCorreo("eq.$correo")

                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val userNube = response.body()!![0]

                    val actualizacion = mapOf(
                        "pregunta_seguridad" to pregunta,
                        "respuesta_seguridad" to respuesta
                    )

                    // Se envía un PATCH a Supabase para actualizar solo las preguntas
                    val updateRes = apiService.actualizarUsuario("eq.${userNube.id}", actualizacion)

                    if (updateRes.isSuccessful) {
                        val usuarioLocalActual = repository.loginUsuario(correo, userNube.contrasena ?: "")

                        val usuarioActualizado = Usuario(
                            id = userNube.id,
                            nombre = userNube.nombre,
                            correo = userNube.correo,
                            contrasena = usuarioLocalActual?.contrasena ?: "",
                            preguntaSeguridad = pregunta,
                            respuestaSeguridad = respuesta
                        )
                        repository.registrarUsuario(usuarioActualizado)
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                Log.e("CONFIG_SEGURIDAD", "Error al configurar pregunta: ${e.message}")
                false
            }
        }
    }
}

class GastosViewModelFactory(private val repository: GastosRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastosViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}