pluginManagement {
    repositories {
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "oneconfig-loader"

include("common")
include("stage1")
include("stage0")
