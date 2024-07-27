package org.polyfrost.oneconfig.loader.stage1.dependency.maven;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.cache.MavenCachingSolution;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
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
import java.net.HttpURLConnection;
import java.net.URI;
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
    private final CachingSolution cache;

    private static final RequestHelper requestHelper = new RequestHelper();

    public MavenDependencyManager(XDG.ApplicationStore store, URI repository) {
        this.store = store;
        this.repository = repository;
        this.cache = new MavenCachingSolution(store, repository);
    }

    public MavenArtifact buildArtifact(String groupId, String artifactId, String version) {
        return this.buildArtifact(groupId, artifactId, version, null, "jar");
    }

    @Override
    public MavenArtifact buildArtifact(String groupId, String artifactId, String version, String classifier, String extension) {
        return new MavenArtifact(groupId, artifactId, version, classifier, extension);
    }

    @Override
    public MavenArtifactDeclaration resolveArtifact(MavenArtifact artifact) {
        Path dataDir = store.getDataDir();
        Path localLibraries = dataDir.resolve("libraries");

        Path artifactRelativePath = artifact.getRelativePath();

        String pomPath = artifactRelativePath.toString().replace(artifact.getExtension(), "pom");
        Path localPomPath = localLibraries.resolve(pomPath);
        if (!localPomPath.toFile().exists()) {
            URI remotePom = repository.resolve(pomPath);

            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) requestHelper.establishConnection(remotePom.toURL());
                IOUtils.readInto(httpURLConnection.getInputStream(), Files.newOutputStream(localPomPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            List<MavenArtifactDependency> dependencyList = this.getDependency(Files.newInputStream(localPomPath))
                    .stream()
                    .map((mavenArtifact) -> {
                        MavenArtifactDependency mavenArtifactDependency = new MavenArtifactDependency();
                        mavenArtifactDependency.setDeclaration(new MavenArtifactDeclaration(mavenArtifact, new ArrayList<>()));
                        return mavenArtifactDependency;
                    }).collect(Collectors.toList());
            return new MavenArtifactDeclaration(artifact, dependencyList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream createArtifactInputStream(MavenArtifact mavenArtifact) {
        URI resolved = repository.resolve(mavenArtifact.getDeclaration());

        try {
            // assume resolved is URL
            HttpURLConnection httpURLConnection = (HttpURLConnection) requestHelper.establishConnection(resolved.toURL());
            return httpURLConnection.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MavenArtifact> getDependency(InputStream pom) {
        try {
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

            return list;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
