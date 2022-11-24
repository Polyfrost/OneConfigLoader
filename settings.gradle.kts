pluginManagement {
    repositories {
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "OneConfigStages"

include("Common")
project(":Common").name = "oneconfig-common-loader"

include("Wrapper-Launchwrapper")
project(":Wrapper-Launchwrapper").name = "oneconfig-wrapper-launchwrapper"

include("Loader-Launchwrapper")
project(":Loader-Launchwrapper").name = "oneconfig-loader-launchwrapper"

include("Wrapper-Prelaunch")
project(":Wrapper-Prelaunch").name = "oneconfig-wrapper-prelaunch"

include("Loader-Prelaunch")
project(":Loader-Prelaunch").name = "oneconfig-loader-prelaunch"