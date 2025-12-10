@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.polyfrost.org/releases") {
			name = "Polyfrost Releases"
		}
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven("https://repo.polyfrost.org/releases")
		maven("https://repo.polyfrost.org/snapshots")
		maven("https://maven.neoforged.net/releases")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "loader"

// Direct submodules
include("common")
include("stage0")
include("relaunch")
include("stage1")
include("loader-assets")
