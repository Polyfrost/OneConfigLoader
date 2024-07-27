package org.polyfrost.oneconfig.loader.stage1.dependency;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;

import java.io.InputStream;

/**
 *
 * @param <T>
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface DependencyManager<A extends Artifact, D extends ArtifactDeclaration> {
    A buildArtifact(String groupId, String artifactId, String version, String classifier, String extension);
    default A buildArtifact(String groupId, String artifactId, String version) { return buildArtifact(groupId, artifactId, version, null, "jar"); }
    D resolveArtifact(A artifact);
    InputStream createArtifactInputStream(A artifact);
}
