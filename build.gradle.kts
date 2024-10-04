@file:Suppress("VulnerableLibrariesLocal")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow) apply(false)
    alias(libs.plugins.lombok) apply(false)
}

group = "org.polyfrost.oneconfig"
version = "1.1.0-alpha.9"

allprojects {
    apply(plugin = "maven-publish")

	group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
        maven("https://maven.neoforged.net/releases")
    }

    configure<PublishingExtension> {
        repositories {
			mavenLocal()

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

    val compileOnly by configurations
    val include: Configuration by configurations.creating {
        compileOnly.extendsFrom(this)
    }

    val sourceSets = extensions.getByName<SourceSetContainer>("sourceSets")

    configure<JavaPluginExtension> {
        withSourcesJar()
//        withJavadocJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
		compileOnly(rootProject.libs.bundles.subproject)
    }

    if (project.name !== "common") {
        val main by sourceSets
        val mock by sourceSets.creating {
            compileClasspath += main.compileClasspath
        }
        main.compileClasspath += mock.output
    }

	configure<PublishingExtension> {
		publications {
			create<MavenPublication>("mavenJava") {
				artifactId = project.name
				group = project.group
				version = project.version.toString()

				artifact(tasks.named("shadowJar"))
				artifact(tasks.named("sourcesJar"))
			}
		}
	}

    tasks {

        named<Jar>("jar") {
            manifest.attributes += mapOf(
                "Specification-Title" to "OneConfig Loader",
                "Specification-Vendor" to "Polyfrost",
                "Specification-Version" to "2.0.0",
                "Implementation-Title" to "loader-${project.name}",
                "Implementation-Vendor" to project.group,
                "Implementation-Version" to project.version,
				"Implementation-License" to "GPL-3.0",
				"Implementation-Source" to "https://github.com/Polyfrost/OneConfigLoader",
				"Implementation-Website" to "https://polyfrost.org",
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
