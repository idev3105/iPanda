plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildConfig)
}

buildConfig {
    packageName("com.ipanda.desktop")
    
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
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(project(":shared"))
                implementation(project(":movie-crawler"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.vlcj)
                implementation(libs.logback.classic)
                implementation(libs.kotlin.logging)
                implementation(libs.compose.webview)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ipanda.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "iPanda"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register<JavaExec>("runTestPlayer") {
    group = "verification"
    description = "Runs the standalone TestPlayer"
    mainClass.set("com.ipanda.desktop.TestPlayerKt")
    classpath = kotlin.targets.getByName("desktop").compilations.getByName("main").output.allOutputs +
            kotlin.targets.getByName("desktop").compilations.getByName("main").runtimeDependencyFiles
}
