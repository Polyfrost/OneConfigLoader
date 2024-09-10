package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import cc.polyfrost.polyio.util.PolyHashing;
import lombok.extern.log4j.Log4j2;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactResolver;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;
import org.polyfrost.oneconfig.loader.stage1.dependency.utils.FileUtils;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
public class MavenArtifactResolver implements ArtifactResolver<MavenArtifact, MavenArtifactDeclaration> {
	private static final String[] MAVEN_METADATA_NAMES = new String[] {
			"maven-metadata-local.xml",
			"maven-metadata.xml",
	};

	private final XDG.ApplicationStore store;
	private final RequestHelper requestHelper;
	private final URI[] repositories;

	private final DocumentBuilderFactory documentBuilderFactory;

	public MavenArtifactResolver(XDG.ApplicationStore store, RequestHelper requestHelper, URI[] repositories) {
		this.store = store;
		this.requestHelper = requestHelper;
		this.repositories = repositories;

		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();

		try {
			this.documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Something went wrong while setting secure processing... What???", e);
		}
	}

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

		String sha1Path = artifactRelativePath + ".sha1";

		String rawPomPath = artifactRelativePath.toString().replace(declaration.getExtension(), "pom");
		Path pomPath = localLibraries.resolve(rawPomPath);

		boolean resolved = false;
		long startTime = System.currentTimeMillis();
		for (URI repository : repositories) {
			if (resolved) {
				break;
			}

			URI remoteSha1 = repository.resolve(FileUtils.encodePath(sha1Path.replace('\\', '/')));
			long sha1StartTime = System.currentTimeMillis();
			try (InputStream inputStream = this.requestHelper.establishConnection(remoteSha1.toURL()).getInputStream()) {
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

				log.info("Resolved SHA1 for {} in {}ms", declaration.getDeclaration(), System.currentTimeMillis() - sha1StartTime);
				String sha1 = byteStream.toString().trim();
				if (sha1.isEmpty()) {
					continue;
				}

				Pattern validSha1Pattern = Pattern.compile("[a-fA-F0-9]{40}");
				if (!validSha1Pattern.matcher(sha1).matches()) {
					continue;
				}

				if (localArtifactPath.toFile().exists()) {
					long sha1CheckStartTime = System.currentTimeMillis();
					String artifactSha1 = this.getHash(localArtifactPath);
					if (sha1.equals(artifactSha1)) {
						log.info("{} matched SHA1 in {}, resolving", localArtifactPath, System.currentTimeMillis() - sha1CheckStartTime);
						long pomStartTime = System.currentTimeMillis();
						try (InputStream pomStream = Files.newInputStream(pomPath)) {
							log.info("Checked SHA1 for artifact {} in {}ms", declaration.getDeclaration(), System.currentTimeMillis() - pomStartTime);
							List<MavenArtifactDependency> dependencyList = this.getDependencies(declaration, pomStream)
									.stream()
									.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
									.collect(Collectors.toList());
							log.info("Resolved CACHED artifact {} in {}ms", declaration.getDeclaration(), System.currentTimeMillis() - startTime);
							resolved = true;
							return new MavenArtifact(declaration, dependencyList);
						} catch (Throwable t) {
							// If we can't find the dependencies, we can't find the artifact
							t.printStackTrace();
						}
					} else {
						log.info("{} does not match SHA1, updating", localArtifactPath);
					}
				} else {
					log.info("{} does not exist, downloading", localArtifactPath);
				}
			} catch (Throwable t) {
				// Ignore if this is a FileNotFoundException since we check multiple paths
				if (t instanceof FileNotFoundException) {
					continue;
				}

				throw new RuntimeException("Error while checking SHA1 of " + declaration.getDeclaration(), t);
			}

			if (!pomPath.toFile().exists()) {
				URI remotePom = repository.resolve(FileUtils.encodePath(rawPomPath.replace('\\', '/')));

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
				URI remoteArtifact = repository.resolve(FileUtils.encodePath(artifactRelativePath.toString().replace('\\', '/')));

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
				List<MavenArtifactDependency> dependencyList = this.getDependencies(declaration, inputStream)
						.stream()
						.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
						.collect(Collectors.toList());
				log.info("Resolved artifact {} in {}ms", declaration.getDeclaration(), System.currentTimeMillis() - startTime);
				resolved = true;
				return new MavenArtifact(declaration, dependencyList);
			} catch (Throwable t) {
				// If we can't find the dependencies, we can't find the artifact
				t.printStackTrace();
			}
		}

		log.info("Could not resolve artifact {} in {}ms", declaration.getDeclaration(), System.currentTimeMillis() - startTime);
		return null;
	}

	private String getHash(Path path) {
		long startTime = System.currentTimeMillis();
		try {
			byte[] bytes = Files.readAllBytes(path);
			byte[] hashed = PolyHashing.hash(bytes, PolyHashing.SHA1);
			BigInteger bigInt = new BigInteger(1, hashed);
			String hash = bigInt.toString(16);
			log.info("Hashed {} in {}ms", path, System.currentTimeMillis() - startTime);
			return hash;
		} catch (Throwable t) {
			throw new RuntimeException("Error while hashing " + path, t);
		}
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

	private List<MavenArtifactDeclaration> getDependencies(ArtifactDeclaration declaration, InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
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
			String pomContent = byteStream.toString();
			if (pomContent.contains("<html")) {
				log.warn("Skipping POM returned for {} (received HTML)", declaration.getDeclaration());
				return new ArrayList<>();
			}

			InputStream pomStream = new ByteArrayInputStream(byteStream.toByteArray());

			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(pomStream);
			Element documentElement = document.getDocumentElement();

			// Skip HTML files
			if (document.getElementsByTagName("html").getLength() > 0) {
				log.warn("Skipping POM returned for {} (received HTML)", declaration.getDeclaration());
				return new ArrayList<>();
			}

			Element dependencies = (Element) documentElement.getElementsByTagName("dependencies").item(0);
			if (dependencies == null) {
				return new ArrayList<>();
			}

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
								"jar"
						)
				);
			}

			return list;
		} catch (Throwable t) {
			log.error("Error while parsing POM file", t);

			// Print out the POM file using log4j2
			StringBuilder builder = new StringBuilder();
			try (InputStream pomStream = new ByteArrayInputStream(byteStream.toByteArray())) {
				byte[] buffer = new byte[1024];
				int read;
				while ((read = pomStream.read(buffer)) != -1) {
					builder.append(new String(buffer, 0, read));
				}
			}

			log.error("POM file:\n{}", builder.toString());
			return new ArrayList<>();
		}
	}
}
