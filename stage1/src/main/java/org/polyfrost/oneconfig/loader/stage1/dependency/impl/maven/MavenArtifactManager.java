package org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.stage1.dependency.ArtifactManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.impl.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Scope;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public MavenArtifactManager(XDG.ApplicationStore store, URI repository, RequestHelper requestHelper) {
        this.store = store;
        this.repository = repository;
        this.requestHelper = requestHelper;
        this.cache = new MavenCachingSolution(requestHelper, store, repository);
    }

	public MavenArtifactDeclaration buildArtifactDeclaration(String stringDeclaration) {
		String[] artifactParts = stringDeclaration.split(":");

		if (artifactParts.length < 3) {
			throw new IllegalArgumentException("Invalid artifact declaration: " + stringDeclaration);
		}

		String groupId = artifactParts[0];
		String artifactId = artifactParts[1];
		String version = artifactParts[2];
		String classifier = null;
		String extension = "jar";

		// Get the classifier if it exists
		if (artifactParts.length > 3) {
			classifier = artifactParts[3];
		}

		// Finally, check if an extension is declared explicitly (f.ex: @zip)
		if (stringDeclaration.contains("@")) {
			extension = stringDeclaration.substring(stringDeclaration.indexOf("@") + 1);
		}

		return new MavenArtifactDeclaration(groupId, artifactId, version, classifier, extension);
	}

	public MavenArtifactDeclaration buildArtifactDeclaration(Map<String, String> elements) {
		return new MavenArtifactDeclaration(
				elements.get("groupId"),
				elements.get("artifactId"),
				elements.get("version"),
				elements.get("classifier"),
				elements.get("extension")
		);
	}

	public MavenArtifact resolveArtifact(MavenArtifactDeclaration declaration) throws Exception {
		Path dataDir = this.store.getDataDir();
		Path localLibraries = dataDir.resolve("libraries");

		Path artifactRelativePath = declaration.getRelativePath();
		Path localArtifactPath = localLibraries.resolve(artifactRelativePath);

		if (!localArtifactPath.toFile().exists()) {
			URI remoteArtifact = this.repository.resolve(artifactRelativePath.toString().replace('\\', '/'));

			try (InputStream inputStream = this.requestHelper.establishConnection(remoteArtifact.toURL()).getInputStream()) {
				Files.copy(inputStream, localArtifactPath);
			}
		}

		List<MavenArtifactDependency> dependencyList = this.getDependency(Files.newInputStream(localArtifactPath))
				.stream()
				.map((mavenArtifactDeclaration) -> new MavenArtifactDependency(mavenArtifactDeclaration, Scope.RUNTIME, new ArrayList<>()))
				.collect(Collectors.toList());
		return new MavenArtifact(declaration, dependencyList);
	}

	public InputStream createArtifactInputStream(MavenArtifact artifact) throws IOException {
		URI resolved = this.repository.resolve(artifact.getDeclaration().getRelativePath().toString());

		if (!resolved.isAbsolute()) {
			throw new RuntimeException("Could not createArtifactInputStream from " + resolved);
		}

		URLConnection connection = this.requestHelper.establishConnection(resolved.toURL());
		return connection.getInputStream();
	}

	//	public MavenArtifactDeclaration buildArtifactDeclaration(String groupId, String artifactId, String version) {
//        return this.buildArtifactDeclaration(groupId, artifactId, version, null, "jar");
//    }
//
//    @Override
//    public MavenArtifactDeclaration buildArtifactDeclaration(String groupId, String artifactId, String version, String classifier, String extension) {
//        return new MavenArtifactDeclaration(groupId, artifactId, version, classifier, extension);
//    }
//
//    @Override
//    public MavenArtifact resolveArtifact(MavenArtifactDeclaration artifact) throws IOException, ParserConfigurationException, SAXException {
//        Path dataDir = this.store.getDataDir();
//        Path localLibraries = dataDir.resolve("libraries");
//
//        Path artifactRelativePath = artifact.getRelativePath();
//
//        String pomPath = artifactRelativePath.toString().replace(artifact.getExtension(), "pom");
//        Path localPomPath = localLibraries.resolve(pomPath);
//        if (!localPomPath.toFile().exists()) {
//            URI remotePom = this.repository.resolve(pomPath);
//
//            try (InputStream inputStream = this.requestHelper.establishConnection(remotePom.toURL()).getInputStream()) {
//                Files.copy(inputStream, localPomPath);
//            }
//        }
//
//        List<MavenArtifactDependency> dependencyList = this.getDependency(Files.newInputStream(localPomPath))
//                .stream()
//                .map((mavenArtifact) -> {
//                    MavenArtifactDependency mavenArtifactDependency = new MavenArtifactDependency();
//                    mavenArtifactDependency.setDeclaration(new MavenArtifact(mavenArtifact, new ArrayList<>()));
//                    return mavenArtifactDependency;
//                })
//                .collect(Collectors.toList());
//        return new MavenArtifact(artifact, dependencyList);
//    }
//
//	@Override
//    public InputStream createArtifactInputStream(MavenArtifactDeclaration mavenArtifact) throws IOException {
//        URI resolved = repository.resolve(mavenArtifact.getDeclaration());
//
//        if (!resolved.isAbsolute()) {
//            throw new RuntimeException("Could not createArtifactInputStream from " + resolved);
//        }
//
//        URLConnection connection = this.requestHelper.establishConnection(resolved.toURL());
//        return connection.getInputStream();
//    }

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public Class<MavenArtifactDeclaration> getArtifactDeclarationClass() {
		return MavenArtifactDeclaration.class;
	}

	private List<MavenArtifactDeclaration> getDependency(InputStream pom) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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

        pom.close();
        return list;
    }
}
