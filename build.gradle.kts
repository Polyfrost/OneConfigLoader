@file:Suppress("VulnerableLibrariesLocal")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow) apply(false)
    alias(libs.plugins.lombok) apply(false)
	alias(libs.plugins.reproducible.builds) apply(false)
}

group = "org.polyfrost.oneconfig"

allprojects {
    apply(plugin = "maven-publish")
	apply(plugin = "org.gradlex.reproducible-builds")

	group = rootProject.group

    configure<PublishingExtension> {
		afterEvaluate {
			publications.withType<MavenPublication> {
				version = project.version.toString()
			}
		}

        repositories {
			mavenLocal()

            mapOf(
                "polyfrostReleases" to "basic",
                "polyfrostSnapshots" to "basic",
                "polyfrostPrivate" to "private"
            ).forEach { (channel, authMethod) ->
                maven {
                    name = channel
                    setUrl("https://repo.polyfrost.org/${channel.removePrefix("polyfrost").lowercase()}")
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
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "io.freefair.lombok")

    val compileOnly by configurations
    val include by configurations.registering {
        compileOnly.extendsFrom(this)
    }

    val sourceSets = extensions.getByName<SourceSetContainer>("sourceSets")

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()

		targetCompatibility = JavaVersion.VERSION_1_8
		sourceCompatibility = JavaVersion.VERSION_1_8
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
			register<MavenPublication>("mavenJava") {
				artifactId = project.name
				group = project.group
				version = project.version.toString()

				artifact(tasks.named("shadowJar"))
				artifact(tasks.named("sourcesJar"))
			}
		}
	}

    tasks {
		withType<Javadoc> {
			options {
				this as StandardJavadocDocletOptions
				addStringOption("Xdoclint:none", "-quiet")
			}
		}

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
            configurations = listOf(include.get())
        }

        val assemble by this
        withType(ShadowJar::class) {
            assemble.dependsOn(this)
        }

		fun applyCompilerOptions(compileOptions: JavaCompile) {
			compileOptions.targetCompatibility = "1.8"
			compileOptions.sourceCompatibility = "1.8"
		}

		named<JavaCompile>("compileJava") {
			applyCompilerOptions(this)
		}

		withType<JavaCompile> {
			applyCompilerOptions(this)
		}
    }
}
