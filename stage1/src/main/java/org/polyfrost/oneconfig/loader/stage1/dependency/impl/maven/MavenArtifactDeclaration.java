package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import lombok.Data;

import lombok.Getter;

import lombok.RequiredArgsConstructor;

import lombok.Setter;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public class MavenArtifactDeclaration implements ArtifactDeclaration {

	@Setter
	private boolean isSnapshot = false;

	@Setter
	private String snapshotVersion;

	@Setter
	private String actualVersion;

	private final String groupId;
	private final String artifactId;
	private final String version;
    private final String classifier;
    private final String extension;

	@Override
    public String getDeclaration() {
		ensureVersion();

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

	public Path getRelativePath() {
		ensureVersion();

		return Paths.get(
				groupId.replace('.', '/'),
				artifactId,
				actualVersion,
				getFileName()
		);
	}

	@Override
    public String getFileName() {
		ensureVersion();

		StringBuilder builder = new StringBuilder();
		builder.append(artifactId).append("-");

		if (isSnapshot) {
			builder.append(snapshotVersion);
		} else {
			builder.append(actualVersion);
		}

		if (classifier != null) {
			builder.append("-").append(classifier);
		}

		if (extension != null) {
			builder.append(".").append(extension);
		} else {
			builder.append(".jar");
		}

		return builder.toString();
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MavenArtifactDeclaration that = (MavenArtifactDeclaration) o;
		ensureVersion();
		return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(actualVersion, that.actualVersion) && Objects.equals(classifier, that.classifier) && Objects.equals(extension, that.extension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, artifactId, version, classifier, extension);
	}

	@Override
	public String toString() {
		return getDeclaration();
	}

	private void ensureVersion() {
		if (actualVersion == null) {
			actualVersion = !isSnapshot ? version : snapshotVersion;
		}
	}
}
