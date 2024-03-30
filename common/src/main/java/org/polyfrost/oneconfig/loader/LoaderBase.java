package org.polyfrost.oneconfig.loader;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;

/**
 * @author xtrm
 */
@Getter
public abstract class LoaderBase implements ILoader {
    private final @NotNull String name;
    private final @NotNull String version;
    private final @NotNull Capabilities capabilities;

    protected final Logger log;

    protected LoaderBase(
            @NotNull String name,
            @NotNull String version,
            @NotNull Capabilities capabilities
    ) {
        this.name = name;
        this.version = version;
        this.capabilities = capabilities;

        this.log = LogManager.getLogger(getClass());
        this.log.info("Current launch platform: " + capabilities.getEntrypointType().name());

        RequestHelper.tryInitialize(this);
    }
}
