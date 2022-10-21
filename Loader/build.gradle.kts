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
        create<MavenPublication>("wrapper") {
            artifactId = name
            from(components["java"])
        }
    }
}

