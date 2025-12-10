version = "1.1.0-alpha.54" // When you bump this, you should also bump stage0, cause stage0 includes a copy of stage1 too

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

	implementation("net.minecraft:launchwrapper:1.12") {
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
