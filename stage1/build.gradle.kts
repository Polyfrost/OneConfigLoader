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
    implementation(project(":common"))
    "legacyCompileOnly"("net.minecraft:launchwrapper:1.12")
}