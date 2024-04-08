@file:Suppress("VulnerableLibrariesLocal")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.freefair.lombok") version "8.6" apply false
}

group = "cc.polyfrost.oneconfig"
version = "1.1.0-alpha.3"

allprojects {
    apply(plugin = "maven-publish")

    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
        maven("https://maven.neoforged.net/releases")
    }

    configure<PublishingExtension> {
        repositories {
            mapOf(
                "releases" to "basic",
                "snapshots" to "basic",
                "private" to "private"
            ).forEach { (channel, authMethod) ->
                maven {
                    name = channel
                    setUrl("https://repo.polyfrost.org/$channel")
                    credentials(PasswordCredentials::class)
                    authentication {
                        create<BasicAuthentication>(authMethod)
                    }
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "io.freefair.lombok")

    val isCommon = (project.name == "common")

    val compileOnly by configurations
    val include: Configuration by configurations.creating {
        compileOnly.extendsFrom(this)
    }

    val sourceSets = extensions.getByName<SourceSetContainer>("sourceSets")

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:20.1.0")

        // real MC uses older versions of log4j but that doesn't really matter in this case
        compileOnly("org.apache.logging.log4j:log4j-api:2.19.0")
        compileOnly("org.apache.logging.log4j:log4j-core:2.19.0")

        // same for Gson, let's use the oldest version to ensure compatibility
        compileOnly("com.google.code.gson:gson:2.2.4")
    }

    if (!isCommon) {
        val main by sourceSets
        val mock by sourceSets.creating {
            compileClasspath += main.compileClasspath
        }
        main.compileClasspath += mock.output
    }

    tasks {
        named<Jar>("jar") {
            manifest.attributes += mapOf(
                "Specification-Title" to "OneConfig Loader",
                "Specification-Vendor" to "Polyfrost",
                "Specification-Version" to "2.0.0",
                "Implementation-Title" to "loader-${project.name}",
                "Implementation-Vendor" to project.group,
                "Implementation-Version" to project.version
            )
        }

        named<ShadowJar>("shadowJar") {
            configurations = listOf(include)
        }

        val build by this
        withType(ShadowJar::class) {
            build.finalizedBy(this)
        }

        withType(JavaCompile::class) {
            options.encoding = "UTF-8"
        }
    }
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>(project.name) {
            artifactId = this.name
            group = project.group
            version = project.version.toString()

            artifacts {
                subprojects.forEach { proj ->
                    proj.tasks.withType(ShadowJar::class) {
                        artifact(this) {
                            classifier = project.name +
                                    if (classifier.isNullOrBlank()) ""
                                    else "-$classifier"
                        }
                    }
                    proj.tasks.withType(Jar::class) {
                        if (this is ShadowJar) return@withType
                        artifact(this) {
                            classifier = project.name +
                                    if (classifier.isNullOrBlank()) ""
                                    else "-$classifier"
                        }
                    }
                }
            }
        }
    }
}
