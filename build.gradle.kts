@file:Suppress("DEPRECATION")

buildscript {
    repositories {
        google() // Required for AGP
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}