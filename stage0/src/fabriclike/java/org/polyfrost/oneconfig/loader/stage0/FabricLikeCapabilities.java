package org.polyfrost.oneconfig.loader.stage0;

import net.fabricmc.loader.api.FabricLoader;
import org.polyfrost.oneconfig.loader.ILoader;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class FabricLikeCapabilities implements ILoader.Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.FABRICLIKE;

    @Override
    public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {

    }

    @Override
    public @Nullable ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}
