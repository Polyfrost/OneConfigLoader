package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import java.util.List;

/**
 * A resolved declaration of an artifact, with its hard-referenced dependencies and other properties.
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface Artifact<DECLARATION extends ArtifactDeclaration, DEPENDENCY extends ArtifactDependency> {
	DECLARATION getDeclaration();

	List<DEPENDENCY> getDependencies();
}
