package org.polyfrost.oneconfig.loader.stage1;

import lombok.Data;
import net.minecraft.launchwrapper.Launch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.loader.ILoader.Capabilities;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;

import java.net.URL;
import java.nio.file.Path;

/**
 * {@link Capabilities} implementation for initialization through legacy entrypoints.
 *
 * @author xtrm
 * @since 1.1.0
 */
public @Data class LegacyCapabilities implements Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.LAUNCHWRAPPER;

    @Override
    public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
        for (URL url : urls) {
            Launch.classLoader.addURL(url);
        }
    }

    @Override
    public @Nullable ClassLoader getClassLoader() {
        return Launch.classLoader;
    }

    @Override
    public Path getGameDir() {
        return Launch.minecraftHome.toPath();
    }
}
