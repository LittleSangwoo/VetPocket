import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Секреты читаются из local.properties (он не попадает в git) или из переменных окружения.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(name: String, default: String = ""): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: default).trim()

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

kotlin {
    jvmToolchain(17) // Укажите версию, которая нужна вам (17 или 21)
}

android {
    namespace = "com.vetpocket.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vetpocket.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "YANDEX_MAPKIT_KEY", secret("YANDEX_MAPKIT_KEY").asBuildConfigString())
        buildConfigField("String", "LLM_PROVIDER", secret("LLM_PROVIDER", "GROQ").asBuildConfigString())
        buildConfigField("String", "LLM_API_KEY", secret("LLM_API_KEY").asBuildConfigString())
        buildConfigField("String", "LLM_FOLDER_ID", secret("LLM_FOLDER_ID").asBuildConfigString())
        buildConfigField("String", "LLM_MODEL", secret("LLM_MODEL").asBuildConfigString())

        ndk {
            // Оставляем только архитектуру современных смартфонов
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true   // Удаляет неиспользуемый код (R8)
            isShrinkResources = true // Удаляет неиспользуемые картинки и ресурсы
            signingConfig = signingConfigs.getByName("debug") // Подписываем debug-ключом, чтобы не создавать свой ключ
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Сеть: Retrofit + kotlinx.serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Камера и распознавание текста
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    //implementation(libs.mlkit.text.recognition)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.8.0")

    // График веса
    implementation(libs.vico.compose.m3)

    // Яндекс MapKit (full — нужен для поиска организаций)
    implementation(libs.yandex.mapkit)
}
