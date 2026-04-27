import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

            // QR
            implementation(libs.qrcode.kotlin)

            // Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            // Firebase KMP
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.messaging)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.android)

            // Ktor engine
            implementation(libs.ktor.client.okhttp)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)

            // Maps
            implementation(libs.google.maps.compose)
            implementation(libs.play.services.maps)
            implementation(libs.play.services.location)

            // Firebase Android
            implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
            implementation(libs.firebase.messaging.android)

            // Credential Manager
            implementation(libs.credential.manager)
            implementation(libs.credential.manager.play.services)
            implementation(libs.google.id)

            // CameraX + MLKit
            implementation(libs.camera.x.core)
            implementation(libs.camera.x.camera2)
            implementation(libs.camera.x.lifecycle)
            implementation(libs.camera.x.view)
            implementation(libs.mlkit.barcode)
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

        buildConfigField("String", "BASE_URL_ANDROID", "\"http://192.168.1.23:8000/api/v1\"")
        buildConfigField("String", "BASE_URL_IOS", "\"http://localhost:8000/api/v1\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"887389827022-oj7di7remsi2k1avdgqp8asf65rlu2h3.apps.googleusercontent.com\"")

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

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
