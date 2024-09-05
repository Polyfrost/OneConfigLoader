package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.polyfrost.oneconfig.loader.stage1.dependency.ArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class MavenArtifactManager implements ArtifactManager<MavenArtifact, MavenArtifactDeclaration> {
	private final XDG.ApplicationStore store;
	private final URI repository;
	private final RequestHelper requestHelper;
	private final CachingSolution cache;

	private final DocumentBuilderFactory documentBuilderFactory;

	public MavenArtifactManager(XDG.ApplicationStore store, URI repository, RequestHelper requestHelper) {
		this.store = store;
		this.repository = repository;
		System.out.println("Repository: " + repository);
		System.out.println("Snapshots: " + repository.resolve("test"));
		this.requestHelper = requestHelper;
		this.cache = new MavenCachingSolution(store, repository, requestHelper);
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			this.documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Something went wrong while setting secure processing... what", e);
		}
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
	public MavenArtifactDeclaration buildArtifactDeclaration(Map<String, String> elements) {
		return new MavenArtifactDeclaration(
				elements.get("groupId"),
				elements.get("artifactId"),
				elements.get("version"),
				elements.get("classifier"),
				elements.get("extension")
		);
	}

	@Override
	public MavenArtifact resolveArtifact(MavenArtifactDeclaration declaration) throws Exception {
		Path dataDir = this.store.getDataDir();
		Path localLibraries = dataDir.resolve("libraries");

		if (declaration.getVersion().equals("+")) {
			resolveLatestVersion(declaration);
		} else {
			declaration.setActualVersion(declaration.getVersion());
		}

		Path artifactRelativePath = declaration.getRelativePath();
		Path localArtifactPath = localLibraries.resolve(artifactRelativePath);

		if (!localArtifactPath.toFile().exists()) {
			URI remoteArtifact = this.repository.resolve(artifactRelativePath.toString().replace('\\', '/'));
			System.out.println("Remote Artifact: " + remoteArtifact);

			try (InputStream inputStream = this.requestHelper.establishConnection(remoteArtifact.toURL()).getInputStream()) {
				// Ensure parent directories exist
				Files.createDirectories(localArtifactPath.getParent());

				Files.copy(inputStream, localArtifactPath);
			}
		}

		try (InputStream inputStream = Files.newInputStream(localArtifactPath)) {
			System.out.println("Local Artifact Path: " + localArtifactPath);
			List<MavenArtifactDependency> dependencyList = this.getDependency(inputStream)
					.stream()
					.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
					.collect(Collectors.toList());
			return new MavenArtifact(declaration, dependencyList);
		}
	}

	private void resolveLatestVersion(MavenArtifactDeclaration declaration) {
		String[] paths = new String[]{
				"maven-metadata-local.xml",
				"maven-metadata.xml",
		};
		for (String path : paths) {
			Path metadataPath = Paths.get(declaration.getGroupId().replace('.', '/'), declaration.getArtifactId(), path);
			URI metadataUri = this.repository.resolve(metadataPath.toString().replace('\\', '/'));
			System.out.println("Metadata URI: " + metadataUri);
			try (InputStream inputStream = this.requestHelper.establishConnection(metadataUri.toURL()).getInputStream()) {
				String latestVersion = this.getLatestVersion(inputStream);
				System.out.println("Latest Version: " + latestVersion);
				if (latestVersion != null) {
					declaration.setActualVersion(latestVersion);
					return;
				}
			} catch (IOException | ParserConfigurationException | SAXException e) {
				// Ignore if this is a FileNotFoundException since we check multiple paths
				if (e instanceof FileNotFoundException) {
					continue;
				}

				throw new RuntimeException("Error while parsing the latest version of " + declaration.getDeclaration(), e);
			}
		}
		throw new RuntimeException("Could not resolve latest version of " + declaration.getDeclaration());
	}

	@Override
	public InputStream createArtifactInputStream(MavenArtifact artifact) throws IOException {
		URI resolved = this.repository.resolve(artifact.getDeclaration().getRelativePath().toString().replace('\\', '/'));
		System.out.println("Resolved: " + resolved);

		if (!resolved.isAbsolute()) {
			throw new RuntimeException("Could not createArtifactInputStream from " + resolved);
		}

		URLConnection connection = this.requestHelper.establishConnection(resolved.toURL());
		return connection.getInputStream();
	}

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public Class<MavenArtifactDeclaration> getArtifactDeclarationClass() {
		return MavenArtifactDeclaration.class;
	}

	private String getLatestVersion(InputStream mavenMetadata) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(mavenMetadata);
		Element documentElement = document.getDocumentElement();
		Element versioning = (Element) documentElement.getElementsByTagName("versioning").item(0);
		Element latest = (Element) versioning.getElementsByTagName("latest").item(0);
		return latest.getTextContent();
	}

	private List<MavenArtifactDeclaration> getDependency(InputStream pom) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(pom);
		Element documentElement = document.getDocumentElement();
		Element dependencies = (Element) documentElement.getElementsByTagName("dependencies").item(0);
		NodeList dependencyList = dependencies.getElementsByTagName("dependency");

		List<MavenArtifactDeclaration> list = new ArrayList<>();
		for (int i = 0; i < dependencyList.getLength(); i++) {
			Element dependency = (Element) dependencyList.item(i);

			String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
			String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
			String version = dependency.getElementsByTagName("version").item(0).getTextContent();

			list.add(
					new MavenArtifactDeclaration(
							groupId,
							artifactId,
							version,
							null,
							".jar"
					)
			);
		}
		return list;
	}
}
