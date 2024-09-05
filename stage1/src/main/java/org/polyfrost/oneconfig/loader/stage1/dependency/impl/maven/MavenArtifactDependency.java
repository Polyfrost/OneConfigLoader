package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.util.List;

import lombok.Data;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.DependencyExclusion;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;

public @Data class MavenArtifactDependency implements ArtifactDependency {
	private final MavenArtifactDeclaration declaration;
	private final Scope scope;
	private final List<DependencyExclusion> exclusions;
}
