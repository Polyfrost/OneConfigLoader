pluginManagement {
    repositories {
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "OneConfigStages"

include("Common")
project(":Common").name = "oneconfig-common-loader"

include("Wrapper")
project(":Wrapper").name = "oneconfig-wrapper-launchwrapper"

include("Loader")
project(":Loader").name = "oneconfig-loader-launchwrapper"
