package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Data;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

import java.util.List;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class MavenArtifactDeclaration implements ArtifactDeclaration {
    private final MavenArtifact artifact;
    private final List<MavenArtifactDependency> dependencies;

    public void resolveDependencies(DependencyManager<MavenArtifact, MavenArtifactDeclaration> dependencyManager) {
        for (MavenArtifactDependency dependency : dependencies) {
            MavenArtifactDeclaration declaration = dependency.getDeclaration();
            if (declaration == null) {
                declaration = dependencyManager.resolveArtifact(dependency.getDeclaration().getArtifact());
                dependency.setDeclaration(declaration);
                declaration.resolveDependencies(dependencyManager);
            }
        }
    }
}
