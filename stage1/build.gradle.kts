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
    include("cc.polyfrost:polyio:0.0.13")
    "legacyCompileOnly"("net.minecraft:launchwrapper:1.12")
}
