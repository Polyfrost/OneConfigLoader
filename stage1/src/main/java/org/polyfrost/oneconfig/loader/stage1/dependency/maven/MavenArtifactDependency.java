package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.DependencyExclusion;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;

import java.util.List;

public class MavenArtifactDependency implements ArtifactDependency {
	private MavenArtifactDeclaration declaration;
	private Scope scope;
	private List<DependencyExclusion> exclusions;

	public MavenArtifactDependency() {
	}

	public MavenArtifactDependency(MavenArtifactDeclaration declaration, Scope scope, List<DependencyExclusion> exclusions) {
		this.declaration = declaration;
		this.scope = scope;
		this.exclusions = exclusions;
	}

	@Override
	public MavenArtifactDeclaration getDeclaration() {
		return this.declaration;
	}

	public void setDeclaration(MavenArtifactDeclaration declaration) {
		this.declaration = declaration;
	}

	@Override
	public Scope getScope() {
		return this.scope;
	}

	@Override
	public List<DependencyExclusion> getExclusions() {
		return this.exclusions;
	}
}
