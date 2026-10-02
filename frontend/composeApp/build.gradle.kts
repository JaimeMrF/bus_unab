import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)

            // Serialization & DateTime
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)


            // Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            // Firebase KMP
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.messaging)
        }

        // Registra explícitamente los directorios generados por Compose Resources
        // para que el compilador de Kotlin los encuentre en todos los entornos
        commonMain {
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonResClass")
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors")
        }


        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.android)

            // QR (solo Android — qrcode-kotlin 4.x no tiene artefactos iosArm64)
            implementation(libs.qrcode.kotlin)

            // Ktor engine
            implementation(libs.ktor.client.okhttp)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)

            // Maps — MapLibre + OpenFreeMap (sin API key ni tarjeta)
            implementation(libs.maplibre.android)
            implementation(libs.play.services.location)

            // Firebase Android
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.7.0"))
            implementation(libs.firebase.messaging.android)

            // Credential Manager
            implementation(libs.credential.manager)
            implementation(libs.credential.manager.play.services)
            implementation(libs.google.id)

            // SceneView (3D / GLB rendering)
            implementation(libs.sceneview)

            // CameraX + MLKit
            implementation(libs.camera.x.core)
            implementation(libs.camera.x.camera2)
            implementation(libs.camera.x.lifecycle)
            implementation(libs.camera.x.view)
            implementation(libs.mlkit.barcode)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.vibra.bus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vibra.bus"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Backend por build: ./gradlew assembleRelease -PapiBaseUrl=https://api.tu-dominio.com/api/v1
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "https://bucaratransit.duckdns.org/api/v1"
        buildConfigField("String", "BASE_URL_ANDROID", "\"$apiBaseUrl\"")
        buildConfigField("String", "BASE_URL_IOS", "\"$apiBaseUrl\"")
        // Slug de la organización por defecto de este build; vacío = pedir el código al primer arranque.
        buildConfigField("String", "DEFAULT_ORG_SLUG", "\"${project.findProperty("defaultOrgSlug") ?: ""}\"")
        // Web client id de Google por build: -PgoogleServerClientId=... (fallback: el actual)
        val googleClientId = (project.findProperty("googleServerClientId") as String?)
            ?: "887389827022-oj7di7remsi2k1avdgqp8asf65rlu2h3.apps.googleusercontent.com"
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleClientId\"")
        
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        // Ya no hay meta-data de API key: el mapa usa MapLibre + OpenFreeMap (sin key).
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                properties.load(localPropertiesFile.inputStream())
            }
            storeFile = rootProject.file(properties.getProperty("KEYSTORE_FILE", "vibra-bus.jks"))
            storePassword = properties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = properties.getProperty("KEY_ALIAS", "")
            keyPassword = properties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        getByName("debug") {
            // Sin URLs fijas: el servidor se elige en runtime (pantalla de organización > Opciones
            // avanzadas) o con -PapiBaseUrl. Cleartext http solo se permite en debug.
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
