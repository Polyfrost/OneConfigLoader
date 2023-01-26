pluginManagement {
    repositories {
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "oneconfig-loader"

arrayOf("common", "stage0", "stage1").forEach { name ->
    include(name)

    // Add loader- prefix to all subprojects
    if (!name.startsWith("loader-")) {
        project(":$name").name = "loader-$name"
    }
}