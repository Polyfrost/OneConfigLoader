package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.util.FlexVerComparator;

/**
 * @author xtrm
 * @author Deftu
 * @since 1.1.0
 */
@Getter
@Setter
@RequiredArgsConstructor
public class MavenArtifactDeclaration implements ArtifactDeclaration, Comparable<MavenArtifactDeclaration> {
	private final String groupId;
	private final String artifactId;
	private final String version;
    private final String classifier;
    private final String extension;
	private boolean shouldValidate = false;
	private boolean isSnapshot = false;
	private String snapshotVersion;
	private String actualVersion;

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

	@Override
	public int compareTo(@NotNull MavenArtifactDeclaration o) {
		if (this == o) return 0;
		if (!Objects.equals(groupId, o.groupId)) return groupId.compareTo(o.groupId);
		if (!Objects.equals(artifactId, o.artifactId)) return artifactId.compareTo(o.artifactId);
		ensureVersion();
		return FlexVerComparator.compare(o.actualVersion, actualVersion);
	}
}
