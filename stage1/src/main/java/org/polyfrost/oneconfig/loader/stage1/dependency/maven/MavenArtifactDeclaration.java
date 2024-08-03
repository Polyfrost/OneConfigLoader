package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Data;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;

import java.util.List;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class MavenArtifactDeclaration implements ArtifactDeclaration {
    private final MavenArtifact artifact;
    private final List<ArtifactDependency> dependencies;

    public void resolveDependencies(DependencyManager<MavenArtifact, MavenArtifactDeclaration> dependencyManager) throws Exception {
        for (ArtifactDependency dependency : dependencies) {
            MavenArtifactDependency mavenDependency = (MavenArtifactDependency) dependency;
            MavenArtifactDeclaration declaration = (MavenArtifactDeclaration) mavenDependency.getDeclaration();
            if (declaration == null) {
                //FIXME: ?
                declaration = dependencyManager.resolveArtifact((MavenArtifact) mavenDependency.getDeclaration().getArtifact());
                mavenDependency.setDeclaration(declaration);
                declaration.resolveDependencies(dependencyManager);
            }
        }
    }
}
