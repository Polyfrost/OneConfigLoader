package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Data;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class MavenArtifact implements Artifact {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String extension;

    @Override
    public String getDeclaration() {
        return String.format(
                "%s:%s:%s",
                groupId, artifactId, version
        ) + (classifier == null ? "" : ":" + classifier)
                + (extension == null ? "" : "@" + extension);
    }

    @Override
    public Path getRelativePath() {
        return Paths.get(
                groupId.replace('.', '/'),
                artifactId,
                version,
                getFileName()
        );
    }

    @Override
    public String getFileName() {
        return artifactId + "-" + version
                + (classifier == null ? "" : "-" + classifier)
                + "." + (extension == null ? "jar" : extension);
    }
}
