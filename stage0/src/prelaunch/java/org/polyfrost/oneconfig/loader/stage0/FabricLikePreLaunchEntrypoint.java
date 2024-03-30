package org.polyfrost.oneconfig.loader.stage0;

import org.polyfrost.oneconfig.loader.ILoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class FabricLikePreLaunchEntrypoint implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        ILoader.Capabilities capabilities = new FabricLikeCapabilities();
        new Stage0Loader(capabilities).load();
    }
}
