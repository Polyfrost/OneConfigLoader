package org.polyfrost.oneconfig.loader.stage1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.stage1.dependency.ArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.MavenArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;
import org.polyfrost.oneconfig.loader.utils.ErrorHandler;
import org.polyfrost.oneconfig.loader.utils.XDG;

/**
 * @author xtrm
 * @since 1.1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Stage1Loader extends LoaderBase {
	private static final String PROPERTIES_FILE_PATH = "/assets/oneconfig-loader/metadata/stage1.properties";
	private final ArtifactManager artifactManager;
	private final Properties stage1Properties = new Properties();

	@SneakyThrows
	public Stage1Loader(Capabilities capabilities) {
		super(
				"stage1",
				Stage1Loader.class.getPackage().getImplementationVersion(),
				capabilities
		);

		try (InputStream inputStream = this.getClass().getResourceAsStream(PROPERTIES_FILE_PATH)) {
			this.stage1Properties.load(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		String repository = this.stage1Properties.getProperty("oneconfig-repository");
		if (repository == null) {
			throw new RuntimeException("oneconfig-repository option is not found in stage1.properties");
		}

		System.out.println("Repository: " + repository);
		URI repositoryURI = URI.create(repository);
		this.artifactManager = new MavenArtifactManager(
				XDG.provideApplicationStore("OneConfig"),
				repositoryURI,
				getRequestHelper()
		);
	}

	@Override
	public void load() {
		Capabilities capabilities = getCapabilities();
		Capabilities.RuntimeAccess runtimeAccess = capabilities.getRuntimeAccess();
		Capabilities.GameMetadata gameMetadata = capabilities.getGameMetadata();

		String oneConfigVersion = this.stage1Properties.getProperty("oneconfig-version");
		if (oneConfigVersion == null) {
			throw new RuntimeException("oneconfig-version option is not found in stage1.properties");
		}
		String targetSpecifier = gameMetadata.getTargetSpecifier();

		// Fetch oneConfig version info
		String artifactSpecifier = "org.polyfrost.oneconfig:" + targetSpecifier + ":" + oneConfigVersion;
		final Set<ArtifactDeclaration> resolveQueue = new HashSet<>();
		final Set<Artifact> resolvedArtifacts = new HashSet<>();
		ArtifactDeclaration oneConfigDeclaration = this.artifactManager.buildArtifactDeclaration(artifactSpecifier);
		resolveQueue.add(oneConfigDeclaration);

		while (!resolveQueue.isEmpty()) {
			ArtifactDeclaration artifactDeclaration = resolveQueue.iterator().next();
			resolveQueue.remove(artifactDeclaration);
			Artifact<ArtifactDeclaration, ArtifactDependency> resolvedArtifact;

			try {
				resolvedArtifact = this.artifactManager.resolveArtifact(artifactDeclaration);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (resolvedArtifact != null) {
				resolvedArtifacts.add(resolvedArtifact);
				resolveQueue.addAll(resolvedArtifact.getDependencies().stream().map(ArtifactDependency::getDeclaration).collect(Collectors.toSet()));
				resolveQueue.removeIf(it -> resolvedArtifact.getDependencies().stream().anyMatch(dependency -> it.equals(dependency.getDeclaration())));
			}
		}
		resolvedArtifacts.forEach(artifact -> checkAndAppendArtifactToClasspath(runtimeAccess, artifact));

////		// Download to cache
////		checkAndAppendArtifactToClasspath(runtimeAccess, oneConfigDeclaration);
//
//		Artifact mavenArtifactDeclaration;
//		try {
//			mavenArtifactDeclaration = this.artifactManager.resolveArtifact(oneConfigDeclaration);
//		} catch (Exception e) {
//			logger.fatal("Could not resolve artifact {}", oneConfigDeclaration.getDeclaration(), e);
//			ErrorHandler.displayError(this, "Error while resolving artifact: " + oneConfigDeclaration.getDeclaration());
//			return;
//		}
//		try {
//			mavenArtifactDeclaration.resolveDependencies(this.artifactManager);
//		} catch (Exception e) {
//			logger.fatal("Could not resolve dependencies", e);
//			ErrorHandler.displayError(this, "Error while resolving dependencies");
//			return;
//		}
//
//		mavenArtifactDeclaration
//				.getDependencies()
//				.stream()
//				.flatMap((dependency) -> dependency.getDeclaration().getDependencies().stream())
//				.forEach((dependency) -> checkAndAppendArtifactToClasspath(runtimeAccess, dependency.getDeclaration().getArtifact()));

		try {
			ClassLoader classLoader = runtimeAccess.getClassLoader();
			classLoader.loadClass("org.spongepowered.asm.mixin.Mixins")
					.getDeclaredMethod("addConfigurations", String[].class)
					.invoke(null, (Object) new String[]{"mixins.oneconfig.json"});

			String oneConfigMainClass = this.stage1Properties.getProperty("oneconfig-main-class");
			if (oneConfigMainClass == null) {
				throw new RuntimeException("oneconfig-main-class option is not found in stage1.properties");
			}

			classLoader.loadClass(oneConfigMainClass)
					// TODO: CHANGE PARAMETER
					.getDeclaredMethod("init")
					.invoke(capabilities);
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

	private void checkAndAppendArtifactToClasspath(Capabilities.RuntimeAccess runtimeAccess, Artifact artifact) {
		Path artifactFile = provideLocalArtifactPath(artifact);

		if (!Files.exists(artifactFile)) {
			try (InputStream inputStream = this.artifactManager.createArtifactInputStream(artifact)) {
				Files.copy(inputStream, provideLocalArtifactPath(artifact));
			} catch (IOException e) {
				logger.fatal("Could not download artifact {}", artifact.getDeclaration(), e);
				ErrorHandler.displayError(this, "Error while downloading artifact: " + artifact.getDeclaration());
			}
		}

		try {
			runtimeAccess.appendToClassPath(false, artifactFile.toUri().toURL());
		} catch (IOException e) {
			throw new RuntimeException("Failed to append artifact to class path", e);
		}
	}
}
