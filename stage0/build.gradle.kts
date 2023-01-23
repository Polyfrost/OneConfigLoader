val isForge = loom.isForge
val isFabric = !isForge

loom {
    launchConfigs {
        "client" {

        }
    }
}

sourceSets {
    val main by this

    fun createConfigured(name: String, block: SourceSet.() -> Unit = {}) {
        create(name) {
            java.srcDir("src/$name/java")
            resources.srcDir("src/$name/resources")
            compileClasspath += main.compileClasspath + main.output
            runtimeClasspath += main.runtimeClasspath + main.output
            block()
        }
    }

    createConfigured("launchwrapper")
    createConfigured("modlauncher")
    createConfigured("prelaunch")
}

dependencies {
    implementation("cpw.mods:modlauncher:8.0.9")
}

publishing {
    publications {
        create<MavenPublication>("wrapper") {
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

