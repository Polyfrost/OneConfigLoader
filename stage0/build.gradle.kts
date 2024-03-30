import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val platforms = mapOf(
    "launchwrapper" to arrayOf(
        "net.minecraft:launchwrapper:1.12"
    ),
    "modlauncher" to arrayOf(
        "cpw.mods:modlauncher:8.0.9",
//                "net.minecraftforge:forgespi:3.2.3",
//                "net.neoforged.fancymodloader:spi:+"
    ),
    "prelaunch" to arrayOf(
        "net.fabricmc:fabric-loader:0.11.6"
    )
)

val include by configurations

val platformSets = sourceSets.run {
    fun createConfigured(name: String, block: SourceSet.() -> Unit = {}): SourceSet =
        create(name) {
            compileClasspath += main.get().compileClasspath + main.get().output + mock.get().output
            block()
        }

    platforms.keys.map(::createConfigured)
}

dependencies {
    platforms.forEach { (name, deps) ->
        deps.forEach { "${name}CompileOnly"(it) }
    }
}

tasks {
    named<Jar>(sourceSets.main.get().sourcesJarTaskName) {
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
            from(sourceSets.main.get().output)

            manifest.inheritFrom(jar.get().manifest)
            configurations = listOf(include)
        }
    }

    named<ShadowJar>("shadowJar") {
        platformSets.forEach {
            from(it.output)
        }
    }
}