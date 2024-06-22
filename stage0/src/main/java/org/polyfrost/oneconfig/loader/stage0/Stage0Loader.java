package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;
import org.polyfrost.oneconfig.loader.LoaderBase;
import org.polyfrost.oneconfig.loader.utils.IOUtils;

import javax.swing.*;
import java.io.File;

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

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                IOUtils.provideImplementationVersion(
                        Stage0Loader.class, UNKNOWN_VERSION
                ),
                capabilities
        );
    }

    private static final String TEST_FILE_PATH =
            "/home/x/Work/Polyfrost/test/build/libs/test-1.0-SNAPSHOT.jar";

    @SneakyThrows
    @Override
    public void load() {
        capabilities.appendToClassPath(
                false,
                new File(TEST_FILE_PATH).toURI().toURL()
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
}
