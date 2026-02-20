pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://jogamp.org/deployment/maven")
    }
}

rootProject.name = "iPanda"

include(":shared")
include(":movie-crawler")
include(":desktopApp")
include(":androidApp")
