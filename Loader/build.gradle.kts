loom {
    launchConfigs {
        "client" {
            arg("--tweakClass", "cc.polyfrost.oneconfig.loader.OneConfigLoader")
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
                artifact(tasks["remapJar"])
                artifact(tasks["sourcesJar"]) {
                    classifier = "sources"
                }
            }
        }
    }
}

