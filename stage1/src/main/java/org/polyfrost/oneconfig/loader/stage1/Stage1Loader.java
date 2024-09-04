package org.polyfrost.oneconfig.loader.stage1;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenArtifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenDependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.util.SystemProperties;
import org.polyfrost.oneconfig.loader.utils.ErrorHandler;
import org.polyfrost.oneconfig.loader.utils.XDG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class Stage1Loader extends LoaderBase {

	private final Properties stage1Properties;

    private final XDG.ApplicationStore applicationStore;
    private final DependencyManager<MavenArtifact, MavenArtifactDeclaration> dependencyManager;
    private final String oneConfigVersion;

    @SneakyThrows
    public Stage1Loader(Capabilities capabilities) {
        super(
                "stage1",
                Stage1Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );

        this.applicationStore = XDG.provideApplicationStore("OneConfig");

        this.dependencyManager = new MavenDependencyManager(
                this.applicationStore,
                SystemProperties.REPOSITORY_URL.get(),
                this.getRequestHelper()
        );

		try (InputStream inputStream = this.getClass().getResourceAsStream("/polyfrost/stage1.properties")) {
			this.stage1Properties = new Properties();
			this.stage1Properties.load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        String oneConfigVersion = this.stage1Properties.getProperty("oneconfig-version");
        if (oneConfigVersion == null) {
            throw new RuntimeException("oneconfig-version option is not found in stage1.properties");
        }

        this.oneConfigVersion = oneConfigVersion;
    }

    @Override
    public void load() {
		// Get platform info
		String modLoader = getCapabilities().getModLoaderName();
		String gameVersion = getCapabilities().getGameVersion();

        // Fetch oneConfig version info
        MavenArtifact oneConfigArtifact = this.dependencyManager.buildArtifact("org.polyfrost", String.format("oneconfig-%s-%s", gameVersion, modLoader), this.oneConfigVersion);

        // Download to cache
        checkAndAppendArtifactToClasspath(oneConfigArtifact);

        MavenArtifactDeclaration mavenArtifactDeclaration;
        try {
            mavenArtifactDeclaration = this.dependencyManager.resolveArtifact(oneConfigArtifact);
        } catch (Exception e) {
            getLogger().fatal("Could not resolve artifact {}", oneConfigArtifact.getDeclaration(), e);
            ErrorHandler.displayError(this, "Error while resolving artifact: " + oneConfigArtifact.getDeclaration());
            return;
        }
        try {
            mavenArtifactDeclaration.resolveDependencies(this.dependencyManager);
        } catch (Exception e) {
            getLogger().fatal("Could not resolve dependencies", e);
            ErrorHandler.displayError(this, "Error while resolving dependencies");
            return;
        }

        mavenArtifactDeclaration
                .getDependencies()
                .stream()
                .flatMap((dependency) -> dependency.getDeclaration().getDependencies().stream())
                .forEach((dependency) -> checkAndAppendArtifactToClasspath(dependency.getDeclaration().getArtifact()));

        try {
            this.getCapabilities()
                    .getClassLoader()
                    .loadClass("org.spongepowered.asm.mixin.Mixins")
                    .getDeclaredMethod("addConfigurations", String[].class)
                    .invoke(null, (Object) new String[] { "mixins.oneconfig.json" });

            String oneConfigMainClass = this.stage1Properties.getProperty("oneconfig-main-class");
            if (oneConfigMainClass == null) {
                throw new RuntimeException("oneconfig-main-class option is not found in stage1.properties");
            }

            this.getCapabilities()
                    .getClassLoader()
                    .loadClass(oneConfigMainClass)
                    // TODO: CHANGE PARAMETER
                    .getDeclaredMethod("init")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
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
                        .resolve(mavenArtifact.getRelativePath())
                );
    }

    private void checkAndAppendArtifactToClasspath(Artifact artifact){
        File artifactFile = provideLocalArtifactPath(artifact).toFile();

        if (!artifactFile.exists()) {
            try (InputStream inputStream = this.dependencyManager.createArtifactInputStream(this.dependencyManager.getArtifactClass().cast(artifact))) {
                Files.copy(inputStream, provideLocalArtifactPath(artifact));
            } catch (IOException e) {
                getLogger().fatal("Could not download artifact {}", artifact.getDeclaration(), e);
                ErrorHandler.displayError(this, "Error while downloading artifact: " + artifact.getDeclaration());
            }
        }

        this.appendToClasspath(artifactFile);
    }

    private void appendToClasspath(File jar) {
        try {
            this.getCapabilities().appendToClassPath(false, jar.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
