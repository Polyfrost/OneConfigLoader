package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.ArtifactDependency;
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
import java.util.stream.Collectors;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class MavenDependencyManager implements DependencyManager<MavenArtifact, MavenArtifactDeclaration> {
    private final XDG.ApplicationStore store;
    private final URI repository;
    private final RequestHelper requestHelper;
    private final CachingSolution cache;
    private final Class<MavenArtifact> artifactClass = MavenArtifact.class;
    private final Class<MavenArtifactDeclaration> artifactDeclarationClass = MavenArtifactDeclaration.class;

    public MavenDependencyManager(XDG.ApplicationStore store, URI repository, RequestHelper requestHelper) {
        this.store = store;
        this.repository = repository;
        this.requestHelper = requestHelper;
        this.cache = new MavenCachingSolution(requestHelper, store, repository);
    }

    public MavenArtifact buildArtifact(String groupId, String artifactId, String version) {
        return this.buildArtifact(groupId, artifactId, version, null, "jar");
    }

    @Override
    public MavenArtifact buildArtifact(String groupId, String artifactId, String version, String classifier, String extension) {
        return new MavenArtifact(groupId, artifactId, version, classifier, extension);
    }

    @Override
    public MavenArtifactDeclaration resolveArtifact(MavenArtifact artifact) throws IOException, ParserConfigurationException, SAXException {
        Path dataDir = this.store.getDataDir();
        Path localLibraries = dataDir.resolve("libraries");

        Path artifactRelativePath = artifact.getRelativePath();

        String pomPath = artifactRelativePath.toString().replace(artifact.getExtension(), "pom");
        Path localPomPath = localLibraries.resolve(pomPath);
        if (!localPomPath.toFile().exists()) {
            URI remotePom = this.repository.resolve(pomPath);

            try (InputStream inputStream = this.requestHelper.establishConnection(remotePom.toURL()).getInputStream()) {
                Files.copy(inputStream, localPomPath);
            }
        }

        List<MavenArtifactDependency> dependencyList = this.getDependency(Files.newInputStream(localPomPath))
                .stream()
                .map((mavenArtifact) -> {
                    MavenArtifactDependency mavenArtifactDependency = new MavenArtifactDependency();
                    mavenArtifactDependency.setDeclaration(new MavenArtifactDeclaration(mavenArtifact, new ArrayList<>()));
                    return mavenArtifactDependency;
                })
                .collect(Collectors.toList());
        return new MavenArtifactDeclaration(artifact, dependencyList);
    }

    public InputStream createArtifactInputStream(MavenArtifact mavenArtifact) throws IOException {
        URI resolved = repository.resolve(mavenArtifact.getDeclaration());

        if (!resolved.isAbsolute()) {
            throw new RuntimeException("Could not createArtifactInputStream from " + resolved);
        }

        URLConnection connection = this.requestHelper.establishConnection(resolved.toURL());
        return connection.getInputStream();
    }

    private List<MavenArtifact> getDependency(InputStream pom) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(pom);
        Element documentElement = document.getDocumentElement();
        Element dependencies = (Element) documentElement.getElementsByTagName("dependencies").item(0);
        NodeList dependencyList = dependencies.getElementsByTagName("dependency");

        List<MavenArtifact> list = new ArrayList<>();

        for (int i = 0; i < dependencyList.getLength(); i++) {
            Element dependency = (Element) dependencyList.item(i);

            String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
            String version = dependency.getElementsByTagName("version").item(0).getTextContent();

            list.add(
                    new MavenArtifact(
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
