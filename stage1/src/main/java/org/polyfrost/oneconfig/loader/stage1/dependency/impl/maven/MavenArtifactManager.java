package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.jetbrains.annotations.Nullable;

import org.polyfrost.oneconfig.loader.stage1.dependency.ArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

/**
 * @author xtrm
 * @author Deftu
 * @since 1.1.0
 */
@Getter
@Log4j2
public class MavenArtifactManager implements ArtifactManager<MavenArtifact, MavenArtifactDeclaration, MavenArtifactResolver> {
	private final XDG.ApplicationStore store;
	private final URI[] repositories;
	private final RequestHelper requestHelper;
	private final MavenArtifactResolver resolver;
	private final CachingSolution cache;

	public MavenArtifactManager(XDG.ApplicationStore store, @Nullable Path readOnlyLibraryCache, RequestHelper requestHelper, URI... repositories) {
		this.store = store;
		this.requestHelper = requestHelper;
		this.repositories = repositories;
		this.resolver = new MavenArtifactResolver(store, readOnlyLibraryCache, requestHelper, repositories);
		this.cache = new MavenCachingSolution(store, repositories, requestHelper);
	}

	public MavenArtifactResolver getArtifactResolver() {
		return this.resolver;
	}

	@Override
	public MavenArtifactDeclaration buildArtifactDeclaration(String stringDeclaration) {
		String[] artifactParts = stringDeclaration.split(Pattern.quote(":"));

		if (artifactParts.length < 3) {
			throw new IllegalArgumentException("Invalid artifact declaration (missing parts): " + stringDeclaration);
		}

		// Those are obvious
		String groupId = artifactParts[0];
		String artifactId = artifactParts[1];
		String version = artifactParts[2];
		// These are optional
		String classifier = null;
		String extension = "jar";

		lookup:
		{
			// Get the classifier if it exists
			if (artifactParts.length > 3) {
				classifier = artifactParts[3];
				// If we detect an extension too (f.ex: @zip), let's fill it in & fix the classifier
				int index;
				if ((index = classifier.indexOf('@')) != -1) {
					extension = classifier.substring(index + 1);
					classifier = classifier.substring(0, index);
					break lookup;
				}
			}

			// Finally, check if only the extension is declared
			if (stringDeclaration.contains("@")) {
				extension = stringDeclaration.substring(stringDeclaration.indexOf("@") + 1);
				// Simple check to make sure the extension is at the end
				String finalPart = "@" + extension;
				if (!stringDeclaration.endsWith(finalPart)) {
					throw new IllegalArgumentException("Invalid artifact declaration (invalid extension specifier): " + stringDeclaration);
				}
			}
		}

		return new MavenArtifactDeclaration(groupId, artifactId, version, classifier, extension);
	}

	@Override
	public InputStream createArtifactInputStream(MavenArtifact artifact) {
		Path dataDir = this.store.getDataDir();
		Path localLibraries = dataDir.resolve("libraries");
		Path artifactRelativePath = artifact.getDeclaration().getRelativePath();
		Path localArtifactPath = localLibraries.resolve(artifactRelativePath);

		if (Files.exists(localArtifactPath)) {
			try {
				return Files.newInputStream(localArtifactPath);
			} catch (IOException e) {
				log.error("Error while creating input stream for {}", artifact.getDeclaration(), e);
			}
		}

		return null;
	}

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public Class<MavenArtifactDeclaration> getArtifactDeclarationClass() {
		return MavenArtifactDeclaration.class;
	}
}
