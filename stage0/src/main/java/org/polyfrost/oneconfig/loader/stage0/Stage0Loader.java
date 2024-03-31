package org.polyfrost.oneconfig.loader.stage0;

import org.polyfrost.oneconfig.loader.LoaderBase;

import javax.swing.*;

/**
 * The first stage of the OneConfig Loader.
 * <p>
 * This loader stage wraps around the current launching environment, that
 * being either {@code "launchwrapper"}, {@code "modlauncher"},
 * or {@code "prelaunch"}.
 * <p>
 * This class should provide the necessary interfaces for platform-specific
 * mechanisms to be abstracted away and possibly delegated to further stages.
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
                Stage0Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );
    }

    @Override
    public void load() {
        logger.info("Loading OneConfig settings");
        logger.info("Fetching stage1 version info");
        logger.info("Getting stage1 from cache");
        logger.info("Loading stage1 as a library");

        JOptionPane.showMessageDialog(null, "Loading shit fuckers");
        // fetch settings

        // Fetch stage1 version info
        // Lookup stage1 in cache, handle downloading
        // Load in classloader as a library
        // Delegate loading to stage1
    }
}