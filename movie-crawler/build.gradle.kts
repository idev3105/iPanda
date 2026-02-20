plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.ipanda.crawler"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}


