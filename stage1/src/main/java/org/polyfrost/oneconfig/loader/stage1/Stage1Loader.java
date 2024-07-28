package org.polyfrost.oneconfig.loader.stage1;

import lombok.SneakyThrows;
import org.polyfrost.oneconfig.loader.LoaderBase;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenArtifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenDependencyManager;
import org.polyfrost.oneconfig.loader.stage1.util.SystemProperties;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class Stage1Loader extends LoaderBase {
    private final Attributes manifestAttributes;

    private final RequestHelper requestHelper;

    private final XDG.ApplicationStore applicationStore;
    private final DependencyManager<MavenArtifact, MavenArtifactDeclaration> dependencyManager;
    private final String oneConfigVersion;

    @SneakyThrows
    public Stage1Loader(Capabilities capabilities, RequestHelper requestHelper) {
        super(
                "stage1",
                Stage1Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );

        this.requestHelper = requestHelper;

        this.applicationStore = XDG.provideApplicationStore("Polyfrost/OneConfig/loader");

        this.dependencyManager = new MavenDependencyManager(
                this.applicationStore,
                SystemProperties.REPOSITORY_URL.get(),
                this.requestHelper
        );

        try (JarFile jarFile = new JarFile(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath())) {
            this.manifestAttributes = jarFile.getManifest().getMainAttributes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String oneConfigVersion = this.manifestAttributes.getValue("OneConfig-Version");
        if (oneConfigVersion == null) {
            throw new RuntimeException("OneConfig-Version option is not found in MANIFEST.MF");
        }
        this.oneConfigVersion = oneConfigVersion;
    }

    @Override
    public void load() {
        // Fetch oneConfig version info
        MavenArtifact oneConfigArtifact = this.dependencyManager.buildArtifact("org.polyfrost", "oneconfig", this.oneConfigVersion);

        // Download to cache
        checkAndAppendArtifactToClasspath(oneConfigArtifact);

        MavenArtifactDeclaration mavenArtifactDeclaration = this.dependencyManager.resolveArtifact(oneConfigArtifact);
        mavenArtifactDeclaration.resolveDependencies(this.dependencyManager);

        mavenArtifactDeclaration
                .getDependencies()
                .stream()
                .flatMap((dependency) -> dependency.getDeclaration().getDependencies().stream())
                .forEach((dependency) -> checkAndAppendArtifactToClasspath(dependency.getDeclaration().getArtifact()));

        try {
            this.capabilities
                    .getClassLoader()
                    .loadClass("org.spongepowered.asm.mixin.Mixins")
                    .getDeclaredMethod("addConfigurations", String[].class)
                    .invoke(null, (Object) new String[] { "mixins.oneconfig.json" });

            String oneConfigMainClass = this.manifestAttributes.getValue("OneConfig-Main-Class");
            if (oneConfigMainClass == null) {
                throw new RuntimeException("OneConfig-Main-Class option is not found in MANIFEST.MF");
            }
            this.capabilities
                    .getClassLoader()
                    .loadClass(this.manifestAttributes.getValue(oneConfigMainClass))
                    // TODO: CHANGE PARAMETER
                    .getDeclaredMethod("init")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Path provideLocalArtifactPath(MavenArtifact mavenArtifact) {
        return XDG
                .provideCacheDir("OneConfig")
                .resolve("loader")
                .resolve(XDG
                        .provideCacheDir("OneConfig")
                        .resolve("loader")
                        .resolve(mavenArtifact.getRelativePath())
                );
    }

    private void checkAndAppendArtifactToClasspath(MavenArtifact mavenArtifact) {
        File artifactFile = provideLocalArtifactPath(mavenArtifact).toFile();

        if (!artifactFile.exists()) {
            downloadArtifact(mavenArtifact);
        }

        this.appendToClasspath(artifactFile);
    }

    private void downloadArtifact(MavenArtifact mavenArtifact) {
        try {
            IOUtils.readInto(
                    this.dependencyManager.createArtifactInputStream(mavenArtifact),
                    Files.newOutputStream(provideLocalArtifactPath(mavenArtifact))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void appendToClasspath(File jar) {
        try {
            this.capabilities.appendToClassPath(false, jar.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
