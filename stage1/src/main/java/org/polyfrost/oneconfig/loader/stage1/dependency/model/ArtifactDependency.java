package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import java.util.List;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface ArtifactDependency {
    ArtifactDeclaration getDeclaration();

    Scope getScope();

    List<DependencyExclusion> getExclusions();
}
