package com.example.controlgastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "cuentas",
    indices = [Index(value = ["usuarioId", "nombre"], unique = true)] // Evita duplicar cuentas con el mismo nombre por usuario
)
data class Cuenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @SerializedName("usuario_id") val usuarioId: String = "",
    val nombre: String = "",
    @SerializedName("saldo_inicial") val saldoInicial: Double = 0.0,
    val activa: Boolean = true
)

@Entity(tableName = "movimientos", indices = [Index(value = ["usuarioId"])])
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @SerializedName("usuario_id") val usuarioId: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    @SerializedName("metodo_pago") val metodoPago: String = "",
    val fecha: Long = 0L,
    val notas: String = "",
    @SerializedName("comprobante_uri") val comprobanteUri: String? = null
)

@Entity(
    tableName = "deudas",
    indices = [Index(value = ["usuarioId", "entidad"], unique = true)] // Evita duplicar deudas con la misma entidad por usuario
)
data class Deuda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @SerializedName("usuario_id") val usuarioId: String = "",
    val entidad: String = "",
    val tipo: String = "",
    @SerializedName("monto_total") val montoTotal: Double = 0.0,
    @SerializedName("numero_cuotas") val numeroCuotas: Int = 0
)

@Entity(
    tableName = "cuotas",
    indices = [Index(value = ["deudaId", "nroCuota"], unique = true)], // Evita duplicar el número de cuota para una misma deuda
    foreignKeys = [
        ForeignKey(
            entity = Deuda::class,
            parentColumns = ["id"],
            childColumns = ["deudaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Cuota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @SerializedName("deuda_id") val deudaId: Long = 0L,
    @SerializedName("nro_cuota") val nroCuota: Int = 0,
    @SerializedName("monto_cuota") val montoCuota: Double = 0.0,
    @SerializedName("fecha_vencimiento") val fechaVencimiento: Long = 0L,
    val pagada: Boolean = false
)

@Entity(
    tableName = "presupuestos_fijos",
    indices = [Index(value = ["usuarioId", "descripcion"], unique = true)] // Evita duplicar presupuestos con la misma descripción
)
data class PresupuestoFijo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @SerializedName("usuario_id") val usuarioId: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    @SerializedName("dia_vencimiento") val diaVencimiento: Int = 1,
    @SerializedName("metodo_pago") val metodoPago: String = "Efectivo",
    @SerializedName("ultimo_mes_pagado") val ultimoMesPagado: String = ""
)

@Entity(
    tableName = "metas",
    indices = [Index(value = ["usuarioId", "nombre"], unique = true)] // Evita duplicar metas con el mismo nombre por usuario
)
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("usuario_id") val usuarioId: String = "",
    val nombre: String = "",
    @SerializedName("monto_objetivo") val montoObjetivo: Double = 0.0,
    @SerializedName("monto_actual") val montoActual: Double = 0.0,
    @SerializedName("fecha_limite") val fechaLimite: Long = 0L,
    val completada: Boolean = false,
    @SerializedName("tienda_nombre") val tiendaNombre: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Entity(
    tableName = "aportes_meta",
    indices = [Index(value = ["metaId"])],
    foreignKeys = [
        ForeignKey(
            entity = Meta::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AporteMeta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("meta_id") val metaId: Int = 0,
    val monto: Double = 0.0,
    val fecha: Long = 0L,
    val nota: String = ""
)

// --- TABLA DE USUARIOS CON ID UUID COMPATIBLE CON SUPABASE ---
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class Usuario(
    @PrimaryKey val id: String, // UUID real (coincide con Supabase)
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val tokenSincronizacion: String? = null,

    // --- CAMPOS DE RECUPERACIÓN DE CONTRASEÑA ---
    @SerializedName("pregunta_seguridad") val preguntaSeguridad: String = "",
    @SerializedName("respuesta_seguridad") val respuestaSeguridad: String = ""
)