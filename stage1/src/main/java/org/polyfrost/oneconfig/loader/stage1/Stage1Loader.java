package org.polyfrost.oneconfig.loader.stage1;

import static me.xtrm.propy.MutableProperty.mutablePropertyOf;
import static me.xtrm.propy.Property.propertyOf;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.xtrm.propy.MutableProperty;
import me.xtrm.propy.Property;

import org.jetbrains.annotations.NotNull;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.relaunch.DetectionSupplier;
import org.polyfrost.oneconfig.loader.relaunch.Relaunch;
import org.polyfrost.oneconfig.loader.stage1.backend.BackendArtifact;
import org.polyfrost.oneconfig.loader.stage1.ui.LoaderFrame;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
import org.polyfrost.oneconfig.loader.utils.XDG;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
public class Stage1Loader extends LoaderBase {
	//FIXME: Detect this from the target artifacts
	private static final String ONECONFIG_MAIN_CLASS = "org.polyfrost.oneconfig.internal.bootstrap.Bootstrap";

	private static final MutableProperty<Boolean> RELAUNCH_STATE =
			mutablePropertyOf("oneconfig.loader.relaunch.state", false);

	private static final Property<Boolean> ARTIFACT_SNAPSHOTS =
			propertyOf("oneconfig.loader.stage1.snapshots", false);
	private static final Property<Boolean> STAGE1_ARTIFACT_SNAPSHOTS =
			propertyOf("oneconfig.loader.stage1.update.snapshots", false);
	private static final Property<Boolean> RELAUNCH_ARTIFACT_SNAPSHOTS =
			propertyOf("oneconfig.loader.stage1.relaunch.snapshots", false);
	private static final Property<Boolean> ONECONFIG_ARTIFACT_SNAPSHOTS =
			propertyOf("oneconfig.loader.stage1.oneconfig.snapshots", false);
	private static final Property<Boolean> IGNORE_UPDATES =
			propertyOf("oneconfig.loader.stage1.ignoreupdates", false);

	private static boolean isUpdateChecked = false;
	private static boolean isRelaunchDownloaded = false;
	private static boolean isOneConfigDownloaded = false;

	private Class<?> oneconfigMainClass;
	private Object oneconfigMainInstance;

	private LoaderFrame loaderFrame = null;

	private boolean delegateToDelayedTweaker = false;

	private static Capabilities lastDetectedCapabilities = null;

	public Stage1Loader(Capabilities capabilities) {
		super(
				"stage1",
				IOUtils.provideImplementationVersion(Stage1Loader.class, UNKNOWN_VERSION),
				capabilities
		);
		lastDetectedCapabilities = capabilities;
	}

	/**
	 * Don't ask.
	 */
	Stage1Loader() {
		this(lastDetectedCapabilities);
	}

	@Override
	public void load() {
		if (DelayedStage0Tweaker.isRequired()) {
			DelayedStage0Tweaker.prepare();
			this.delegateToDelayedTweaker = true;
			return;
		}

		log.info("Loading stage1...");
		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();
		Capabilities.GameMetadata gameMetadata = capabilities.getGameMetadata();

		String targetSpecifier = gameMetadata.getTargetSpecifier();
		log.info("Target specifier: {}", targetSpecifier);

		checkForUpdates();
		maybeDownloadRelaunch();
		downloadOneConfigArtifacts();

		// Close our updater window, we're done
		if (this.loaderFrame != null) {
			this.loaderFrame.destroy();
		}

		try {
			ClassLoader classLoader = runtimeAccess.getClassLoader();
			log.info("Bootstrapping OneConfig...");

			oneconfigMainInstance = (oneconfigMainClass = classLoader.loadClass(ONECONFIG_MAIN_CLASS)).getConstructor().newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
			throw new RuntimeException(e);
		}
    }

	@Override
	public void postLoad() {
		if (this.delegateToDelayedTweaker) {
			DelayedStage0Tweaker.inject();
			return;
		}
		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();

		Relaunch relaunch = Relaunch.maybeCreate();

		RELAUNCH_STATE.set(true); // So that stage0 knows not to try to mess with files which are already loaded
		relaunch.maybeRelaunch(DetectionSupplier.maybeCreate(), runtimeAccess.getAppendedUrls());

		try {
			oneconfigMainClass.getDeclaredMethod("init").invoke(oneconfigMainInstance);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
    }

	private void checkForUpdates() {
		if (isUpdateChecked) {
			return;
		}

		boolean usingUpdateSnapshots = shouldUseSnapshots(STAGE1_ARTIFACT_SNAPSHOTS);
		BackendArtifact stage1Artifact = null;
		if (!IGNORE_UPDATES.get()) {
			stage1Artifact = readArtifactAt("https://api.polyfrost.org/v1/artifacts/stage1?snapshots=" + usingUpdateSnapshots);
			if (stage1Artifact == null) {
				// Retry with the opposite snapshot setting
				stage1Artifact = readArtifactAt("https://api.polyfrost.org/v1/artifacts/stage1?snapshots=" + !usingUpdateSnapshots);
				if (stage1Artifact == null) {
					throw new RuntimeException("Failed to fetch stage1 artifact");
				}
			}
		}

		Path dataDir = XDG
				.provideCacheDir("OneConfig")
				.resolve("loader")
				.resolve("data");
		Path selfFile = dataDir.resolve("stage1.jar");

		if (!IGNORE_UPDATES.get() && (!stage1Artifact.checksum.isMatching(selfFile))) {
			requestLoaderFrame();
			loaderFrame.updateMessage("Downloading OneConfig Loader stage 1...");
			stage1Artifact.downloadTo(getRequestHelper(), dataDir.resolve("stage1.update.jar"), loaderFrame::updateProgress);

			// TODO: Prompt for restart
		}

		isUpdateChecked = true;
	}

	@SneakyThrows
	private void maybeDownloadRelaunch() {
		if (isRelaunchDownloaded) {
			return;
		}

		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();

		boolean usingRelaunchSnapshots = shouldUseSnapshots(RELAUNCH_ARTIFACT_SNAPSHOTS);
		BackendArtifact relaunchArtifact = null;
		if (!IGNORE_UPDATES.get()) {
			relaunchArtifact = readArtifactAt("https://api.polyfrost.org/v1/artifacts/relaunch?snapshots=" + usingRelaunchSnapshots);
			if (relaunchArtifact == null) {
				// Retry with the opposite snapshot setting
				relaunchArtifact = readArtifactAt("https://api.polyfrost.org/v1/artifacts/relaunch?snapshots=" + !usingRelaunchSnapshots);
				if (relaunchArtifact == null) {
					throw new RuntimeException("Failed to fetch relaunch artifact");
				}
			}
		}

		Path dataDir = XDG
				.provideCacheDir("OneConfig")
				.resolve("loader")
				.resolve("data");
		Path relaunchFile = dataDir.resolve("relaunch.jar");

		if (!IGNORE_UPDATES.get() && (!Files.exists(relaunchFile) || !relaunchArtifact.checksum.isMatching(relaunchFile))) {
			requestLoaderFrame();
			loaderFrame.updateMessage("Downloading OneConfig Loader Relaunch...");
			relaunchArtifact.downloadTo(getRequestHelper(), relaunchFile, loaderFrame::updateProgress);
		}

		runtimeAccess.appendToClassPath("relaunch", false, relaunchFile.toUri().toURL());
		isRelaunchDownloaded = true;
	}

	@SneakyThrows
	private void downloadOneConfigArtifacts() {
		if (isOneConfigDownloaded) {
			return;
		}

		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();
		Capabilities.GameMetadata gameMetadata = capabilities.getGameMetadata();

		String gameVersion = gameMetadata.getGameVersion();
		String loaderName = gameMetadata.getLoaderName();

		Path dataDir = XDG
				.provideCacheDir("OneConfig")
				.resolve("loader")
				.resolve("data");
		Path artifactCacheFile = dataDir.resolve("artifact-cache.json");

		boolean usingSnapshots = shouldUseSnapshots(ONECONFIG_ARTIFACT_SNAPSHOTS);
		List<BackendArtifact> artifacts = null;

		if (!IGNORE_UPDATES.get()) {
			artifacts = readArtifactsAt("https://api.polyfrost.org/v1/artifacts/oneconfig?version=" + gameVersion + "&loader=" + loaderName + "&snapshots=" + usingSnapshots);

			String dummyArtifactsPath = System.getProperty("oneconfig.loader.stage1.dummyArtifacts");
			if (dummyArtifactsPath != null) {
				Path dummyArtifactsPathObj = Paths.get(dummyArtifactsPath);
				if (Files.exists(dummyArtifactsPathObj)) {
					artifacts = readArtifactsFrom(Files.newInputStream(dummyArtifactsPathObj));
				}
			}

			if (artifacts == null) {
				// Retry with the opposite snapshot setting
				artifacts = readArtifactsAt("https://api.polyfrost.org/v1/artifacts/oneconfig?version=" + gameVersion + "&loader=" + loaderName + "&snapshots=" + !usingSnapshots);
			}
		}

		if (artifacts == null) {
			if (Files.exists(artifactCacheFile)) {
				artifacts = readArtifactsFrom(Files.newInputStream(artifactCacheFile));
			}
		}

		if (artifacts == null || artifacts.isEmpty()) {
			throw new RuntimeException("Failed to fetch OneConfig artifacts");
		}

		// Compare all the hashes of any known artifacts and download any that are missing or have changed
		for (BackendArtifact artifact : artifacts) {
			Path artifactFile = dataDir.resolve(artifact.name + ".jar");
			if (!IGNORE_UPDATES.get() && (!Files.exists(artifactFile) || !artifact.checksum.isMatching(artifactFile))) {
				requestLoaderFrame();
				loaderFrame.updateMessage("Downloading OneConfig artifact: " + artifact.name);
				artifact.downloadTo(getRequestHelper(), artifactFile, loaderFrame::updateProgress);
			}

			if (artifact.jij) {
				Path jijDir = dataDir
						.resolve("jij")
						.resolve(artifact.name);

				// We need to extract all the JARs inside here and add them individually
				try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(artifactFile))) {
					// All of our JARs are inside META-INF/jars
					ZipEntry entry;
					while ((entry = jarInputStream.getNextJarEntry()) != null) {
						String name = entry.getName();
						if (name.startsWith("META-INF/jars/") && name.endsWith(".jar")) {
							// Now, we need to extract this JAR into it's own directory
							Path jarFile = jijDir.resolve(name.substring("META-INF/jars/".length()));
							if (!Files.exists(jarFile.getParent())) {
								Files.createDirectories(jarFile.getParent());
							}

							try (FileOutputStream outputStream = new FileOutputStream(jarFile.toFile())) {
								byte[] buffer = new byte[4096];
								int read;
								while ((read = jarInputStream.read(buffer)) != -1) {
									outputStream.write(buffer, 0, read);
								}
							}

							// Once the JAR is extracted, add it to the classpath
							String jarName = jarFile.getFileName().toString();
							runtimeAccess.appendToClassPath(jarName, artifact.name.contains("dependencies"), jarFile.toUri().toURL());
						}

						jarInputStream.closeEntry();
					}
				}
			} else {
				runtimeAccess.appendToClassPath(artifact.group + ":" + artifact.name, artifact.name.contains("dependencies"), artifactFile.toUri().toURL());
			}
		}

		// Write the new artifact cache
		try (OutputStream outputStream = Files.newOutputStream(artifactCacheFile)) {
			outputStream.write(new Gson().toJson(artifacts).getBytes(StandardCharsets.UTF_8));
		}

		isOneConfigDownloaded = true;
	}

	private void requestLoaderFrame() {
		if (this.loaderFrame == null) {
			log.info("Creating UI");
			loaderFrame = new LoaderFrame();
			loaderFrame.display();
		}
	}

	@SneakyThrows
	private BackendArtifact readArtifactAt(String url) {
		URLConnection connection;
		try {
			connection = getRequestHelper().establishConnection(URI.create(url).toURL());

			connection.connect();

			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				if (httpConnection.getResponseCode() != 200) {
					return null;
				}
			}
		} catch (Exception e) {
			log.error("Failed to connect to {}", url, e);
			return null;
		}

		try (InputStream inputStream = connection.getInputStream()) {
			return new Gson().fromJson(new String(IOUtils.readFully(inputStream), StandardCharsets.UTF_8), BackendArtifact.class);
		} catch (Exception e) {
			log.error("Failed to read artifact from {}", url, e);
			return null;
		}
	}

	@SneakyThrows
	private List<BackendArtifact> readArtifactsFrom(InputStream inputStream) {
		return new Gson().fromJson(new String(IOUtils.readFully(inputStream), StandardCharsets.UTF_8), new CustomBackendArtifactListType());
	}

	@SneakyThrows
	private List<BackendArtifact> readArtifactsAt(String url) {
		URLConnection connection = getRequestHelper().establishConnection(URI.create(url).toURL());
		log.info("Fetching artifacts from {}", url);

		connection.connect();

		if (connection instanceof HttpURLConnection) {
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			if (httpConnection.getResponseCode() != 200) {
				return null;
			}
		}

		try (InputStream inputStream = connection.getInputStream()) {
			return readArtifactsFrom(inputStream);
		} catch (Exception e) {
			log.error("Failed to read artifacts from {}", url, e);
			return null;
		}
	}

	private static boolean shouldUseSnapshots(Property<Boolean> property) {
		if (ARTIFACT_SNAPSHOTS.get()) {
			return true;
		}
		return property.get();
	}

	public static class CustomBackendArtifactListType implements ParameterizedType {
		@Override
		public Type @NotNull [] getActualTypeArguments() {
			return new Type[]{BackendArtifact.class};
		}

		@Override
		public @NotNull Type getRawType() {
			return List.class;
		}

		@Override
		public Type getOwnerType() {
			return null;
		}
	}
}
