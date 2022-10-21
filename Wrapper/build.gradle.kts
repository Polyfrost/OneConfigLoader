version = "1.0.0-alpha8"

loom {
    launchConfigs {
        "client" {
            arg("--tweakClass", "cc.polyfrost.oneconfigwrapper.OneConfigWrapper")
        }
    }
}

tasks.withType(Jar::class) {
    manifest.attributes.run {
        this["ForceLoadAsMod"] = "true"
        this["TweakOrder"] = "0"
        this["TweakClass"] = "cc.woverflow.onecore.tweaker.OneCoreTweaker"
        this["ModSide"] = "CLIENT"
    }
}

publishing {
    publications {
        create<MavenPublication>("wrapper") {
            artifactId = project.name
            from(components["java"])
        }
    }
}

