pluginManagement {
    repositories {
        maven("https://repo.polyfrost.org/releases")
    }
}

rootProject.name = "OneConfigStages"

include("Common")
project(":Common").name = "oneconfig-common"

include("Common-Loader")
project(":Common-Loader").name = "oneconfig-common-loader"

include("Wrapper")
project(":Wrapper").name = "oneconfig-wrapper-launchwrapper"

include("Loader")
project(":Loader").name = "oneconfig-loader-launchwrapper"
