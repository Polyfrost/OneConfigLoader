package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cc.polyfrost.polyio.util.PolyHashing;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDeclaration;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactResolver;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;
import org.polyfrost.oneconfig.loader.stage1.dependency.utils.FileUtils;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

@Log4j2
public class MavenArtifactResolver implements ArtifactResolver<MavenArtifact, MavenArtifactDeclaration> {
	private static final String[] MAVEN_METADATA_NAMES = new String[]{
			"maven-metadata.xml",
			"maven-metadata-local.xml",
	};

	private static final ExecutorService REPOSITORY_DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
			log.warn("Resolving latest version for {}", declaration.getDeclaration());
			long startTime = System.currentTimeMillis();
			resolveLatestVersion(declaration);
			long endTime = System.currentTimeMillis();
			log.info("Resolved latest version for {} (took {}ms)", declaration.getDeclaration(), endTime - startTime);
		} else if (declaration.getVersion().endsWith("-SNAPSHOT")) {
			log.warn("Resolving snapshot version for {}", declaration.getDeclaration());
			long startTime = System.currentTimeMillis();
			resolveSnapshotVersion(declaration);
			long endTime = System.currentTimeMillis();
			log.info("Resolved snapshot version for {} (took {}ms)", declaration.getDeclaration(), endTime - startTime);
		} else {
			declaration.setActualVersion(declaration.getVersion());
		}

		Path artifactRelativePath = declaration.getRelativePath();
		Path localArtifactPath = localLibraries.resolve(artifactRelativePath);
		String sha1Path = artifactRelativePath + ".sha1";
		String rawPomPath = artifactRelativePath.toString().replace(declaration.getExtension(), "pom");
		Path pomPath = localLibraries.resolve(rawPomPath);

		List<CompletableFuture<MavenArtifact>> futures = Arrays.stream(repositories)
				.map(repository -> CompletableFuture.supplyAsync(() -> {
					try {
						return resolveArtifactFromRepository(repository, declaration, localArtifactPath, artifactRelativePath, sha1Path, pomPath, rawPomPath);
					} catch (Throwable t) {
						return null;
					}
				})).collect(Collectors.toList());

		return resolveFirstNonNull(futures).get();
	}

	private CompletableFuture<MavenArtifact> resolveFirstNonNull(List<CompletableFuture<MavenArtifact>> futures) {
		return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
				.thenCompose(result -> {
					if (result instanceof MavenArtifact) {
						return CompletableFuture.completedFuture((MavenArtifact) result); // Return first non-null result
					} else {
						// Remove the completed future and continue with the rest
						List<CompletableFuture<MavenArtifact>> remainingFutures = futures.stream()
								.filter(future -> !future.isDone() || future.join() != null) // Keep only unfinished or non-null futures
								.collect(Collectors.toList());

						if (remainingFutures.isEmpty()) {
							return CompletableFuture.completedFuture(null); // All futures are done or null
						}

						// Recurse with the remaining futures
						return resolveFirstNonNull(remainingFutures);
					}
				});
	}

	private MavenArtifact resolveArtifactFromRepository(
			URI repository,
			MavenArtifactDeclaration declaration,
			Path localArtifactPath,
			Path artifactRelativePath,
			String sha1Path,
			Path pomPath,
			String rawPomPath
	) {
		long startTime = System.currentTimeMillis();
		if (declaration.isShouldValidate()) {
			URI remoteSha1 = repository.resolve(FileUtils.encodePath(sha1Path.replace('\\', '/')));
			try {
				URLConnection connection = this.requestHelper.establishConnection(remoteSha1.toURL());
				ByteArrayOutputStream byteStream = this.requestHelper.consumeConnection(connection);
				String sha1 = byteStream.toString().trim();
				byteStream.close();
				if (sha1.isEmpty()) {
					log.warn("Did NOT receive SHA1 (repository: {}, took {}ms)", repository, System.currentTimeMillis() - startTime);
					return null;
				}

				Pattern validSha1Pattern = Pattern.compile("[a-fA-F0-9]{40}");
				if (!validSha1Pattern.matcher(sha1).matches()) {
					log.warn("Invalid SHA1 (repository: {}, took {}ms): {}", repository, System.currentTimeMillis() - startTime, sha1);
					return null;
				}

				if (localArtifactPath.toFile().exists()) {
					String artifactSha1 = getHash(localArtifactPath);
					if (sha1.equals(artifactSha1)) {
						log.info("SHA1 matches for {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
						return loadArtifact(declaration, pomPath); // Load from local
					} else {
						if (!pomPath.toFile().exists()) {
							downloadPom(repository, rawPomPath, pomPath);
						}

						if (downloadArtifact(repository, artifactRelativePath.toString(), localArtifactPath)) {
							log.info("Resolved (updated) artifact {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
							return loadArtifact(declaration, pomPath);
						}
					}
				} else {
					if (!pomPath.toFile().exists()) {
						downloadPom(repository, rawPomPath, pomPath);
					}

					if (downloadArtifact(repository, artifactRelativePath.toString(), localArtifactPath)) {
						log.info("Resolved (validated + downloaded) artifact {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
						return loadArtifact(declaration, pomPath);
					}
				}

				if (!pomPath.toFile().exists()) {
					downloadPom(repository, rawPomPath, pomPath);
				}

				log.info("Resolved artifact {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
				return loadArtifact(declaration, pomPath);
			} catch (FileNotFoundException e) {
				return null; // Ignore and continue
			} catch (Throwable t) {
				throw new RuntimeException("Error while checking SHA1 of " + declaration.getDeclaration(), t);
			}
		}

		try {
			if (!pomPath.toFile().exists()) {
				downloadPom(repository, rawPomPath, pomPath);
			}
		} catch (Throwable t) {
			if (t instanceof FileNotFoundException) {
				return null; // Ignore
			}

			throw new RuntimeException("Error while resolving " + declaration.getDeclaration(), t);
		}

		try {
			if (localArtifactPath.toFile().exists()) {
				log.warn("Resolved (local) artifact {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
				return loadArtifact(declaration, pomPath); // Load from local
			} else {
				if (downloadArtifact(repository, artifactRelativePath.toString(), localArtifactPath)) {
					log.info("Resolved (downloaded) artifact {} (repository: {}, took {}ms)", declaration.getDeclaration(), repository, System.currentTimeMillis() - startTime);
					return loadArtifact(declaration, pomPath);
				}
			}
		} catch (Throwable t) {
			if (t instanceof FileNotFoundException) {
				return null; // Ignore
			}

			throw new RuntimeException("Error while resolving " + declaration.getDeclaration(), t);
		}

		log.warn("Could not resolve artifact {}", declaration.getDeclaration());
		return null;
	}

	private MavenArtifact loadArtifact(MavenArtifactDeclaration declaration, Path pomPath) throws IOException, ParserConfigurationException, SAXException {
		log.info("Loading artifact {} with pom {}", declaration.getDeclaration(), pomPath);
		try (InputStream inputStream = Files.newInputStream(pomPath)) {
			List<MavenArtifactDependency> dependencyList = this.getDependencies(declaration, inputStream)
					.stream()
					.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
					.collect(Collectors.toList());
			return new MavenArtifact(declaration, dependencyList);
		}
	}

	private boolean downloadArtifact(URI repository, String artifactRelativePath, Path localArtifactPath) throws IOException {
		URI remoteArtifact = repository.resolve(FileUtils.encodePath(artifactRelativePath.replace('\\', '/')));
		try (InputStream inputStream = this.requestHelper.provideStream(this.requestHelper.establishConnection(remoteArtifact.toURL()))) {
			boolean checked = false;
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024 * 128];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				if (!checked) {
					// This is a ZIP file, which is the same as a JAR file
					if (read < 4 || buffer[0] != 0x50 || buffer[1] != 0x4B || buffer[2] != 0x03 || buffer[3] != 0x04) {
						log.warn("File at {} is not a JAR file", remoteArtifact);
						inputStream.close();
						byteStream.close();
						return false;
					}
					checked = true;
				}
				byteStream.write(buffer, 0, read);
			}
			byteStream.flush();

			// Double-check if the file is actually a JAR file
			byte[] bytes = byteStream.toByteArray();
			if (bytes.length > 4 && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
				Files.createDirectories(localArtifactPath.getParent());
				Files.write(localArtifactPath, bytes);
				return true;
			} else {
				return false;
			}
		}
	}

	private void downloadPom(URI repository, String rawPomPath, Path pomPath) throws IOException {
		URI remotePom = repository.resolve(FileUtils.encodePath(rawPomPath.replace('\\', '/')));
		URLConnection connection = this.requestHelper.establishConnection(remotePom.toURL());
		ByteArrayOutputStream byteStream = this.requestHelper.consumeConnection(connection);
		if (!byteStream.toString().contains("<html")) {
			Files.createDirectories(pomPath.getParent());
			Files.write(pomPath, byteStream.toByteArray());
		} else {
			log.error("Tried to download POM file from {} but it was HTML", remotePom);
			log.error("File contents:\n{}", byteStream.toString());
		}
		byteStream.close();
	}

	private String getHash(Path path) {
		try {
			byte[] bytes = Files.readAllBytes(path);
			byte[] hashed = PolyHashing.hash(bytes, PolyHashing.SHA1);
			BigInteger bigInt = new BigInteger(1, hashed);
			String hash = bigInt.toString(16);
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
			log.error("Error while parsing latest version", t);
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
			log.error("Error while parsing snapshot version", t);
			return null;
		}
	}

	private void resolveLatestVersion(MavenArtifactDeclaration declaration) {
		for (URI repository : repositories) {
			for (String path : MAVEN_METADATA_NAMES) {
				Path metadataPath = Paths.get(declaration.getGroupId().replace('.', '/'), declaration.getArtifactId(), path);
				URI metadataUri = repository.resolve(metadataPath.toString().replace('\\', '/'));
				try {
					URLConnection connection = this.requestHelper.establishConnection(metadataUri.toURL());
					InputStream inputStream = this.requestHelper.provideStream(connection);
					String latestVersion = this.getLatestVersion(inputStream);
					inputStream.close();
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
				try (InputStream inputStream = this.requestHelper.provideStream(this.requestHelper.establishConnection(metadataUri.toURL()))) {
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
				return new ArrayList<>(); // Ignore HTML files
			}

			InputStream pomStream = new ByteArrayInputStream(byteStream.toByteArray());

			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(pomStream);
			Element documentElement = document.getDocumentElement();

			if (document.getElementsByTagName("html").getLength() > 0) {
				return new ArrayList<>(); // Ignore HTML files
			}

			Element dependencies = (Element) documentElement.getElementsByTagName("dependencies").item(0);
			if (dependencies == null) {
				return new ArrayList<>();
			}

			NodeList dependencyList = dependencies.getElementsByTagName("dependency");
			List<MavenArtifactDeclaration> list = new ArrayList<>();
			for (int i = 0; i < dependencyList.getLength(); i++) {
				Element dependency = (Element) dependencyList.item(i);

				Element groupIdElement = (Element) dependency.getElementsByTagName("groupId").item(0);
				if (groupIdElement == null) {
					continue;
				}

				Element artifactIdElement = (Element) dependency.getElementsByTagName("artifactId").item(0);
				if (artifactIdElement == null) {
					continue;
				}

				Element versionElement = (Element) dependency.getElementsByTagName("version").item(0);
				if (versionElement == null) {
					continue;
				}

				Element scopeElement = (Element) dependency.getElementsByTagName("scope").item(0);
				if (scopeElement != null && "test".equalsIgnoreCase(scopeElement.getTextContent())) {
					continue;
				}

				list.add(
						new MavenArtifactDeclaration(
								groupIdElement.getTextContent(),
								artifactIdElement.getTextContent(),
								versionElement.getTextContent(),
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
