version = "1.0.0-alpha10"

loom {
    launchConfigs {
        "client" {
            arg("--tweakClass", "cc.polyfrost.oneconfigwrapper.OneConfigLoader")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("loader") {
            artifactId = project.name
            from(components["java"])
        }
    }
}

