pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.org/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.+"
}

rootProject.name = "loader"

// Direct submodules
val blacklist = arrayOf("gradle", "build-logic", "buildSrc")
rootDir.listFiles { it ->
    it.isDirectory && !it.name.startsWith(".") && it.name !in blacklist
}?.forEach { include(it.name) }