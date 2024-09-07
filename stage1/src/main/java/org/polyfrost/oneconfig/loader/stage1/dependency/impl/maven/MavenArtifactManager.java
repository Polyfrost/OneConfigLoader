package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import lombok.extern.log4j.Log4j2;
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
@Log4j2
public class MavenArtifactManager implements ArtifactManager<MavenArtifact, MavenArtifactDeclaration> {

	private static final String[] MAVEN_METADATA_NAMES = new String[] {
			"maven-metadata-local.xml",
			"maven-metadata.xml",
	};

	private final XDG.ApplicationStore store;
	private final URI[] repositories;
	private final RequestHelper requestHelper;
	private final CachingSolution cache;

	private final DocumentBuilderFactory documentBuilderFactory;

	public MavenArtifactManager(XDG.ApplicationStore store, RequestHelper requestHelper, URI... repositories) {
		this.store = store;
		this.requestHelper = requestHelper;
		this.repositories = repositories;
		this.cache = new MavenCachingSolution(store, this.repositories, requestHelper);
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
		} else if (declaration.getVersion().endsWith("-SNAPSHOT")) {
			log.warn("Resolving snapshot version for {}", declaration.getDeclaration());
			resolveSnapshotVersion(declaration);
		} else {
			declaration.setActualVersion(declaration.getVersion());
		}

		Path artifactRelativePath = declaration.getRelativePath();
		Path localArtifactPath = localLibraries.resolve(artifactRelativePath);

		String rawPomPath = artifactRelativePath.toString().replace(declaration.getExtension(), "pom");
		Path pomPath = localLibraries.resolve(rawPomPath);
		for (URI repository : repositories) {
			if (!pomPath.toFile().exists()) {
				URI remotePom = repository.resolve(rawPomPath.replace('\\', '/'));

				try (InputStream inputStream = this.requestHelper.establishConnection(remotePom.toURL()).getInputStream()) {
					// Ensure parent directories exist
					Files.createDirectories(pomPath.getParent());

					Files.copy(inputStream, pomPath);
				} catch (Throwable t) {
					// If we can't find the POM, we can't find the artifact

					t.printStackTrace();

					continue;
				}
			}

			if (!localArtifactPath.toFile().exists()) {
				URI remoteArtifact = repository.resolve(artifactRelativePath.toString().replace('\\', '/'));

				try (InputStream inputStream = this.requestHelper.establishConnection(remoteArtifact.toURL()).getInputStream()) {
					// Ensure parent directories exist
					Files.createDirectories(localArtifactPath.getParent());

					Files.copy(inputStream, localArtifactPath);
				} catch (Throwable t) {
					// If we can't find the artifact, this might be a BOM or something. Simply warn and move on
					log.warn("Could not find artifact {} at {}", declaration.getDeclaration(), remoteArtifact);
				}
			}

			try (InputStream inputStream = Files.newInputStream(pomPath)) {
				List<MavenArtifactDependency> dependencyList = this.getDependencies(inputStream)
						.stream()
						.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
						.collect(Collectors.toList());
				return new MavenArtifact(declaration, dependencyList);
			} catch (Throwable t) {
				// If we can't find the dependencies, we can't find the artifact
				t.printStackTrace();
			}
		}

		throw new RuntimeException("Could not resolve artifact " + declaration.getDeclaration());
	}

	private String getLatestVersion(InputStream mavenMetadata) throws ParserConfigurationException, IOException, SAXException {
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(mavenMetadata);
			Element documentElement = document.getDocumentElement();
			Element versioning = (Element) documentElement.getElementsByTagName("versioning").item(0);
			if (versioning == null) {
				return null;
			}

			Element latest = (Element) versioning.getElementsByTagName("latest").item(0);
			if (latest == null) {
				return null;
			}

			return latest.getTextContent();
		} catch (Throwable t) {
			return null;
		}
	}

	private String getSnapshotVersion(InputStream mavenMetadata) throws ParserConfigurationException, IOException, SAXException {
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(mavenMetadata);
			Element versioning = (Element) document.getElementsByTagName("versioning").item(0);
			if (versioning == null) {
				return null;
			}

			Element snapshot = (Element) versioning.getElementsByTagName("snapshot").item(0);
			if (snapshot == null) {
				return null;
			}

			Element timestamp = (Element) snapshot.getElementsByTagName("timestamp").item(0);
			if (timestamp == null) {
				return null;
			}

			Element buildNumber = (Element) snapshot.getElementsByTagName("buildNumber").item(0);
			if (buildNumber == null) {
				return null;
			}

			return timestamp.getTextContent() + "-" + buildNumber.getTextContent();
		} catch (Throwable t) {
			return null;
		}
	}

	private void resolveLatestVersion(MavenArtifactDeclaration declaration) {
		for (URI repository : repositories) {
			for (String path : MAVEN_METADATA_NAMES) {
				Path metadataPath = Paths.get(declaration.getGroupId().replace('.', '/'), declaration.getArtifactId(), path);
				URI metadataUri = repository.resolve(metadataPath.toString().replace('\\', '/'));
				try (InputStream inputStream = this.requestHelper.establishConnection(metadataUri.toURL()).getInputStream()) {
					String latestVersion = this.getLatestVersion(inputStream);
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
		}

		throw new RuntimeException("Could not resolve latest version of " + declaration.getDeclaration());
	}

	private void resolveSnapshotVersion(MavenArtifactDeclaration declaration) {
		for (URI repository : repositories) {
			for (String path : MAVEN_METADATA_NAMES) {
				Path metadataPath = Paths.get(declaration.getGroupId().replace('.', '/'), declaration.getArtifactId(), declaration.getVersion(), path);
				URI metadataUri = repository.resolve(metadataPath.toString().replace('\\', '/'));
				try (InputStream inputStream = this.requestHelper.establishConnection(metadataUri.toURL()).getInputStream()) {
					log.error(metadataUri);
					String snapshotVersion = this.getSnapshotVersion(inputStream);
					log.warn("Snapshot version {} in {} @ {}", snapshotVersion, path, repository);
					if (snapshotVersion != null) {
						declaration.setSnapshot(true);
						declaration.setSnapshotVersion(declaration.getVersion().replace("SNAPSHOT", snapshotVersion));

						return;
					}
				} catch (IOException | ParserConfigurationException | SAXException e) {
					// Ignore if this is a FileNotFoundException since we check multiple paths
					if (e instanceof FileNotFoundException) {
						continue;
					}

					throw new RuntimeException("Error while parsing the snapshot version of " + declaration.getDeclaration(), e);
				}
			}
		}

		throw new RuntimeException("Could not resolve snapshot version of " + declaration.getDeclaration());
	}

	@Override
	public InputStream createArtifactInputStream(MavenArtifact artifact) {
		for (URI repository : repositories) {
			try {
				URI resolved = repository.resolve(artifact.getDeclaration().getRelativePath().toString().replace('\\', '/'));
				if (!resolved.isAbsolute()) {
					throw new RuntimeException("Could not createArtifactInputStream from " + resolved);
				}

				URLConnection connection = this.requestHelper.establishConnection(resolved.toURL());
				return connection.getInputStream();
			} catch (IOException e) {
				// Ignore if this is a FileNotFoundException since we check multiple paths
				if (e instanceof FileNotFoundException) {
					continue;
				}

				throw new RuntimeException("Error while creating input stream for " + artifact.getDeclaration(), e);
			}
		}

		throw new RuntimeException("Could not create input stream for " + artifact.getDeclaration());
	}

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public Class<MavenArtifactDeclaration> getArtifactDeclarationClass() {
		return MavenArtifactDeclaration.class;
	}

	private List<MavenArtifactDeclaration> getDependencies(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		{
			byte[] buffer = new byte[1024];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				byteStream.write(buffer, 0, read);
			}

			byteStream.flush();
			inputStream.close();
		}

		try {
			InputStream pomStream = new ByteArrayInputStream(byteStream.toByteArray());

			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(pomStream);
			Element documentElement = document.getDocumentElement();
			Element dependencies = (Element) documentElement.getElementsByTagName("dependencies").item(0);
			if (dependencies == null) {
				return new ArrayList<>();
			}

			NodeList dependencyList = dependencies.getElementsByTagName("dependency");
			log.error("Found {} dependencies:", dependencyList.getLength());

			List<MavenArtifactDeclaration> list = new ArrayList<>();
			for (int i = 0; i < dependencyList.getLength(); i++) {
				Element dependency = (Element) dependencyList.item(i);
				log.error("- {}", dependency.getElementsByTagName("artifactId").item(0).getTextContent());

				String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
				String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
				String version = dependency.getElementsByTagName("version").item(0).getTextContent();

				list.add(
						new MavenArtifactDeclaration(
								groupId,
								artifactId,
								version,
								null,
								"jar"
						)
				);
			}

			return list;
		} catch (Throwable t) {
			t.printStackTrace();

			// Print out the POM file using log4j2
			try (InputStream pomStream = new ByteArrayInputStream(byteStream.toByteArray())) {
				byte[] buffer = new byte[1024];
				int read;
				while ((read = pomStream.read(buffer)) != -1) {
					log.error(new String(buffer, 0, read));
				}
			}

			throw t;
		}
	}
}
