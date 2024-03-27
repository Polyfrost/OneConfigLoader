@file:Suppress("VulnerableLibrariesLocal")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("io.freefair.lombok") version "6.1.0" apply false
}

group = "cc.polyfrost.oneconfig"
version = "1.1.0-alpha.2"

allprojects {
    apply(plugin = "maven-publish")

    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.cc/releases")
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
                    setUrl("https://repo.polyfrost.cc/$channel")
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
    val isStage0 = (project.name == "stage0")

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
        val platforms = mapOf(
            "launchwrapper" to "net.minecraft:launchwrapper:1.12",
            "modlauncher" to "cpw.mods:modlauncher:8.0.9",
            "prelaunch" to "net.fabricmc:fabric-loader:0.11.6"
        )

        val platformSets = mutableListOf<SourceSet>()
        val main by sourceSets
        if (isStage0) {
            platformSets += sourceSets.run {
                fun createConfigured(name: String, block: SourceSet.() -> Unit = {}): SourceSet =
                    create(name) {
                        compileClasspath += main.compileClasspath + main.output
                        runtimeClasspath += main.runtimeClasspath + main.output
                        block()
                    }

                platforms.keys.map(::createConfigured)
            }

            dependencies {
                platforms.forEach { (name, dep) ->
                    "${name}CompileOnly"(dep)
                }

                include(project(":common")) {
                    isTransitive = false
                }
            }
        }

        tasks {
            val jar = named<Jar>("jar") {
                manifest.attributes += mapOf(
                    "Specification-Title" to "OneConfig Loader",
                    "Specification-Vendor" to "Polyfrost",
                    "Specification-Version" to "2.0.0",
                    "Implementation-Title" to "loader-${project.name}",
                    "Implementation-Vendor" to project.group,
                    "Implementation-Version" to project.version
                )
            }

            withType(JavaCompile::class) {
                options.encoding = "UTF-8"
            }

            if (isStage0) {
                named<Jar>(main.sourcesJarTaskName) {
                    platformSets.forEach { set ->
                        from(set.allSource)
                    }
                }

                platformSets.forEach { set ->
                    create(set.jarTaskName, ShadowJar::class) {
                        archiveBaseName.set(project.name)
                        archiveClassifier.set(set.name)
                        group = "build"

                        from(set.output)
                        from(main.output)

                        manifest.inheritFrom(jar.get().manifest)
                        configurations = listOf(include)
                    }
                }
            }

            named<ShadowJar>("shadowJar") {
                archiveBaseName.set(project.name)
                archiveClassifier.set("")

                from(main.output)
                platformSets.forEach {
                    from(it.output)
                }

                manifest.inheritFrom(jar.get().manifest)
                configurations = listOf(include)
            }
        }
    } else {
        tasks {
            named<ShadowJar>("shadowJar") {
                archiveBaseName.set(project.name)
                archiveClassifier.set("")
                configurations = listOf(include)
                from(sourceSets["main"].output)
            }
        }
    }

    tasks {
        named("jar") {
            enabled = false
        }

        val build by this
        withType(ShadowJar::class) {
            build.finalizedBy(this)
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
