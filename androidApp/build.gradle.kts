plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildConfig)
}

buildConfig {
    packageName("com.ipanda.android")
    
    fun getEnvOrProp(key: String): String {
        return System.getenv(key) 
            ?: project.findProperty(key)?.toString()
            ?: project.rootProject.file(".env").takeIf { it.exists() }?.readLines()
                ?.find { it.startsWith("$key=") }?.substringAfter("=")?.trim()
            ?: ""
    }

    val browserlessKey = getEnvOrProp("BROWSERLESS_API_KEY")
    val browserlessEndpoint = getEnvOrProp("BROWSERLESS_ENDPOINT")

    buildConfigField("String", "BROWSERLESS_API_KEY", "\"$browserlessKey\"")
    buildConfigField("String", "BROWSERLESS_ENDPOINT", "\"$browserlessEndpoint\"")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":movie-crawler"))
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayer.hls)
                implementation(libs.androidx.media3.ui)
                implementation(libs.coil.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.logging)
                implementation(libs.logback.android)
                implementation(libs.kotlinx.serialization.json)
                implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
                implementation(libs.compose.webview)
            }
        }
    }
}

android {
    namespace = "com.ipanda.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ipanda.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
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
