import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("gg.essential.loom") version "0.10.0.+" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

allprojects {
    apply(plugin = "maven-publish")
    group = "cc.polyfrost"
    repositories {
        mavenCentral()
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
    apply(plugin = "gg.essential.loom")
    apply(plugin = "com.github.johnrengelman.shadow")

    val include: Configuration by configurations.creating {
        configurations.named("implementation").get().extendsFrom(this)
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }
    configure<LoomGradleExtensionAPI> {
        forge {
            pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        }
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:1.8.9")
        "mappings"("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
        "forge"("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    tasks.withType(Jar::class) {
        archiveBaseName.set(project.name)
        archiveClassifier.set("obf")
    }

    val shadowJar by tasks.named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        configurations = listOf(include)
    }

    val remapJar by tasks.named<RemapJarTask>("remapJar") {
        from(shadowJar)
        input.set(shadowJar.archiveFile)
    }
    tasks.named("assemble") {
        dependsOn(remapJar)
    }
}

