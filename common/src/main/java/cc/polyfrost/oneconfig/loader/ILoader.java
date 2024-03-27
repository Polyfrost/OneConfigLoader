package cc.polyfrost.oneconfig.loader;

import cc.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface ILoader extends IMetaHolder {
    /**
     * Initializes and runs the loader.
     *
     * @param previous the previous loader, if any.
     */
    void load(@Nullable ILoader previous);

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
        //TODO(@xtrm): probably refactor to pass an entire version object
        EnumEntrypoint getEntrypointType();

        void appendToClassPath(boolean mod, @NotNull URL @NotNull... urls);

        @Nullable ClassLoader getClassLoader();
    }
}