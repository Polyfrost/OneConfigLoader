package org.polyfrost.oneconfig.loader.stage0;

import cc.polyfrost.oneconfig.loader.ILoader;
import cc.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import lombok.Data;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * LaunchWrapper Capabilities for OneConfig Loader.
 * <p>
 * LaunchWrapper loading is surprisingly simple, as it only requires the
 * addition of URLs to the classpath, both for libraries and mods.
 *
 * @author xtrm
 * @since 1.1.0
 */
public @Data class LaunchWrapperCapabilities implements ILoader.Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.LAUNCHWRAPPER;
    private final LaunchClassLoader launchClassLoader;

    @Override
    public void appendToClassPath(boolean mod, URL @NotNull ... urls) {
        for (URL url : urls) {
            launchClassLoader.addURL(url);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return launchClassLoader;
    }
}
