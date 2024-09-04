package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.DependencyExclusion;

@Getter
public class MavenDependencyExclusion implements DependencyExclusion {
    private final String groupId;
    private final String artifactId;

    public MavenDependencyExclusion(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public MavenDependencyExclusion(Artifact artifact) {
        this(artifact.getGroupId(), artifact.getArtifactId());
    }

    @Override
    public boolean matches(Artifact artifact) {
        return this.groupId.equals(artifact.getGroupId()) && this.artifactId.equals(artifact.getArtifactId());
    }
}
