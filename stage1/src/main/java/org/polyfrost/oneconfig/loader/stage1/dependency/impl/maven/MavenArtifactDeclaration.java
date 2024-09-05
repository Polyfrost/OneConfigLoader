package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Data;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class MavenArtifactDeclaration implements ArtifactDeclaration {
	private final String groupId;
	private final String artifactId;
	private final String version;
	private String actualVersion;
    private final String classifier;
    private final String extension;

	public String getVersion() {
		if (actualVersion == null) {
			return version;
		}
		return actualVersion;
	}

	@Override
    public String getDeclaration() {
		StringBuilder builder = new StringBuilder();
		builder.append(groupId).append(":").append(artifactId).append(":").append(actualVersion);

		if (classifier != null) {
			builder.append(":").append(classifier);
		}

		if (extension != null) {
			builder.append("@").append(extension);
		}

		return builder.toString();
    }

    @Override
    public Path getRelativePath() {
        return Paths.get(
                groupId.replace('.', '/'),
                artifactId,
                actualVersion,
                getFileName()
        );
    }

    @Override
    public String getFileName() {
        return artifactId + "-" + actualVersion
                + (classifier == null ? "" : "-" + classifier)
                + "." + (extension == null ? "jar" : extension);
    }

}
