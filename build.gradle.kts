import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("io.freefair.lombok") version "6.1.0" apply false
}

group = "cc.polyfrost"
version = "1.1.0-alpha.1"

allprojects {
    apply(plugin = "maven-publish")

    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.cc/releases")
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "releases"
                setUrl("https://repo.polyfrost.cc/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            maven {
                name = "snapshots"
                setUrl("https://repo.polyfrost.cc/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            maven {
                name = "private"
                setUrl("https://repo.polyfrost.cc/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("private")
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

    group = "${rootProject.group}.oneconfig"

    val isCommon = (project.name.contains("common"))

    val compileOnly by configurations
    val include: Configuration by configurations.creating {
        compileOnly.extendsFrom(this)
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:20.1.0")
    }

    if (!isCommon) {
        val platforms = mapOf(
            "launchwrapper" to "net.minecraft:launchwrapper:1.12",
            "modlauncher" to "cpw.mods:modlauncher:8.0.9",
            "prelaunch" to "net.fabricmc:fabric-loader:0.11.6"
        )

        val sourceSets = extensions.getByName<SourceSetContainer>("sourceSets")

        sourceSets {
            val main by this

            fun createConfigured(name: String, block: SourceSet.() -> Unit = {}) {
                create(name) {
                    compileClasspath += main.compileClasspath + main.output
                    runtimeClasspath += main.runtimeClasspath + main.output
                    block()
                }
            }

            platforms.keys.forEach(::createConfigured)
        }

        dependencies {
            platforms.forEach { (name, dep) ->
                "${name}CompileOnly"(dep)
            }

            include(project(":loader-common")) {
                isTransitive = false
            }
        }

        tasks {
            withType(JavaCompile::class) {
                options.encoding = "UTF-8"
            }

            val shadowJar by named<ShadowJar>("shadowJar") {
                archiveBaseName.set(project.name)
                archiveClassifier.set("")
                from(sourceSets["main"].output)
                platforms.keys.forEach {
                    from(sourceSets[it].output)
                }
                configurations = listOf(include)
            }

            val build by this
            build.dependsOn(shadowJar)
        }

//        publishing {
//            publications {
//                create<MavenPublication>("wrapper") {
//                    artifactId = project.name
//                    group = project.group
//                    version = project.version.toString()
//
//                    artifacts {
//                        artifact(tasks["shadowJar"])
//                        artifact(tasks["sourcesJar"]) {
//                            classifier = "sources"
//                        }
//                    }
//                }
//            }
    }
}


