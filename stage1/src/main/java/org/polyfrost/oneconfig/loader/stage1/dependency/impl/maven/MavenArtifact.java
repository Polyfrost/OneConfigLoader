package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.util.List;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class MavenArtifact implements Artifact {
    private final ArtifactDeclaration declaration;
    private final List<ArtifactDependency> dependencies;

	public MavenArtifact(ArtifactDeclaration declaration, List<ArtifactDependency> dependencies) {
		this.declaration = declaration;
		this.dependencies = dependencies;
	}

	@Override
	public ArtifactDeclaration getDeclaration() {
		return this.declaration;
	}

	@Override
	public List<ArtifactDependency> getDependencies() {
		return this.dependencies;
	}
}
