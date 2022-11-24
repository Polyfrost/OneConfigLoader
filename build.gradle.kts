import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("cc.polyfrost.loom") version "0.10.0.+" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

allprojects {
    apply(plugin = "maven-publish")
    group = "cc.polyfrost"
    version = "1.0.0-alpha9"
    repositories {
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
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "com.github.johnrengelman.shadow")
    val common = project.name == "oneconfig-common-loader"
    if (!common) {
        apply(plugin = "cc.polyfrost.loom")
    }

    val shade: Configuration by configurations.creating {
        configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
        configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }

    if (!common) {
        configure<LoomGradleExtensionAPI> {
            if (project.name.contains("prelaunch")) {
                intermediaryUrl.set("https://maven.legacyfabric.net/net/legacyfabric/intermediary/%1\$s/intermediary-%1\$s-v2.jar")
            } else {
                forge {
                    pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
                }
            }
        }

        dependencies {
            "minecraft"("com.mojang:minecraft:1.8.9")
            if (project.name.contains("launchwrapper")) {
                "mappings"("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
                "forge"("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
            } else {
                "mappings"("net.legacyfabric:yarn:1.8.9+build.385:v2")
                "modImplementation"("net.fabricmc:fabric-loader:0.14.10")
            }
            shade(project(":oneconfig-common-loader"))
        }
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    if (!common) {
        tasks.withType(Jar::class) {
            archiveBaseName.set(project.name)
            archiveClassifier.set("obf")
        }

        val shadowJar by tasks.named<ShadowJar>("shadowJar") {
            archiveBaseName.set(project.name)
            archiveClassifier.set("")
            configurations = listOf(shade)
        }

        val remapJar by tasks.named<RemapJarTask>("remapJar") {
            from(shadowJar)
            input.set(shadowJar.archiveFile)
        }
        tasks.named("assemble") {
            dependsOn(remapJar)
        }
        tasks.withType<ProcessResources> {
            filesMatching(listOf("fabric.mod.json")) {
                expand(
                    mapOf(
                        "version" to project.version
                    )
                )
            }
        }
    }
}


