package org.polyfrost.oneconfig.loader;

import org.apache.logging.log4j.core.Appender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;

import java.net.URL;
import java.nio.file.Path;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface ILoader extends IMetaHolder {
    /**
     * Initializes and runs the current loader.
     */
    void load();

    /**
     * @return the loader's preserved {@link Capabilities}.
     */
    @NotNull Capabilities getCapabilities();

    /**
     * Handoff object that'll be used to delegate mod-loader/platform specific
     * capabilities and metadata.
     *
     * @author xtrm
     * @since 1.1.0
     */
    interface Capabilities {
        EnumEntrypoint getEntrypointType();

        void appendToClassPath(boolean mod, @NotNull URL @NotNull... urls);

        ClassLoader getClassLoader();

        Path getGameDir();

        @Nullable
        default Appender provideLogAppender() {
            return null;
        }
    }
}