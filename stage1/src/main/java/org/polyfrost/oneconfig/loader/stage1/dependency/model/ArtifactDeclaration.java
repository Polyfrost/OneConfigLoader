package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenArtifactDependency;

import java.util.List;

/**
 * A resolved declaration of an artifact, with its hard-referenced dependencies and other properties.
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface ArtifactDeclaration {
	Artifact getArtifact();

	List<MavenArtifactDependency> getDependencies();
}
