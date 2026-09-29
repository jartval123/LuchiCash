plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Usamos el plugin moderno KSP en lugar de kapt
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.controlgastos"
    compileSdk = 37 // <-- Cambiar a 37

    defaultConfig {
        applicationId = "com.example.controlgastos"
        minSdk = 24
        targetSdk = 37 // <-- Cambiar a 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    // Iconos extendidos
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.play.services.location)

    // Librerías de Room (Usando KSP)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // <- Aquí está la corrección clave

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Retrofit para Web Services
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Convertidor de JSON a Objetos Kotlin (GSON)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp para interceptar peticiones y añadir tokens de seguridad
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}