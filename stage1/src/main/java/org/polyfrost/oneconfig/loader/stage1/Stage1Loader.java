package org.polyfrost.oneconfig.loader.stage1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.MavenArtifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.MavenArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.MavenArtifactDependency;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.MavenArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.utils.ErrorHandler;
import org.polyfrost.oneconfig.loader.utils.XDG;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
@SuppressWarnings({"rawtypes"})
public class Stage1Loader extends LoaderBase {
	private static final String PROPERTIES_FILE_PATH = "/assets/oneconfig-loader/metadata/stage1.properties";
	private final MavenArtifactManager artifactManager;
	private final Properties stage1Properties = new Properties();

	@SneakyThrows
	public Stage1Loader(Capabilities capabilities) {
		super(
				"stage1",
				Stage1Loader.class.getPackage().getImplementationVersion(),
				capabilities
		);

		log.info("Loading stage1 properties from {}", PROPERTIES_FILE_PATH);
		try (InputStream inputStream = this.getClass().getResourceAsStream(PROPERTIES_FILE_PATH)) {
			this.stage1Properties.load(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("Looking for raw repositories...");
		String rawRepositories = this.stage1Properties.getProperty("oneconfig-repositories");
		if (rawRepositories == null) {
			throw new RuntimeException("oneconfig-repositories option is not found in stage1.properties");
		}

		log.info("Found raw repositories: {}", rawRepositories);
		URI[] repositories = Arrays.stream(rawRepositories.split(",")).map(rawRepository -> {
			// If the repository does not end with a slash, add it
			if (!rawRepository.endsWith("/")) {
				rawRepository += "/";
			}

			return URI.create(rawRepository);
		}).distinct().toArray(URI[]::new);

		log.info("Creating artifact manager...");
		this.artifactManager = new MavenArtifactManager(
				XDG.provideApplicationStore("OneConfig"),
				getRequestHelper(),
				repositories
		);

		log.info("Artifact manager created");
	}

	@Override
	public void load() {
		long startTime = System.currentTimeMillis();

		log.info("Loading stage1...");
		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();
		Capabilities.GameMetadata gameMetadata = capabilities.getGameMetadata();

		log.info("Looking for OneConfig version...");
		String oneConfigVersion = this.stage1Properties.getProperty("oneconfig-version");
		if (oneConfigVersion == null) {
			throw new RuntimeException("oneconfig-version option is not found in stage1.properties");
		}

		String targetSpecifier = gameMetadata.getTargetSpecifier();
		log.info("Target specifier: {}", targetSpecifier);

		// Fetch oneConfig version info
		String artifactSpecifier = "org.polyfrost.oneconfig:" + targetSpecifier + ":" + oneConfigVersion;
		final Set<MavenArtifactDeclaration> resolveQueue = new HashSet<>();
		final Set<MavenArtifact> resolvedArtifacts = new HashSet<>();
		MavenArtifactDeclaration oneConfigDeclaration = this.artifactManager.buildArtifactDeclaration(artifactSpecifier);
		oneConfigDeclaration.setShouldValidate(true);
		log.info("Resolving OneConfig artifact: {}", oneConfigDeclaration);
		resolveQueue.add(oneConfigDeclaration);

		while (!resolveQueue.isEmpty()) {
			MavenArtifactDeclaration artifactDeclaration = resolveQueue.iterator().next();
			resolveQueue.remove(artifactDeclaration);
			MavenArtifact resolvedArtifact;

			try {
				resolvedArtifact = this.artifactManager.getArtifactResolver().resolveArtifact(artifactDeclaration);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (resolvedArtifact != null) {
				resolvedArtifacts.add(resolvedArtifact);

				Set<MavenArtifactDeclaration> newDependencies = resolvedArtifact
						.getDependencies()
						.stream()
						.map(MavenArtifactDependency::getDeclaration)
						.filter(it -> resolvedArtifacts.stream().noneMatch(artifact -> artifact.getDeclaration().equals(it)))
						.collect(Collectors.toSet());
				resolveQueue.addAll(newDependencies);
			} else {
				logger.warn("Could not resolve artifact {}", artifactDeclaration);
			}
		}

		log.info("Found {} artifacts to load:", resolvedArtifacts.size());

		resolvedArtifacts.forEach(artifact -> checkAndAppendArtifactToClasspath(runtimeAccess, artifact));

		log.info("OneConfig artifacts loaded in {}ms", System.currentTimeMillis() - startTime);

		runtimeAccess.doRelaunchShitREMOVETHISLATER();

		try {
			ClassLoader classLoader = runtimeAccess.getClassLoader();
			String oneConfigMainClass = this.stage1Properties.getProperty("oneconfig-main-class");
			if (oneConfigMainClass == null) {
				throw new RuntimeException("oneconfig-main-class option is not found in stage1.properties");
			}

			classLoader.loadClass(oneConfigMainClass)
					.getDeclaredMethod("init")
					.invoke(null);
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
				 IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private Path provideLocalArtifactPath(Artifact mavenArtifact) {
		return XDG
				.provideCacheDir("OneConfig")
				.resolve("loader")
				.resolve(XDG
						.provideCacheDir("OneConfig")
						.resolve("loader")
						.resolve(mavenArtifact.getDeclaration().getRelativePath())
				);
	}

	private void checkAndAppendArtifactToClasspath(Capabilities.RuntimeAccess runtimeAccess, MavenArtifact artifact) {
		Path artifactFile = provideLocalArtifactPath(artifact);

		if (!Files.exists(artifactFile)) {
			try (InputStream inputStream = this.artifactManager.createArtifactInputStream(artifact)) {
				if (inputStream == null) {
					return; // Skip if the artifact is not found
				}

				Files.createDirectories(artifactFile.getParent());
				Files.copy(inputStream, artifactFile);
			} catch (IOException e) {
				logger.fatal("Could not download artifact {}", artifact.getDeclaration(), e);
				ErrorHandler.displayError(this, "Error while downloading artifact: " + artifact.getDeclaration());
			}
		}

		try {
			logger.info("Appending artifact {} to class path", artifact.getDeclaration());
			runtimeAccess.appendToClassPath(artifact.getDeclaration().getDeclaration(), false, artifactFile.toUri().toURL());
		} catch (IOException e) {
			throw new RuntimeException("Failed to append artifact to class path", e);
		}
	}
}
