package cc.polyfrost.oneconfig.loader.stage0;

import cc.polyfrost.oneconfig.loader.ILoader;
import cc.polyfrost.oneconfig.loader.LoaderBase;
import org.jetbrains.annotations.NotNull;

/**
 * <p>The first stage of the OneConfig Loader.</p>
 *
 * <p>This loader stage wraps around the current launching environment, that
 * being either {@code "launchwrapper"}, {@code "modlauncher"}, or
 * {@code "prelaunch"}.</p>
 *
 * <p>This class should provide the necessary interfaces for platform-specific
 * mechanisms to be abstracted away and possibly delegated to urther stages.
 * </p>
 *
 * @author xtrm
 */
class Stage0Loader extends LoaderBase {
    public Stage0Loader() {
        super("stage0", Stage0Loader.class.getPackage().getImplementationVersion());
    }

    @Override
    public @NotNull ILoader getNextLoader() {
        return null;
    }
}
