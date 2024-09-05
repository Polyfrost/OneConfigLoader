package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.util.List;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class MavenArtifact implements Artifact<MavenArtifactDeclaration, MavenArtifactDependency> {
    private final MavenArtifactDeclaration declaration;
    private final List<MavenArtifactDependency> dependencies;

	public MavenArtifact(MavenArtifactDeclaration declaration, List<MavenArtifactDependency> dependencies) {
		this.declaration = declaration;
		this.dependencies = dependencies;
	}

	@Override
	public MavenArtifactDeclaration getDeclaration() {
		return this.declaration;
	}

	@Override
	public List<MavenArtifactDependency> getDependencies() {
		return this.dependencies;
	}
}
