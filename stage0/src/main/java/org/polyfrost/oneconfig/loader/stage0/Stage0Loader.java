package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;
import org.polyfrost.oneconfig.loader.LoaderBase;
import org.polyfrost.oneconfig.loader.utils.IOUtils;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * The first stage of the OneConfig Loader.
 * <p>
 * TODO: Documentation
 *
 * @author xtrm
 * @since 1.1.0
 */
public class Stage0Loader extends LoaderBase {
    private static final String MAVEN_URL = "https://repo.polyfrost.org/";
    private static final String MAVEN_REPO = "releases";

    public static final String DEF_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";

    private static final String STAGE1_CLASS_NAME = "org.polyfrost.oneconfig.loader.stage1.Stage1Loader";

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );
    }

    /*private static final String TEST_FILE_PATH =
            "/home/x/Work/Polyfrost/test/build/libs/test-1.0-SNAPSHOT.jar";*/

    @SneakyThrows
    @Override
    public void load() {
        /*capabilities.appendToClassPath(
                false,
                new File(TEST_FILE_PATH).toURI().toURL()
        );*/
//        Class<?> testingClass = capabilities.getClassLoader().loadClass("me.xtrm.test.Testing2");
//        testingClass.getMethod("hi").invoke(null);
        JOptionPane.showMessageDialog(null, "Loading hook");

        // fetch settings
        logger.info("Loading OneConfig settings");
        String stage1Version = fetchStage1Version();

        // Fetch stage1 version info
        logger.info("Fetching stage1 version info");
        URL stage1JarUrl = buildMavenJarArtifactUrlPath(stage1Version, ".jar", "org", "polyfrost", "oneconfig", "loader", "stage1");
//        JsonObject stage1MavenJson = (JsonObject) new JsonParser().parse(getString(stage1JarUrl.toString(), DEF_AGENT, 5000, true));

        // Lookup stage1 in cache, handle downloading
        logger.info("Getting stage1 from cache");
        File stage1Jar = lookupStage1CacheOrDownload(stage1Version, stage1JarUrl.toString());

        // Load in classloader as a library
        logger.info("Loading stage1 as a library");
        capabilities.appendToClassPath(false, stage1Jar.toURI().toURL());

        // Delegate loading to stage1
        logger.info("GO");

        Class<?> stage1Class = capabilities.getClassLoader().loadClass(STAGE1_CLASS_NAME);
        Object stage1Instance = stage1Class.getDeclaredConstructor(Capabilities.class).newInstance(capabilities);
        stage1Class.getDeclaredMethod("load").invoke(stage1Instance);
    }

    private String fetchStage1Version() {
        // TODO: Change the way to fetch version later
        String version = System.getProperty("oneconfig.stage0.version");
        return version;
    }

    private File lookupStage1CacheOrDownload(String version, String downloadUrl) {
        File cache = new File("./OneConfig/cache/OneConfigLoader/stage1/OneConfigLoader-stage1-" + version + ".jar");
        if (!cache.exists()) {
            cache.getParentFile().mkdirs();
            downloadFile(downloadUrl, cache.toPath());
        }

        return cache;
    }

    public URL buildMavenJarArtifactUrlPath(String version, String extension, String... path) {
        StringBuilder stringBuilder = new StringBuilder(MAVEN_URL);
        stringBuilder.append(MAVEN_REPO);

        for (String s : path) {
            stringBuilder.append('/');
            stringBuilder.append(s);
        }

        stringBuilder.append(version);

        stringBuilder.append('/');
        stringBuilder.append(path[path.length - 1]);
        stringBuilder.append('-');
        stringBuilder.append(version);
        stringBuilder.append(extension);

        try {
            return new URL(stringBuilder.toString());
        } catch (MalformedURLException e) {
            logger.error("Failed to build maven artifact url from {}", String.join(", ", Arrays.asList(path)));
            return null;
        }
    }

    public String getString(String url, String userAgent, int timeout, boolean useCaches) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(setupConnection(url, userAgent, timeout, useCaches), StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            logger.error("Failed to getString from {}", url, e);
            return null;
        }
        return sb.toString();
    }

    public InputStream setupConnection(String url) throws IOException {
        return setupConnection(url, DEF_AGENT, 5000, false);
    }

    public InputStream setupConnection(String url, String userAgent, int timeout, boolean useCaches) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url).openConnection());
        connection.setRequestMethod("GET");
        connection.setUseCaches(useCaches);
        connection.addRequestProperty("User-Agent", userAgent);
        connection.setReadTimeout(timeout);
        connection.setConnectTimeout(timeout);
        connection.setDoOutput(true);
        return connection.getInputStream();
    }

    public boolean downloadFile(String url, Path path, String userAgent, int timeout, boolean useCaches) {
        try (BufferedInputStream in = new BufferedInputStream(setupConnection(url, userAgent, timeout, useCaches))) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to download file from {}", url, e);
            return false;
        }
        return true;
    }

    public boolean downloadFile(String url, Path path) {
        return downloadFile(url, path, DEF_AGENT, 5000, false);
    }
}
