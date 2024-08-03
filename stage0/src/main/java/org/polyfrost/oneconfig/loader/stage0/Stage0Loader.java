package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;
import org.polyfrost.oneconfig.loader.LoaderBase;
import org.polyfrost.oneconfig.loader.utils.IOUtils;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * The first stage of the OneConfig Loader.
 * <p>
 * This class is loaded via the platform-dependant entrypoint (e.g. the LaunchWrapper tweaker),
 * and is responsible for lookup and loading of the stage1 loader.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class Stage0Loader extends LoaderBase {
    private static final String MAVEN_URL = "https://repo.polyfrost.org/";
    private static final String MAVEN_REPO = "releases";

    private final Attributes manifestAttributes;

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );

        try {
            this.manifestAttributes = new Manifest(this.getClass().getResourceAsStream("/META-INF/MANIFEST.MF")).getMainAttributes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void load() {
        JOptionPane.showMessageDialog(null, "Loading hook");

        // fetch settings
        this.logger.info("Loading OneConfig settings");
        String stage1Version = fetchStage1Version();

        // Fetch stage1 version info
        this.logger.info("Fetching stage1 version info");
        Supplier<String> stage1JarUrl = () -> MAVEN_URL + MAVEN_REPO + "/org/polyfrost/oneconfig/loader/stage1/" + stage1Version + "Stage1.jar";

        // Lookup stage1 in cache, handle downloading
        this.logger.info("Getting stage1 from cache");
        Path stage1Jar = lookupStage1CacheOrDownload(stage1Version, stage1JarUrl);

        // Load in classloader as a library
        this.logger.info("Loading stage1 as a library");
        this.capabilities.appendToClassPath(false, stage1Jar.toUri().toURL());

        // Delegate loading to stage1
        this.logger.info("GO");
        Class<?> stage1Class = this.capabilities
                .getClassLoader()
                .loadClass(this.manifestAttributes.getValue("OneConfig-Stage1-Class"));
        Object stage1Instance = stage1Class
                .getDeclaredConstructor(Capabilities.class, RequestHelper.class)
                .newInstance(this.capabilities, this.requestHelper);
        stage1Class.getDeclaredMethod("load").invoke(stage1Instance);
    }

    private String fetchStage1Version() {
        String value = this.manifestAttributes.getValue("OneConfig-Stage1-Version");
        return value != null ? value : System.getProperty("oneconfig.stage0.version");
    }

    private Path lookupStage1CacheOrDownload(String version, Supplier<String> downloadUrl) throws IOException {
        Path cache = XDG
                .provideCacheDir("OneConfig")
                .resolve("loader")
                .resolve("stage0")
                .resolve("OneConfigLoader-Stage0-" + version + ".jar");

        if (!Files.exists(cache)) {
            Files.createDirectories(cache.getParent());

            try (InputStream inputStream = this.requestHelper.establishConnection(new URL(downloadUrl.get())).getInputStream()) {
                Files.copy(inputStream, cache, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return cache;
    }
}
