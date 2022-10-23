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
            group = project.group
            version = project.version.toString()
            artifacts {
                artifact(tasks["shadowJar"])
                artifact(tasks["sourcesJar"]) {
                    classifier = "sources"
                }
            }
        }
    }
}

