package org.polyfrost.oneconfig.loader.stage1.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

/**
 * @param <A> {@link Artifact} type
 * @param <D> {@link ArtifactDeclaration} type
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface ArtifactManager<A extends Artifact, D extends ArtifactDeclaration> {
    D buildArtifactDeclaration(String stringDeclaration);

    D buildArtifactDeclaration(Map<String, String> elements);

    A resolveArtifact(D declaration) throws Exception;

	InputStream createArtifactInputStream(A artifact) throws IOException;

    Class<A> getArtifactClass();

	Class<D> getArtifactDeclarationClass();
}
