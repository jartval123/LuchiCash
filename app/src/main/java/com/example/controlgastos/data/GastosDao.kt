package com.example.controlgastos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GastosDao {

    // --- CUENTAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarCuenta(cuenta: Cuenta): Long

    @Query("SELECT * FROM cuentas WHERE activa = 1 AND usuarioId = :usuarioId")
    fun obtenerTodasLasCuentas(usuarioId: String): Flow<List<Cuenta>>

    @Query("SELECT IFNULL(SUM(saldoInicial), 0.0) FROM cuentas WHERE activa = 1 AND usuarioId = :usuarioId")
    fun obtenerTotalSaldoInicialCuentas(usuarioId: String): Flow<Double>

    @Update
    fun actualizarCuenta(cuenta: Cuenta) // NUEVO: Permite editar cuentas

    @Delete
    fun eliminarCuenta(cuenta: Cuenta) // NUEVO: Permite eliminar cuentas

    // --- MOVIMIENTOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarMovimiento(movimiento: Movimiento): Long

    @Query("SELECT * FROM movimientos WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun obtenerMovimientosRecientes(usuarioId: String): Flow<List<Movimiento>>

    @Query("SELECT IFNULL(SUM(monto), 0.0) FROM movimientos WHERE tipo = 'INGRESO' AND usuarioId = :usuarioId")
    fun obtenerTotalIngresos(usuarioId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(monto), 0.0) FROM movimientos WHERE tipo = 'GASTO' AND usuarioId = :usuarioId")
    fun obtenerTotalGastos(usuarioId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(monto), 0.0) FROM movimientos WHERE metodoPago = :nombreCuenta AND tipo = 'INGRESO' AND usuarioId = :usuarioId")
    fun obtenerIngresosPorCuenta(nombreCuenta: String, usuarioId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(monto), 0.0) FROM movimientos WHERE metodoPago = :nombreCuenta AND tipo = 'GASTO' AND usuarioId = :usuarioId")
    fun obtenerGastosPorCuenta(nombreCuenta: String, usuarioId: String): Flow<Double>

    @Query("SELECT * FROM movimientos WHERE fecha >= :inicioDia AND fecha <= :finDia AND usuarioId = :usuarioId ORDER BY fecha DESC")
    fun obtenerMovimientosPorRango(inicioDia: Long, finDia: Long, usuarioId: String): Flow<List<Movimiento>>

    @Update
    fun actualizarMovimiento(movimiento: Movimiento)

    @Delete
    fun eliminarMovimiento(movimiento: Movimiento)

    // --- PRESUPUESTOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarPresupuesto(presupuesto: PresupuestoFijo): Long

    @Delete
    fun eliminarPresupuesto(presupuesto: PresupuestoFijo)

    @Query("SELECT * FROM presupuestos_fijos WHERE usuarioId = :usuarioId ORDER BY diaVencimiento ASC")
    fun obtenerPresupuestos(usuarioId: String): Flow<List<PresupuestoFijo>>

    @Query("SELECT IFNULL(SUM(monto), 0.0) FROM presupuestos_fijos WHERE usuarioId = :usuarioId")
    fun obtenerTotalPresupuestado(usuarioId: String): Flow<Double>

    @Update
    fun actualizarPresupuesto(presupuesto: PresupuestoFijo)

    // --- DEUDAS Y CUOTAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarDeuda(deuda: Deuda): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarCuotas(cuotas: List<Cuota>)

    @Query("SELECT * FROM deudas WHERE usuarioId = :usuarioId")
    fun obtenerTodasLasDeudas(usuarioId: String): Flow<List<Deuda>>

    @Query("SELECT * FROM cuotas WHERE deudaId = :deudaId ORDER BY nroCuota ASC")
    fun obtenerCuotasPorDeuda(deudaId: Long): Flow<List<Cuota>>

    @Update
    fun actualizarCuota(cuota: Cuota)

    @Delete
    fun eliminarDeuda(deuda: Deuda)

    @Query("DELETE FROM cuotas WHERE deudaId = :deudaId")
    fun eliminarCuotasDeDeuda(deudaId: Long)

    // INNER JOIN para filtrar cuotas que pertenezcan a las deudas del usuario actual
    @Query("SELECT c.* FROM cuotas c INNER JOIN deudas d ON c.deudaId = d.id WHERE d.usuarioId = :usuarioId AND c.pagada = 0 ORDER BY c.fechaVencimiento ASC LIMIT 1")
    fun obtenerProximaCuotaPendiente(usuarioId: String): Flow<Cuota?>

    @Query("SELECT * FROM deudas WHERE id = :deudaId")
    fun obtenerDeudaPorId(deudaId: Long): Deuda?

    // --- METAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarMeta(meta: Meta): Long

    @Query("SELECT * FROM metas WHERE usuarioId = :usuarioId ORDER BY fechaLimite ASC")
    fun obtenerTodasLasMetas(usuarioId: String): Flow<List<Meta>>

    @Update
    fun actualizarMeta(meta: Meta)

    @Delete
    fun eliminarMeta(meta: Meta)

    // --- APORTES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarAporte(aporte: AporteMeta)

    @Query("SELECT * FROM aportes_meta WHERE metaId = :metaId ORDER BY fecha DESC")
    fun obtenerAportesPorMeta(metaId: Int): Flow<List<AporteMeta>>

    // --- USUARIOS (Registro y Login Local) ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun registrarUsuario(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo AND contrasena = :contrasena LIMIT 1")
    fun loginUsuario(correo: String, contrasena: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios WHERE correo = :correo")
    fun verificarCorreoExistente(correo: String): Int

    // --- VERIFICACIÓN DE DATOS LOCALES ---
    @Query("SELECT COUNT(*) FROM cuentas WHERE usuarioId = :usuarioId")
    fun contarCuentasLocales(usuarioId: String): Int

    @Query("SELECT COUNT(*) FROM movimientos WHERE usuarioId = :usuarioId")
    fun contarMovimientosLocales(usuarioId: String): Int

    @Query("UPDATE usuarios SET contrasena = :nuevaContrasena WHERE id = :usuarioId")
    fun actualizarContrasenaLocal(usuarioId: String, nuevaContrasena: String)
}