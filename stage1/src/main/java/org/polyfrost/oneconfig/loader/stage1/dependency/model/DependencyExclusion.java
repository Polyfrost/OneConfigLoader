package org.polyfrost.oneconfig.loader.stage1.dependency.model;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface DependencyExclusion<A extends Artifact<?, ?>> {
    /**
     * Performs a match against the given artifact.
     *
     * @param artifact the artifact to match against
     *
     * @return {@code true} if the artifact matches the exclusion, {@code false} otherwise
     */
    boolean matches(A artifact);
}
