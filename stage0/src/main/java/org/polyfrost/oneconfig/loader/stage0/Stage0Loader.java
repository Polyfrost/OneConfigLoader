package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
import org.polyfrost.oneconfig.loader.utils.XDG;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * The first stage of the OneConfig Loader.
 * <p>
 * This class is loaded via the platform-dependant entrypoint (e.g. the LaunchWrapper tweaker),
 * and is responsible for lookup and loading of the stage1 loader.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class Stage0Loader extends LoaderBase {
    private static final String DEFAULT_MAVEN_BASE_URL = "https://repo.polyfrost.org/";

    private final Properties stage0Properties;

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );

		try (InputStream inputStream = this.getClass().getResourceAsStream("/polyfrost/stage0.properties")) {
			this.stage0Properties = new Properties();
			this.stage0Properties.load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    @SneakyThrows
    @Override
    public void load() {
        JOptionPane.showMessageDialog(null, "Loading hook");

		String stage1ClassName = fetchStage1ClassName();
		if (stage1ClassName == null) {
			throw new IllegalStateException("Stage1 class name not found");
		}

        // fetch settings
        this.getLogger().info("Loading OneConfig settings");
        String stage1Version = fetchStage1Version();
		if (stage1Version == null) {
			throw new IllegalStateException("Stage1 version not found");
		}

		String mavenUrl = fetchMavenUrl();
		if (mavenUrl == null) {
			throw new IllegalStateException("Maven URL not found");
		}

		mavenUrl = mavenUrl.endsWith("/") ? mavenUrl : mavenUrl + "/";
		mavenUrl = mavenUrl.replace('\\', '/');

        // Fetch stage1 version info
        this.getLogger().info("Fetching stage1 version info");
		String finalMavenUrl = mavenUrl;
		Supplier<String> stage1JarUrl = () -> finalMavenUrl + "org/polyfrost/oneconfig/stage1/" + stage1Version + "/stage1-" + stage1Version + "-all.jar";

        // Lookup stage1 in cache, handle downloading
        this.getLogger().info("Getting stage1 from cache");
        Path stage1Jar = lookupStage1CacheOrDownload(stage1Version, stage1JarUrl);

        // Load in classloader as a library
        this.getLogger().info("Loading stage1 as a library");
        this.getCapabilities().appendToClassPath(false, stage1Jar.toUri().toURL());

        // Delegate loading to stage1
        this.getLogger().info("GO");
        Class<?> stage1Class = this.getCapabilities()
                .getClassLoader()
                .loadClass(stage1ClassName);
		System.out.println("Loaded class " + stage1Class.getName());
		Constructor<?>[] constructors = stage1Class.getDeclaredConstructors();
		System.out.println("Constructors:");
		for (Constructor<?> constructor : constructors) {
			System.out.println("- Constructor: " + constructor);
		}

        Object stage1Instance = stage1Class
                .getDeclaredConstructor(Capabilities.class)
                .newInstance(this.getCapabilities());
		System.out.println("Created instance of " + stage1Instance.getClass().getName());
        stage1Class.getDeclaredMethod("load").invoke(stage1Instance);
    }

	private String fetchStage1ClassName() {
		String value = this.stage0Properties.getProperty("oneconfig-stage1-class");
		return value != null ? value : System.getProperty("oneconfig.stage1.class");
	}

    private String fetchStage1Version() {
        String value = this.stage0Properties.getProperty("oneconfig-stage1-version");
        return value != null ? value : System.getProperty("oneconfig.stage1.version");
    }

	private String fetchMavenUrl() {
		String value = this.stage0Properties.getProperty("oneconfig-maven-uri");
		if (value != null) {
			return value;
		}

		String mavenUrl = System.getProperty("oneconfig.maven.uri");
		if (mavenUrl != null) {
			return mavenUrl;
		}

		return DEFAULT_MAVEN_BASE_URL;
	}

    private Path lookupStage1CacheOrDownload(String version, Supplier<String> uriSupplier) throws IOException {
        Path cache = XDG
                .provideCacheDir("OneConfig")
                .resolve("loader")
                .resolve("stage0")
                .resolve("OneConfigLoader-Stage0-" + version + ".jar");

		if (!Files.exists(cache)) {
			Files.createDirectories(cache.getParent());

			URI uri = URI.create(uriSupplier.get());

			// If it's HTTP, use the RequestHelper
			if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
				try (InputStream inputStream = this.getRequestHelper().establishConnection(new URL(uri.toString())).getInputStream()) {
					Files.copy(inputStream, cache, StandardCopyOption.REPLACE_EXISTING);
				}

				return cache;
			}

			// Otherwise, just use the Path
			try (InputStream inputStream = uri.toURL().openStream()) {
				Files.copy(inputStream, cache, StandardCopyOption.REPLACE_EXISTING);
			}
		}

        return cache;
    }

}
