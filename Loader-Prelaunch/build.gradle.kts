publishing {
    publications {
        create<MavenPublication>("loader-prelaunch") {
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

dependencies {
    modCompileOnly("org.apache.logging.log4j:log4j-api:2.8.1")
    modCompileOnly("org.apache.logging.log4j:log4j-core:2.8.1")
    modRuntimeOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
    modRuntimeOnly("org.apache.logging.log4j:log4j-core:2.0-beta9")
}