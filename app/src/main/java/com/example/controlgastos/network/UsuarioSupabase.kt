package com.example.controlgastos.network

import com.google.gson.annotations.SerializedName

data class UsuarioSupabase(
    val id: String,
    val nombre: String,
    val correo: String,
    val contrasena: String? = null, // Contraseña encriptada (hash)
    @SerializedName("pregunta_seguridad") val pregunta_seguridad: String? = null,
    @SerializedName("respuesta_seguridad") val respuesta_seguridad: String? = null
)