package org.polyfrost.oneconfig.loader.stage0;

import java.io.FileOutputStream;
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

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.ui.LoaderFrame;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
import org.polyfrost.oneconfig.loader.utils.XDG;

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
    private static final String DEFAULT_MAVEN_BASE_URL = "https://repo.polyfrost.org/releases/";

    private final Properties stage0Properties;

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );

		try (InputStream inputStream = this.getClass().getResourceAsStream("/oneconfig-loader/metadata/stage0.properties")) {
			this.stage0Properties = new Properties();
			this.stage0Properties.load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    @SneakyThrows
    @Override
    public void load() {
		LoaderFrame loaderFrame = new LoaderFrame();

		Capabilities capabilities = this.getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();
//		Capabilities.GameMetadata gameMetadata = capabilities.getGameMetadata();

		String stage1ClassName = fetchStage1ClassName();
		if (stage1ClassName == null) {
			throw new IllegalStateException("Stage1 class name not found");
		}

        // fetch settings
        logger.info("Loading OneConfig settings");
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

		// Display the loader frame now that all of our state is loaded
		loaderFrame.display();

        // Fetch stage1 version info
        logger.info("Fetching stage1 version info");
		String finalMavenUrl = mavenUrl;
		Supplier<String> stage1JarUrl = () -> finalMavenUrl + "org/polyfrost/oneconfig/stage1/" + stage1Version + "/stage1-" + stage1Version + "-all.jar";

        // Lookup stage1 in cache, handle downloading
        logger.info("Getting stage1 from cache");
        Path stage1Jar = lookupStage1CacheOrDownload(loaderFrame, stage1Version, stage1JarUrl);

        // Load in classloader as a library
        logger.info("Loading stage1 as a library");
        runtimeAccess.appendToClassPath(false, stage1Jar.toUri().toURL());

        // Delegate loading to stage1
        logger.info("GO");
        Class<?> stage1Class = runtimeAccess.getClassLoader().loadClass(stage1ClassName);
		Constructor<?> constructor;

		try {
			constructor = stage1Class.getDeclaredConstructor(LoaderFrame.class, Capabilities.class);
		} catch (Throwable ignored) {
			// Print out all constructors and exit
			for (Constructor<?> c : stage1Class.getDeclaredConstructors()) {
				logger.error("Constructor: {}", c);
			}

			throw new IllegalStateException("Stage1 constructor not found");
		}

		try {
			constructor.setAccessible(true);
		} catch (Throwable ignored) {
		}

        Object stage1Instance = constructor.newInstance(loaderFrame, capabilities);
        stage1Class.getDeclaredMethod("load").invoke(stage1Instance);
    }

	private String fetchStage1ClassName() {
		String value = System.getProperty("oneconfig.stage1.class");
		if (value != null) {
			return value;
		}

		value = this.stage0Properties.getProperty("oneconfig-stage1-class");
		if (value != null) {
			return value;
		}

		throw new IllegalStateException("Stage1 class name not found");
	}

    private String fetchStage1Version() {
		String value = System.getProperty("oneconfig.stage1.version");
		if (value != null) {
			return value;
		}

		value = this.stage0Properties.getProperty("oneconfig-stage1-version");
		if (value != null) {
			return value;
		}

		throw new IllegalStateException("Stage1 version not found");
    }

	private String fetchMavenUrl() {
		String value = System.getProperty("oneconfig.maven.uri");
		if (value != null) {
			return value;
		}

		value = this.stage0Properties.getProperty("oneconfig-maven-uri");
		if (value != null) {
			return value;
		}

		return DEFAULT_MAVEN_BASE_URL;
	}

    private Path lookupStage1CacheOrDownload(LoaderFrame loaderFrame, String version, Supplier<String> uriSupplier) throws IOException {
        Path cache = XDG
                .provideCacheDir("OneConfig")
                .resolve("loader")
                .resolve("stage0")
                .resolve("OneConfigLoader-Stage0-" + version + ".jar");

		if (!Files.exists(cache)) {
			Files.createDirectories(cache.getParent());

			URI uri = URI.create(uriSupplier.get());

			try (FileOutputStream outputStream = new FileOutputStream(cache.toFile())) {
				InputStream inputStream;

				// If it's HTTP, use the RequestHelper
				if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
					loaderFrame.updateMessage("Downloading OneConfig Loader (Stage1)...");
					inputStream = this.getRequestHelper().establishConnection(new URL(uri.toString())).getInputStream();
				} else {
					// Otherwise, just use the Path
					loaderFrame.updateMessage("Copying OneConfig Loader (Stage1)...");
					inputStream = uri.toURL().openStream();
				}

				byte[] buffer = new byte[4096];
				int totalBytes = inputStream.available();
				int read;
				int totalRead = 0;
				while ((read = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, read);
					totalRead += read;
					loaderFrame.updateProgress((float) (totalRead / totalBytes));
				}

				inputStream.close();
			}
		}

        return cache;
    }

}
