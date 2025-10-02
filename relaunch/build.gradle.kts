version = "1.1.0-alpha.47"

dependencies {
    implementation(projects.common)
    include("org.polyfrost:polyio:0.1.0")

    compileOnly("net.minecraft:launchwrapper:1.12")
	compileOnly("com.google.guava:guava:17.0")
}
