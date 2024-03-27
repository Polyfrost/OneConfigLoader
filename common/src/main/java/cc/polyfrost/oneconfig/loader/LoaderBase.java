package cc.polyfrost.oneconfig.loader;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * @author xtrm
 */
public abstract @Data class LoaderBase implements ILoader {
    private final @NotNull String name;
    private final @NotNull String version;
    private final @NotNull Capabilities capabilities;
}
