package org.polyfrost.oneconfig.loader.stage0;

import cc.polyfrost.oneconfig.loader.ILoader;
import cc.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class ModLauncherCapabilities implements ILoader.Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.MODLAUNCHER;

    @Override
    public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {

    }

    @Override
    public @Nullable ClassLoader getClassLoader() {
        return null;
    }
}
