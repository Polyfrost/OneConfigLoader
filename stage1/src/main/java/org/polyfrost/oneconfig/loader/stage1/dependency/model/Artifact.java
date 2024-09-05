package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import java.util.List;

/**
 * A resolved declaration of an artifact, with its hard-referenced dependencies and other properties.
 *
 * @param <T> the declaration type
 * @param <S> the dependency type
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface Artifact<T extends ArtifactDeclaration, S extends ArtifactDependency> {
	T getDeclaration();

	List<S> getDependencies();
}
