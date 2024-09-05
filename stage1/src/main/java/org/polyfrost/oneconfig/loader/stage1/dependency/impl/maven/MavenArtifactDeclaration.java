package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.Getter;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

/**
 * @author xtrm
 * @since 1.1.0
 */
@AllArgsConstructor
public @Data class MavenArtifactDeclaration implements ArtifactDeclaration {
	@Getter
	private final String groupId;
	@Getter
	private final String artifactId;
	@Getter
	private final String version;
    private final String classifier;
	@Getter
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
