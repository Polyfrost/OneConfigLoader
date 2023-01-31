pluginManagement {
    repositories {
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "loader"

arrayOf("common", "stage0", "stage1").forEach { name ->
    include(name)
}