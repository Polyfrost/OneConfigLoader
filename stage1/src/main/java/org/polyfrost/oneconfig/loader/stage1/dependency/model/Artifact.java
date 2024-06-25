package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * A generic artifact, part of the dependency resolution system.
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface Artifact {
    /**
     * @return the artifact's group ID
     */
    String getGroupId();

    /**
     * @return the artifact's name/ID
     */
    String getArtifactId();

    /**
     * @return the artifact's version
     */
    String getVersion();

    /**
     * @return the artifact's classifier (usually {@code null})
     */
    @Nullable String getClassifier();

    /**
     * @return the artifact's extension (usually {@code jar})
     */
    String getExtension();

    /**
     * @return a service-specific declaration of the artifact
     */
    String getDeclaration();

    /**
     * @return the relative path of the artifact
     */
    Path getRelativePath();

    /**
     * @return the file name of the artifact
     */
    String getFileName();
}
