package org.polyfrost.oneconfig.loader.stage1.dependency.model;

public interface ArtifactResolver<A extends Artifact<?, ?>, D extends ArtifactDeclaration> {

	A resolveArtifact(D declaration) throws Exception;

}
