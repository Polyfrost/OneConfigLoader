package org.polyfrost.oneconfig.loader.stage0;

import lombok.SneakyThrows;
import me.xtrm.test.Testing;
import org.polyfrost.oneconfig.loader.LoaderBase;

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
    private static final String MAVEN_URL = "https://polyfrost.cc/";
    private static final String MAVEN_REPO = "releases";

    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                Stage0Loader.class.getPackage().getImplementationVersion(), // fun fact apparently this doesn't work on fabric wtf
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
        Testing.hi();

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
