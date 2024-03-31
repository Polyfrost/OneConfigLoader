package org.polyfrost.oneconfig.loader.stage1;

import org.polyfrost.oneconfig.loader.LoaderBase;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class Stage1Loader extends LoaderBase {
    public Stage1Loader(Capabilities capabilities) {
        super(
                "stage1",
                Stage1Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );
    }

    @Override
    public void load() {
        // Fetch oneconfig version info
        // Lookup dependencies metadata
        // Download to cache
        // Delegate everything to OneConfig
    }
}
