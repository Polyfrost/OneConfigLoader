import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

data class Platform(
    val name: String,
    val dependencies: Set<String>,
    val j9Platform: J9Platform? = null,
    val extraAttributes: Map<String, String> = emptyMap(),

    var sourceSet: SourceSet? = null,
)
data class J9Platform(
    val dependencies: Set<String>,
    var sourceSet: SourceSet? = null
)

val platforms = setOf(
    Platform(
        "launchwrapper",
		// also depend on fabric-loader since it *can* be used with launchwrapper
        setOf("net.minecraft:launchwrapper:1.12", "net.fabricmc:fabric-loader:0.13.3"),
        extraAttributes = mapOf(
            "TweakClass" to "org.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker",
        )
    ),
    Platform(
        "modlauncher",
        setOf("cpw.mods:modlauncher:8.0.9"),
        J9Platform(setOf("cpw.mods:modlauncher:9.1.6"))
    ),
    Platform(
        "fabriclike",
        setOf("net.fabricmc:fabric-loader:0.13.3", "org.quiltmc:quilt-loader:0.24.0")
    )
)

val include by configurations

sourceSets {
    fun createConfigured(name: String, block: SourceSet.() -> Unit = {}): SourceSet =
        create(name) {
            compileClasspath += main.get().compileClasspath
            compileClasspath += main.get().output
            compileClasspath += mock.get().output
            block()
        }

    platforms.forEach { platform ->
        val set = createConfigured(platform.name)
        platform.j9Platform?.let { j9 ->
            j9.sourceSet = createConfigured("${platform.name}9") {
                compileClasspath += set.output

                java {
                    srcDirs("src/${platform.name}/java9")
                }
            }.also { j9set ->
                tasks.getByName<JavaCompile>(j9set.compileJavaTaskName) {
                    javaCompiler.set(javaToolchains.compilerFor {
                        languageVersion.set(JavaLanguageVersion.of(16))
                    })
                }
            }
        }
        platform.sourceSet = set
    }
}

dependencies {
    include(projects.common)

    platforms.forEach { plat ->
        plat.dependencies.forEach { "${plat.name}CompileOnly"(it) }
        plat.j9Platform?.let { j9 ->
            j9.dependencies.forEach { "${plat.name}9CompileOnly"(it) }
        }
    }
}

tasks {
    named<Jar>(sourceSets.main.get().sourcesJarTaskName) {
		manifest.attributes("OneConfig-Stage1-Class" to "org.polyfrost.oneconfig.loader.stage1.Stage1Loader")

        platforms.forEach { platform ->
            from(platform.sourceSet!!.allSource)
            platform.j9Platform?.let { j9 ->
                into("META-INF/versions/9") {
                    from(j9.sourceSet!!.allSource)
                }
            }
        }
    }

    platforms.forEach { platform ->
        val set = platform.sourceSet!!
        create(set.jarTaskName, ShadowJar::class) {
            archiveBaseName.set(project.name)
            archiveClassifier.set(set.name)
            group = "build"

            from(set.output)
            platform.j9Platform?.let { j9 ->
                into("META-INF/versions/9") {
                    from(j9.sourceSet!!.output)
                }
                manifest.attributes["Multi-Release"] = true
            }
            manifest.attributes += platform.extraAttributes
            from(sourceSets.main.get().output)

            manifest.inheritFrom(jar.get().manifest)
            configurations = listOf(include)
        }
    }

    named<ShadowJar>("shadowJar") {
        enabled = true
    }
}

configure<PublishingExtension> {
	publications {
		named("mavenJava", MavenPublication::class.java) {
			platforms.forEach { platform ->
				artifact(tasks.named<ShadowJar>(platform.sourceSet!!.jarTaskName)) {
					classifier = platform.sourceSet!!.name
				}
			}
		}
	}
}
