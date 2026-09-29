package com.example.controlgastos.data

import com.example.controlgastos.network.AporteMetaSupabase
import com.example.controlgastos.network.CuentaSupabase
import com.example.controlgastos.network.CuotaSupabase
import com.example.controlgastos.network.DeudaSupabase
import com.example.controlgastos.network.MetaSupabase
import com.example.controlgastos.network.MovimientoSupabase
import com.example.controlgastos.network.PresupuestoSupabase
import com.example.controlgastos.network.SupabaseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GastosRepository(
    private val dao: GastosDao,
    private val apiService: SupabaseApiService? = null // Opcional para mantener compatibilidad
) {

    // --- LECTURA (Flows en tiempo real filtrados por usuario) ---
    fun obtenerTodasLasCuentas(usuarioId: String): Flow<List<Cuenta>> = dao.obtenerTodasLasCuentas(usuarioId)
    fun obtenerTotalIngresos(usuarioId: String): Flow<Double> = dao.obtenerTotalIngresos(usuarioId)
    fun obtenerTotalGastos(usuarioId: String): Flow<Double> = dao.obtenerTotalGastos(usuarioId)
    fun obtenerTotalSaldoInicialCuentas(usuarioId: String): Flow<Double> = dao.obtenerTotalSaldoInicialCuentas(usuarioId)
    fun obtenerMovimientosRecientes(usuarioId: String): Flow<List<Movimiento>> = dao.obtenerMovimientosRecientes(usuarioId)
    fun obtenerPresupuestos(usuarioId: String): Flow<List<PresupuestoFijo>> = dao.obtenerPresupuestos(usuarioId)
    fun obtenerTotalPresupuestado(usuarioId: String): Flow<Double> = dao.obtenerTotalPresupuestado(usuarioId)
    fun obtenerTodasLasDeudas(usuarioId: String): Flow<List<Deuda>> = dao.obtenerTodasLasDeudas(usuarioId)
    fun obtenerProximaCuotaPendiente(usuarioId: String): Flow<Cuota?> = dao.obtenerProximaCuotaPendiente(usuarioId)
    fun obtenerTodasLasMetas(usuarioId: String): Flow<List<Meta>> = dao.obtenerTodasLasMetas(usuarioId)

    // Estas no necesitan usuarioId porque se filtran por el ID padre que ya pertenece al usuario
    fun obtenerAportesPorMeta(metaId: Int): Flow<List<AporteMeta>> = dao.obtenerAportesPorMeta(metaId)
    fun obtenerCuotasPorDeuda(deudaId: Long): Flow<List<Cuota>> = dao.obtenerCuotasPorDeuda(deudaId)

    fun obtenerMovimientosPorRango(inicio: Long, fin: Long, usuarioId: String): Flow<List<Movimiento>> = dao.obtenerMovimientosPorRango(inicio, fin, usuarioId)
    fun obtenerIngresosPorCuenta(cuenta: String, usuarioId: String): Flow<Double> = dao.obtenerIngresosPorCuenta(cuenta, usuarioId)
    fun obtenerGastosPorCuenta(cuenta: String, usuarioId: String): Flow<Double> = dao.obtenerGastosPorCuenta(cuenta, usuarioId)

    // --- SINCRONIZACIÓN INICIAL INTELIGENTE (PULL) ---
    suspend fun sincronizarDatosDesdeNube(usuarioId: String) {
        try {
            if (apiService == null) return

            // Si ya existen cuentas o movimientos locales, conservamos el comportamiento
            // anterior para no duplicar movimientos ni sobrescribir datos locales.
            // IMPORTANTE: las cuotas se sincronizan aparte al final de este método,
            // por lo que ya no quedan bloqueadas por este return.
            val totalCuentasLocales = dao.contarCuentasLocales(usuarioId)
            val totalMovimientosLocales = dao.contarMovimientosLocales(usuarioId)
            val necesitaCargaInicial = totalCuentasLocales == 0 && totalMovimientosLocales == 0

            if (necesitaCargaInicial) {

                // 1. Descargar y guardar Cuentas
                val responseCuentas = apiService.obtenerCuentas("eq.$usuarioId")
                if (responseCuentas.isSuccessful && responseCuentas.body() != null) {
                    for (cuentaNube in responseCuentas.body()!!) {
                        dao.insertarCuenta(
                            Cuenta(
                                usuarioId = usuarioId,
                                nombre = cuentaNube.nombre,
                                saldoInicial = cuentaNube.saldoInicial,
                                activa = cuentaNube.activa
                            )
                        )
                    }
                }

                // 2. Descargar y guardar Movimientos
                val responseMovimientos = apiService.obtenerMovimientos("eq.$usuarioId")
                if (responseMovimientos.isSuccessful && responseMovimientos.body() != null) {
                    for (movNube in responseMovimientos.body()!!) {
                        dao.insertarMovimiento(
                            Movimiento(
                                usuarioId = usuarioId,
                                tipo = movNube.tipo,
                                descripcion = movNube.descripcion,
                                monto = movNube.monto,
                                categoria = movNube.categoria,
                                metodoPago = movNube.metodoPago,
                                fecha = movNube.fecha,
                                notas = movNube.notas,
                                comprobanteUri = movNube.comprobanteUri
                            )
                        )
                    }
                }

                // 3. Descargar y guardar Deudas (respetando ID remoto)
                val responseDeudas = apiService.obtenerDeudas("eq.$usuarioId")
                if (responseDeudas.isSuccessful && responseDeudas.body() != null) {
                    for (deudaNube in responseDeudas.body()!!) {
                        dao.insertarDeuda(
                            Deuda(
                                id = deudaNube.id,
                                usuarioId = usuarioId,
                                entidad = deudaNube.entidad,
                                tipo = deudaNube.tipo ?: "",
                                montoTotal = deudaNube.montoTotal,
                                numeroCuotas = deudaNube.numeroCuotas
                            )
                        )
                    }
                }

                // 4. Descargar y guardar Presupuestos
                val responsePresupuestos = apiService.obtenerPresupuestos("eq.$usuarioId")
                if (responsePresupuestos.isSuccessful && responsePresupuestos.body() != null) {
                    for (presNube in responsePresupuestos.body()!!) {
                        dao.insertarPresupuesto(
                            PresupuestoFijo(
                                usuarioId = usuarioId,
                                descripcion = presNube.descripcion,
                                monto = presNube.monto,
                                categoria = presNube.categoria,
                                diaVencimiento = presNube.diaVencimiento,
                                metodoPago = presNube.metodoPago,
                                ultimoMesPagado = presNube.ultimoMesPagado
                            )
                        )
                    }
                }

                // 5. Descargar y guardar Metas (respetando ID remoto)
                val responseMetas = apiService.obtenerMetas("eq.$usuarioId")
                if (responseMetas.isSuccessful && responseMetas.body() != null) {
                    for (metaNube in responseMetas.body()!!) {
                        dao.insertarMeta(
                            Meta(
                                id = metaNube.id ?: 0,
                                usuarioId = usuarioId,
                                nombre = metaNube.nombre,
                                montoObjetivo = metaNube.montoObjetivo,
                                montoActual = metaNube.montoActual,
                                fechaLimite = metaNube.fechaLimite,
                                completada = metaNube.completada,
                                tiendaNombre = metaNube.tiendaNombre,
                                latitud = metaNube.latitud,
                                longitud = metaNube.longitud
                            )
                        )
                    }
                }

                // 6. Descargar y guardar Aportes de Metas
                try {
                    val responseAportes = apiService.obtenerAportes("eq.$usuarioId")
                    if (responseAportes.isSuccessful && responseAportes.body() != null) {
                        for (aporteNube in responseAportes.body()!!) {
                            dao.insertarAporte(
                                AporteMeta(
                                    metaId = aporteNube.meta_id, // Sin conversiones, ambos son Int
                                    monto = aporteNube.monto,
                                    fecha = aporteNube.fecha,
                                    nota = aporteNube.nota
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } // fin de carga inicial

            // Las cuotas se sincronizan SIEMPRE, incluso si el dispositivo ya tenía
            // cuentas/movimientos locales. La relación correcta es: cuota -> deuda -> usuario.
            sincronizarCuotasDesdeNube(usuarioId)

        } catch (e: Exception) {
            android.util.Log.e("SYNC_NUBE", "Error general durante la sincronización", e)
        }
    }

    /**
     * Descarga las cuotas utilizando deuda_id. No se filtran por usuario_id porque
     * la tabla cuotas pertenece a una deuda y la deuda es la que pertenece al usuario.
     */
    private suspend fun sincronizarCuotasDesdeNube(usuarioId: String) {
        if (apiService == null) return

        try {
            // Room nos da las deudas que ya pertenecen al usuario actual.
            // Se usa first() porque esta parte es una sincronización puntual.
            var deudasLocales = dao.obtenerTodasLasDeudas(usuarioId).first()

            // Si por alguna razón el dispositivo tiene otros datos locales pero
            // todavía no tiene las deudas, recuperamos primero las deudas desde
            // Supabase para poder resolver sus cuotas.
            if (deudasLocales.isEmpty()) {
                val responseDeudas = apiService.obtenerDeudas("eq.$usuarioId")

                if (!responseDeudas.isSuccessful) {
                    val error = responseDeudas.errorBody()?.string()
                    android.util.Log.e(
                        "SYNC_CUOTAS",
                        "No se pudieron recuperar las deudas ${responseDeudas.code()}: $error"
                    )
                    return
                }

                val deudasNube = responseDeudas.body().orEmpty()

                for (deudaNube in deudasNube) {
                    dao.insertarDeuda(
                        Deuda(
                            id = deudaNube.id,
                            usuarioId = usuarioId,
                            entidad = deudaNube.entidad,
                            tipo = deudaNube.tipo ?: "",
                            montoTotal = deudaNube.montoTotal,
                            numeroCuotas = deudaNube.numeroCuotas
                        )
                    )
                }

                deudasLocales = dao.obtenerTodasLasDeudas(usuarioId).first()
            }

            if (deudasLocales.isEmpty()) {
                android.util.Log.d("SYNC_CUOTAS", "No hay deudas para sincronizar cuotas")
                return
            }

            var totalCuotas = 0

            for (deuda in deudasLocales) {
                val response = apiService.obtenerCuotasPorDeuda("eq.${deuda.id}")

                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    android.util.Log.e(
                        "SYNC_CUOTAS",
                        "GET cuotas deuda=${deuda.id} ERROR ${response.code()}: $error"
                    )
                    continue
                }

                val cuotasNube = response.body().orEmpty()

                if (cuotasNube.isNotEmpty()) {
                    val cuotasLocales = cuotasNube.map { cuotaNube ->
                        Cuota(
                            id = 0L,
                            deudaId = cuotaNube.deuda_id,
                            nroCuota = cuotaNube.nro_cuota,
                            montoCuota = cuotaNube.monto_cuota,
                            fechaVencimiento = cuotaNube.fecha_vencimiento,
                            pagada = cuotaNube.pagada
                        )
                    }

                    dao.insertarCuotas(cuotasLocales)
                    totalCuotas += cuotasLocales.size
                }
            }

            android.util.Log.d(
                "SYNC_CUOTAS",
                "Sincronización de cuotas finalizada. Usuario=$usuarioId, cuotas=$totalCuotas"
            )
        } catch (e: Exception) {
            android.util.Log.e("SYNC_CUOTAS", "Excepción al sincronizar cuotas", e)
        }
    }

    // --- ESCRITURA LOCAL + SINCRONIZACIÓN NUBE OPCIONAL ---
    suspend fun insertarMovimiento(movimiento: Movimiento, usuarioId: String? = null) {
        dao.insertarMovimiento(movimiento)
        try {
            if (apiService != null && !usuarioId.isNullOrBlank()) {
                apiService.insertarMovimiento(
                    MovimientoSupabase(
                        usuario_id = usuarioId,
                        tipo = movimiento.tipo,
                        descripcion = movimiento.descripcion,
                        monto = movimiento.monto,
                        categoria = movimiento.categoria,
                        metodo_pago = movimiento.metodoPago,
                        fecha = movimiento.fecha,
                        notas = movimiento.notas,
                        comprobante_uri = movimiento.comprobanteUri
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun actualizarMovimiento(movimiento: Movimiento) = dao.actualizarMovimiento(movimiento)

    suspend fun eliminarMovimiento(movimiento: Movimiento) {
        dao.eliminarMovimiento(movimiento)
        try {
            if (apiService != null) {
                apiService.eliminarMovimiento("eq.${movimiento.id}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertarCuenta(cuenta: Cuenta, usuarioId: String? = null) {
        dao.insertarCuenta(cuenta)
        try {
            if (apiService != null && !usuarioId.isNullOrBlank()) {
                apiService.insertarCuenta(
                    CuentaSupabase(
                        usuario_id = usuarioId,
                        nombre = cuenta.nombre,
                        saldo_inicial = cuenta.saldoInicial,
                        activa = cuenta.activa
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun actualizarCuenta(cuenta: Cuenta, nombreOriginal: String? = null, usuarioId: String? = null) {
        dao.actualizarCuenta(cuenta)
        try {
            if (apiService != null && !usuarioId.isNullOrBlank()) {
                val filtroNombre = nombreOriginal ?: cuenta.nombre
                val response = apiService.actualizarCuenta(
                    "eq.$usuarioId",
                    "eq.$filtroNombre",
                    com.example.controlgastos.network.CuentaUpdateSupabase(
                        nombre = cuenta.nombre,
                        saldo_inicial = cuenta.saldoInicial,
                        activa = cuenta.activa
                    )
                )
                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    android.util.Log.e("SUPABASE_CUENTA", "PATCH cuenta ERROR ${response.code()}: $error")
                } else {
                    android.util.Log.d("SUPABASE_CUENTA", "Cuenta actualizada correctamente en Supabase")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE_CUENTA", "Excepción al actualizar cuenta", e)
        }
    }

    suspend fun eliminarCuenta(cuenta: Cuenta) {
        dao.eliminarCuenta(cuenta)
        try {
            if (apiService != null) {
                apiService.eliminarCuenta("eq.${cuenta.id}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertarDeuda(deuda: Deuda, usuarioId: String? = null): Long {
        if (apiService != null && !usuarioId.isNullOrBlank()) {
            try {
                val response = apiService.insertarDeuda(
                    DeudaSupabase(
                        usuario_id = usuarioId,
                        entidad = deuda.entidad,
                        tipo = deuda.tipo,
                        monto_total = deuda.montoTotal,
                        numero_cuotas = deuda.numeroCuotas
                    )
                )
                val idRemoto = response.body()?.firstOrNull()?.id
                if (response.isSuccessful && idRemoto != null) {
                    return dao.insertarDeuda(deuda.copy(id = idRemoto))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return dao.insertarDeuda(deuda)
    }

    suspend fun insertarCuotas(cuotas: List<Cuota>, usuarioId: String? = null) {
        dao.insertarCuotas(cuotas)
        try {
            if (apiService != null && cuotas.isNotEmpty() && !usuarioId.isNullOrBlank()) {
                val cuotasSupabase = cuotas.map { cuota ->
                    CuotaSupabase(
                        deuda_id = cuota.deudaId,
                        nro_cuota = cuota.nroCuota,
                        monto_cuota = cuota.montoCuota,
                        fecha_vencimiento = cuota.fechaVencimiento,
                        pagada = cuota.pagada
                    )
                }
                val response = apiService.insertarCuotas(cuotasSupabase)
                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    android.util.Log.e("SUPABASE_CUOTAS", "POST cuotas ERROR ${response.code()}: $error")
                } else {
                    android.util.Log.d("SUPABASE_CUOTAS", "Cuotas sincronizadas correctamente: ${cuotas.size}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun actualizarCuota(cuota: Cuota) {
        dao.actualizarCuota(cuota)
        try {
            if (apiService != null) {
                apiService.actualizarCuota(
                    "eq.${cuota.deudaId}",
                    "eq.${cuota.nroCuota}",
                    mapOf("pagada" to cuota.pagada)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun eliminarCuotasDeDeuda(deudaId: Long) {
        dao.eliminarCuotasDeDeuda(deudaId)
        try {
            if (apiService != null) {
                apiService.eliminarCuotasPorDeuda("eq.$deudaId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun eliminarDeuda(deuda: Deuda) {
        // Borramos las cuotas asociadas primero en local y en la nube
        eliminarCuotasDeDeuda(deuda.id)
        dao.eliminarDeuda(deuda)
        try {
            if (apiService != null) {
                apiService.eliminarDeuda("eq.${deuda.id}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertarPresupuesto(presupuesto: PresupuestoFijo, usuarioId: String? = null) {
        dao.insertarPresupuesto(presupuesto)
        try {
            if (apiService != null && !usuarioId.isNullOrBlank()) {
                apiService.insertarPresupuesto(
                    PresupuestoSupabase(
                        usuario_id = usuarioId,
                        descripcion = presupuesto.descripcion,
                        monto = presupuesto.monto,
                        categoria = presupuesto.categoria,
                        dia_vencimiento = presupuesto.diaVencimiento,
                        metodo_pago = presupuesto.metodoPago,
                        ultimo_mes_pagado = presupuesto.ultimoMesPagado
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun actualizarPresupuesto(presupuesto: PresupuestoFijo) = dao.actualizarPresupuesto(presupuesto)

    suspend fun eliminarPresupuesto(presupuesto: PresupuestoFijo) {
        dao.eliminarPresupuesto(presupuesto)
        try {
            if (apiService != null) {
                apiService.eliminarPresupuesto("eq.${presupuesto.id}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertarMeta(meta: Meta, usuarioId: String? = null): Long {
        if (apiService != null && !usuarioId.isNullOrBlank()) {
            try {
                val response = apiService.insertarMeta(
                    MetaSupabase(
                        usuario_id = usuarioId,
                        nombre = meta.nombre,
                        monto_objetivo = meta.montoObjetivo,
                        monto_actual = meta.montoActual,
                        fecha_limite = meta.fechaLimite,
                        completada = meta.completada,
                        tienda_nombre = meta.tiendaNombre,
                        latitud = meta.latitud,
                        longitud = meta.longitud
                    )
                )
                val idRemoto = response.body()?.firstOrNull()?.id
                if (response.isSuccessful && idRemoto != null) {
                    return dao.insertarMeta(meta.copy(id = idRemoto))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return dao.insertarMeta(meta)
    }

    suspend fun actualizarMeta(meta: Meta) {
        dao.actualizarMeta(meta)
        try {
            if (apiService != null) {
                val response = apiService.actualizarMeta(
                    "eq.${meta.id}",
                    com.example.controlgastos.network.MetaUpdateSupabase(
                        nombre = meta.nombre,
                        monto_objetivo = meta.montoObjetivo,
                        monto_actual = meta.montoActual,
                        fecha_limite = meta.fechaLimite,
                        completada = meta.completada,
                        tienda_nombre = meta.tiendaNombre,
                        latitud = meta.latitud,
                        longitud = meta.longitud
                    )
                )
                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    android.util.Log.e("SUPABASE_META", "PATCH meta ERROR ${response.code()}: $error")
                } else {
                    android.util.Log.d("SUPABASE_META", "Meta actualizada correctamente en Supabase")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE_META", "Excepción al actualizar meta", e)
        }
    }

    suspend fun eliminarMeta(meta: Meta) {
        dao.eliminarMeta(meta)
        try {
            if (apiService != null) {
                apiService.eliminarMeta("eq.${meta.id}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertarAporte(aporte: AporteMeta, usuarioId: String? = null) {
        dao.insertarAporte(aporte)
        try {
            if (apiService != null && !usuarioId.isNullOrBlank()) {
                val response = apiService.insertarAporte(
                    AporteMetaSupabase(
                        meta_id = aporte.metaId, // Sin conversiones, ambos son Int
                        monto = aporte.monto,
                        fecha = aporte.fecha,
                        nota = aporte.nota
                    )
                )
                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    android.util.Log.e("SUPABASE_APORTE", "POST aporte ERROR ${response.code()}: $error")
                } else {
                    android.util.Log.d("SUPABASE_APORTE", "Aporte sincronizado correctamente")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- USUARIOS (Registro y Login Local) ---
    suspend fun registrarUsuario(usuario: com.example.controlgastos.data.Usuario): Long = dao.registrarUsuario(usuario)
    suspend fun loginUsuario(correo: String, contrasena: String): com.example.controlgastos.data.Usuario? = dao.loginUsuario(correo, contrasena)
    suspend fun verificarCorreoExistente(correo: String): Boolean = dao.verificarCorreoExistente(correo) > 0
    suspend fun actualizarContrasenaLocal(usuarioId: String, nuevaContrasena: String) {
        dao.actualizarContrasenaLocal(usuarioId, nuevaContrasena)
    }
}