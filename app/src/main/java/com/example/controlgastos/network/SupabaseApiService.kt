package com.example.controlgastos.network

import com.example.controlgastos.data.Cuenta
import com.example.controlgastos.data.Movimiento
import com.example.controlgastos.data.Deuda
import com.example.controlgastos.data.PresupuestoFijo
import com.example.controlgastos.data.Meta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    // --- USUARIOS ---
    @Headers("Prefer: return=representation")
    @POST("rest/v1/usuarios")
    suspend fun insertarUsuario(
        @Body usuario: UsuarioSupabase
    ): Response<List<UsuarioSupabase>>

    @GET("rest/v1/usuarios")
    suspend fun obtenerUsuarioPorCorreo(
        @Query("correo") correo: String // Se pasará como "eq.correo@gmail.com"
    ): Response<List<UsuarioSupabase>>

    // --- ACTUALIZAR USUARIO (CONTRASEÑA / PREGUNTAS) ---
    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/usuarios")
    suspend fun actualizarUsuario(
        @Query("id") eqId: String, // Se pasará como "eq.UUID"
        @Body camposActualizados: Map<String, String?>
    ): Response<okhttp3.ResponseBody>

    // --- CUENTAS ---
    @GET("rest/v1/cuentas")
    suspend fun obtenerCuentas(
        @Query("usuario_id") usuarioId: String
    ): Response<List<Cuenta>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/cuentas")
    suspend fun insertarCuenta(
        @Body cuenta: CuentaSupabase
    ): Response<List<Cuenta>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/cuentas")
    suspend fun actualizarCuenta(
        @Query("usuario_id") eqUsuarioId: String,
        @Query("nombre") eqNombreOriginal: String,
        @Body campos: CuentaUpdateSupabase
    ): Response<okhttp3.ResponseBody>

    @DELETE("rest/v1/cuentas")
    suspend fun eliminarCuenta(
        @Query("id") eqId: String // Se pasará como "eq.ID"
    ): Response<okhttp3.ResponseBody>

    // --- MOVIMIENTOS ---
    @GET("rest/v1/movimientos")
    suspend fun obtenerMovimientos(
        @Query("usuario_id") usuarioId: String
    ): Response<List<Movimiento>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/movimientos")
    suspend fun insertarMovimiento(
        @Body movimiento: MovimientoSupabase
    ): Response<List<Movimiento>>

    @DELETE("rest/v1/movimientos")
    suspend fun eliminarMovimiento(
        @Query("id") eqId: String
    ): Response<okhttp3.ResponseBody>

    // --- DEUDAS ---
    @GET("rest/v1/deudas")
    suspend fun obtenerDeudas(
        @Query("usuario_id") usuarioId: String
    ): Response<List<Deuda>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/deudas")
    suspend fun insertarDeuda(
        @Body deuda: DeudaSupabase
    ): Response<List<DeudaSupabaseResponse>>

    @DELETE("rest/v1/deudas")
    suspend fun eliminarDeuda(
        @Query("id") eqId: String
    ): Response<okhttp3.ResponseBody>

    // --- CUOTAS ---
    @GET("rest/v1/cuotas")
    suspend fun obtenerCuotasPorDeuda(
        @Query("deuda_id") eqDeudaId: String // Se pasará como "eq.<id>"
    ): Response<List<CuotaSupabase>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/cuotas")
    suspend fun insertarCuotas(
        @Body cuotas: List<CuotaSupabase>
    ): Response<List<Any>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/cuotas")
    suspend fun actualizarCuota(
        @Query("deuda_id") eqDeudaId: String,   // "eq.<id>"
        @Query("nro_cuota") eqNroCuota: String, // "eq.<n>"
        @Body campos: Map<String, Boolean>      // mapOf("pagada" to true)
    ): Response<okhttp3.ResponseBody>

    @DELETE("rest/v1/cuotas")
    suspend fun eliminarCuotasPorDeuda(
        @Query("deuda_id") eqDeudaId: String
    ): Response<okhttp3.ResponseBody>

    // --- PRESUPUESTOS ---
    @GET("rest/v1/presupuestos_fijos")
    suspend fun obtenerPresupuestos(
        @Query("usuario_id") usuarioId: String
    ): Response<List<PresupuestoFijo>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/presupuestos_fijos")
    suspend fun insertarPresupuesto(
        @Body presupuesto: PresupuestoSupabase
    ): Response<List<Any>>

    @DELETE("rest/v1/presupuestos_fijos")
    suspend fun eliminarPresupuesto(
        @Query("id") eqId: String
    ): Response<okhttp3.ResponseBody>

    // --- METAS ---
    @GET("rest/v1/metas")
    suspend fun obtenerMetas(
        @Query("usuario_id") usuarioId: String
    ): Response<List<Meta>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/metas")
    suspend fun insertarMeta(
        @Body meta: MetaSupabase
    ): Response<List<MetaSupabase>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/metas")
    suspend fun actualizarMeta(
        @Query("id") eqId: String,
        @Body campos: MetaUpdateSupabase
    ): Response<okhttp3.ResponseBody>

    @DELETE("rest/v1/metas")
    suspend fun eliminarMeta(
        @Query("id") eqId: String
    ): Response<okhttp3.ResponseBody>

    // --- APORTES DE METAS ---
    @GET("rest/v1/aportes_meta")
    suspend fun obtenerAportes(
        @Query("usuario_id") usuarioId: String
    ): Response<List<AporteMetaSupabase>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/aportes_meta")
    suspend fun insertarAporte(
        @Body aporte: AporteMetaSupabase
    ): Response<List<AporteMetaSupabaseResponse>>
}

// Modelos auxiliares para enviar a la nube sin romper tu Room local
// (Nota: UsuarioSupabase vive en su propio archivo, aquí dejamos solo los modelos de tablas auxiliares)

data class CuentaUpdateSupabase(
    val nombre: String,
    val saldo_inicial: Double,
    val activa: Boolean
)

data class MetaUpdateSupabase(
    val nombre: String,
    val monto_objetivo: Double,
    val monto_actual: Double,
    val fecha_limite: Long,
    val completada: Boolean,
    val tienda_nombre: String,
    val latitud: Double?,
    val longitud: Double?
)

data class CuentaSupabase(
    val usuario_id: String,
    val nombre: String,
    val saldo_inicial: Double,
    val activa: Boolean
)

data class MovimientoSupabase(
    val usuario_id: String,
    val tipo: String,
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val metodo_pago: String,
    val fecha: Long,
    val notas: String,
    val comprobante_uri: String?
)

data class DeudaSupabase(
    val usuario_id: String,
    val entidad: String,
    val tipo: String?,
    val monto_total: Double,
    val numero_cuotas: Int
)

data class DeudaSupabaseResponse(
    val id: Long
)

data class CuotaSupabase(
    val id: Long? = null,
    val deuda_id: Long,
    val nro_cuota: Int,
    val monto_cuota: Double,
    val fecha_vencimiento: Long,
    val pagada: Boolean
)

data class PresupuestoSupabase(
    val usuario_id: String,
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val dia_vencimiento: Int,
    val metodo_pago: String,
    val ultimo_mes_pagado: String
)

data class MetaSupabase(
    val id: Int? = null,       // <--- Cambiado a Int? para coincidir con Meta.id (Int)
    val usuario_id: String,
    val nombre: String,
    val monto_objetivo: Double,
    val monto_actual: Double,
    val fecha_limite: Long,
    val completada: Boolean,
    val tienda_nombre: String,
    val latitud: Double?,
    val longitud: Double?
)

data class AporteMetaSupabase(
    val meta_id: Int,          // <--- Cambiado a Int para coincidir con AporteMeta.metaId (Int)
    val monto: Double,
    val fecha: Long,
    val nota: String
)

data class AporteMetaSupabaseResponse(
    val id: Long
)