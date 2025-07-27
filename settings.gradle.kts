pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    plugins {
        id("com.android.application") version "8.2.2"
        kotlin("android") version "2.0.20"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // Must include this for AndroidX libraries
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MyApplication"
include(":app")