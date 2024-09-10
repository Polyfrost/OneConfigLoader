package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import lombok.Data;
import lombok.Getter;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.DependencyExclusion;

@Getter
public @Data class MavenDependencyExclusion implements DependencyExclusion<MavenArtifact> {
    private final String groupId;
    private final String artifactId;

    public MavenDependencyExclusion(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public MavenDependencyExclusion(MavenArtifactDeclaration declaration) {
        this(declaration.getGroupId(), declaration.getArtifactId());
    }

    @Override
    public boolean matches(MavenArtifact artifact) {
        return this.groupId.equals(artifact.getDeclaration().getGroupId()) && this.artifactId.equals(artifact.getDeclaration().getArtifactId());
    }
}
