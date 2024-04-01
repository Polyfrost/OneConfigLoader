package org.polyfrost.oneconfig.loader.stage0;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import org.apache.logging.log4j.core.Appender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.loader.ILoader;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;

import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
public @Data class ModLauncherCapabilities implements ILoader.Capabilities {
    static final int MODLAUNCHER_VERSION;
    private final EnumEntrypoint entrypointType = EnumEntrypoint.MODLAUNCHER;

    @Override
    public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
        //TODO: Add to a list/set that's passed on to the TransformationService's runScan thingy
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public @Nullable ClassLoader getClassLoader() {
        return ModLauncherCapabilities.class.getClassLoader();
    }

    @Override
    public Path getGameDir() {
        return Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException(
                        "Game directory key not found in ModLauncher environment"
                ));
    }

    @Override
    public @Nullable Appender provideLogAppender() {
        try {
            return StartupNotificationManager.modLoaderConsumer()
                    .map(consumer -> new BaseAppender("neoforged", consumer))
                    .orElse(null);
        } catch (NoClassDefFoundError ignored) {
            try {
                return StartupMessageManager.modLoaderConsumer()
                        .map(consumer -> new BaseAppender("fml", consumer))
                        .orElse(null);
            } catch (NoClassDefFoundError ignored2) {
                try {
                    return net.minecraftforge.fml.loading.progress.StartupNotificationManager.modLoaderConsumer()
                            .map(consumer -> new BaseAppender("fml", consumer))
                            .orElse(null);
                } catch (NoClassDefFoundError ignored3) {
                    // might be 1.13.x
                    log.warn("No startup message manager found");
                    return null;
                }
            }
        }
    }

    static {
        int version;
        try {
            version = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MLSPEC_VERSION.get())
                    .map(Object::toString)
                    .map(it -> it.split(Pattern.quote("."))[0])
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new IllegalStateException("ModLauncher version not found"));
        } catch (NoClassDefFoundError ex2) {
            try {
                version = Integer.parseInt(IEnvironment.class.getPackage().getSpecificationVersion().split(Pattern.quote("."))[0]);
            } catch (Exception ex) {
                throw new IllegalStateException("Unknown ModLauncher version", ex2);
            }
        }
        MODLAUNCHER_VERSION = version;
    }
}
