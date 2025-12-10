import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = "1.1.0-alpha.54"

val include by configurations

dependencies {
    include(projects.common)

	@Suppress("RedundantSuppression", "VulnerableLibrariesLocal")
	compileOnly("net.minecraft:launchwrapper:1.12")
	@Suppress("RedundantSuppression", "VulnerableLibrariesLocal")
	compileOnly("com.google.guava:guava:17.0")
	compileOnly("org.apache.commons:commons-lang3:3.3.2")
}

tasks {
    named<ShadowJar>("shadowJar") {
        enabled = true
		from(jar)
		configurations = listOf(include)
	}

	jar {
		from(project(":stage1").tasks.named<Jar>("shadowJar").map { it.outputs.files }) {
			rename { "oneconfig-loader/stage1.jar" }
		}
	}
}
