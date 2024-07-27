package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.DependencyExclusion;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;

@Getter
@RequiredArgsConstructor
public class MavenArtifactDependency implements ArtifactDependency {
    @Setter private MavenArtifactDeclaration declaration;
    private Scope scope;
    private DependencyExclusion exclusions;
}
