package com.example.controlgastos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Cuenta::class,
        Movimiento::class,
        Deuda::class,
        Cuota::class,
        PresupuestoFijo::class,
        Meta::class,
        AporteMeta::class,
        Usuario::class
    ],
    version = 8, // <-- Incrementado a 6 por la reestructuración de usuarioId y el UUID
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gastosDao(): GastosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtenerBaseDatos(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_gastos_db"
                )
                    .fallbackToDestructiveMigration() // Recrea la BD limpiamente si detecta cambios de versión
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // NUEVO: cierra la conexión activa de Room y limpia el singleton.
        // Debe llamarse SIEMPRE antes de sobrescribir el archivo control_gastos_db
        // en disco (restaurar backup, borrar todo, etc.), para que no quede una
        // conexión abierta apuntando a datos que ya no coinciden con el archivo.
        fun cerrarBaseDatos() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}