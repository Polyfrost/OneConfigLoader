version = "1.1.0-alpha.47"

sourceSets {
    val main by this
    val legacy by creating {
        compileClasspath += main.compileClasspath + main.output
    }

    tasks.getByName(main.jarTaskName, Jar::class) {
        from(legacy.output)
    }
}

dependencies {
    implementation(projects.common)
    include("org.polyfrost:polyio:0.1.0") {
		isTransitive = false
	}
	include("me.xtrm:propy:0.0.5") {
		isTransitive = false
	}

    "legacyCompileOnly"("net.minecraft:launchwrapper:1.12")
}

tasks.jar {
	manifest {
		attributes(
			"Implementation-Version" to version,
		)
	}
}
