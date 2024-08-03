package org.polyfrost.oneconfig.loader.stage1.dependency;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

import java.io.IOException;
import java.io.InputStream;

/**
 * @param <A> {@link Artifact} type
 * @param <D> {@link ArtifactDeclaration} type
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface DependencyManager<A extends Artifact, D extends ArtifactDeclaration> {
    A buildArtifact(String groupId, String artifactId, String version, String classifier, String extension);
    default A buildArtifact(String groupId, String artifactId, String version) { return buildArtifact(groupId, artifactId, version, null, "jar"); }
    D resolveArtifact(A artifact) throws Exception;
    InputStream createArtifactInputStream(A artifact) throws IOException;

    Class<A> getArtifactClass();
    Class<D> getArtifactDeclarationClass();
}
