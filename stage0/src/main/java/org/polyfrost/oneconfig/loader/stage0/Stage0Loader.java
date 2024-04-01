package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.LoaderBase;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * The first stage of the OneConfig Loader.
 * <p>
 * TODO: Documentation
 *
 * @author xtrm
 * @since 1.1.0
 */
public class Stage0Loader extends LoaderBase {
    private static final String MAVEN_URL = "https://polyfrost.cc/";
    private static final String MAVEN_REPO = "releases";

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                provideImplementationVersion(),
                capabilities
        );
    }

    private static final String FILE_PATH =
            "/home/x/Work/Polyfrost/test/build/libs/test-1.0-SNAPSHOT.jar";

    @SneakyThrows
    @Override
    public void load() {
        capabilities.appendToClassPath(
                false,
                new File(FILE_PATH).toURI().toURL()
        );
//        Class<?> testingClass = capabilities.getClassLoader().loadClass("me.xtrm.test.Testing2");
//        testingClass.getMethod("hi").invoke(null);
        JOptionPane.showMessageDialog(null, "Loading hook");

        // fetch settings
        logger.info("Loading OneConfig settings");
        // Fetch stage1 version info
        logger.info("Fetching stage1 version info");
        // Lookup stage1 in cache, handle downloading
        logger.info("Getting stage1 from cache");
        // Load in classloader as a library
        logger.info("Loading stage1 as a library");
        // Delegate loading to stage1
		logger.info("GO");
    }

    private static @NotNull String provideImplementationVersion() {
        String packageVersion =
                Stage0Loader.class.getPackage().getImplementationVersion();
        if (packageVersion != null) {
            return packageVersion;
        }

        // Fabric / Quilt don't currently support this, so we'll parse the Manifest
        URL manifestUrl = Stage0Loader.class.getResource("/META-INF/MANIFEST.MF");
        if (manifestUrl == null) {
            return UNKNOWN_VERSION;
        }

        Throwable error = null;
        InputStream is = null;
        try {
            is = manifestUrl.openStream();
            Manifest manifest = new Manifest(is);
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version == null) {
                return UNKNOWN_VERSION;
            }
            return version;
        } catch (IOException e) {
            error = e;
            throw new RuntimeException("Error while reading Jar manifest file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (error != null) {
                        error.addSuppressed(e);
                    } else {
                        throw new RuntimeException("Failed to close InputStream", e);
                    }
                }
            }
        }
    }
}
