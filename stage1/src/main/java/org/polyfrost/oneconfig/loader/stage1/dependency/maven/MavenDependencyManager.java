package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.stage1.util.XDG;

import java.net.URI;
import java.nio.file.Path;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class MavenDependencyManager implements DependencyManager<MavenArtifact, MavenArtifactDeclaration> {
    private final XDG.ApplicationStore store;
    private final URI repository;
    private final CachingSolution cache;

    public MavenDependencyManager(XDG.ApplicationStore store, URI repository) {
        this.store = store;
        this.repository = repository;
        this.cache = new MavenCachingSolution(store, repository);
    }

    @Override
    public MavenArtifact buildArtifact(String groupId, String artifactId, String version) {
        return new MavenArtifact(groupId, artifactId, version, null, "jar");
    }

    @Override
    public MavenArtifactDeclaration resolveArtifact(MavenArtifact artifact) {
        Path dataDir = store.getDataDir();
        Path localLibraries = dataDir.resolve("libraries");

        Path artifactRelativePath = artifact.getRelativePath();
        Path localArtifactPath = localLibraries.resolve(artifactRelativePath);

        Path localPomFile = localLibraries.resolve(artifactRelativePath.toString().replace(artifact.getExtension(), "pom"));
        Path remotePomFile = repository
        if (localPomFile.toFile().exists()) {
            return new MavenArtifactDeclaration(artifact);
        }
        return null;
    }
}
