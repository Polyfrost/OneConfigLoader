package org.polyfrost.oneconfig.loader.stage0;

import cc.polyfrost.oneconfig.loader.ILoader;
import cc.polyfrost.oneconfig.loader.LoaderBase;
import org.jetbrains.annotations.Nullable;

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
    Stage0Loader(Capabilities capabilities) {
        super(
                "stage0",
                Stage0Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );
    }

    @Override
    public void load(@Nullable ILoader unused) {
        // Lookup stage1 in cache
        //
    }
}