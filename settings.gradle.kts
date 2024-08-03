enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.org/releases") {
			name = "Polyfrost Releases"
		}
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "loader"

// Direct submodules
include("common")
include("stage0")
include("stage1")
