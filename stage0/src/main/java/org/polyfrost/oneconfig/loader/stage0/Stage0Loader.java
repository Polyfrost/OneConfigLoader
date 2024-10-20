package org.polyfrost.oneconfig.loader.stage0;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
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
	private static final String STAGE1_ARTIFACT_LOCAL = "oneconfig.loader.stage0.local";
	private static final String STAGE1_CLASS_NAME = "org.polyfrost.oneconfig.loader.stage1.Stage1Loader";

	private static final String STAGE1_RESOURCE_PATH = "oneconfig-loader/stage1.jar";

	private Class<?> stage1Class;
	private Object stage1Instance;

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );
    }

    @Override
	@SneakyThrows
    public void load() {
		Capabilities capabilities = this.getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();

        // Lookup stage1
        logger.info("Getting stage1 from cache");
        Path stage1Jar = lookupStage1();

        // Load in classloader as a library
        runtimeAccess.appendToClassPath("stage1", false, stage1Jar.toUri().toURL());

        // Delegate loading to stage1
        stage1Class = runtimeAccess.getClassLoader().loadClass(STAGE1_CLASS_NAME);
		Constructor<?> constructor = stage1Class.getDeclaredConstructor(Capabilities.class);

		try {
			constructor.setAccessible(true);
		} catch (Throwable ignored) {
		}

		stage1Instance = constructor.newInstance(capabilities);
        stage1Class.getDeclaredMethod("load").invoke(stage1Instance);
    }

	@Override
	@SneakyThrows
	public void postLoad() {
		stage1Class.getDeclaredMethod("postLoad").invoke(stage1Instance);
	}

    private Path lookupStage1() throws IOException {
		String localArtifactProp = System.getProperty(STAGE1_ARTIFACT_LOCAL);
		if (localArtifactProp != null) {
			Path localArtifact = Paths.get(localArtifactProp);
			if (Files.exists(localArtifact)) {
				return localArtifact;
			}
		}

		Path dataDir = XDG
				.provideCacheDir("OneConfig")
				.resolve("loader")
				.resolve("data");

		Path stage1UpdateFile = dataDir.resolve("stage1.update.jar");
        Path stage1File = dataDir.resolve("stage1.jar");

		// If the update file exists, replace the possibly existing stage1 file with it and delete the update file
		if (Files.exists(stage1UpdateFile)) {
			Files.deleteIfExists(stage1File);
			Files.move(stage1UpdateFile, stage1File);
		}

		// Lastly, if neither the stage1 file nor the update file exists, extract the stage1 resource
		Files.createDirectories(dataDir);

		URL latestUrl = null;
		int latestVersion = -1;

		if (Files.exists(stage1File)) {
			latestVersion = IOUtils.getJarVersion(stage1File.toUri().toURL());
		}

		Enumeration<URL> resources = Stage0Loader.class.getClassLoader().getResources(STAGE1_RESOURCE_PATH);
		if (!resources.hasMoreElements()) {
			throw new IOException("OneConfig loader could not find the next stage in it's setup process, cannot continue!");
		}

		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			int version = IOUtils.getJarVersion(url);
			if (version > latestVersion) {
				latestUrl = url;
				latestVersion = version;
			}
		}

		if (latestUrl != null) {
			try (InputStream inputStream = latestUrl.openStream()) {
				Files.deleteIfExists(stage1File);
				Files.copy(inputStream, stage1File);
			}
		}

        return stage1File;
    }

}
