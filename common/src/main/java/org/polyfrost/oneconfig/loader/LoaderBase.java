package org.polyfrost.oneconfig.loader;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;

import java.util.Objects;

/**
 * @author xtrm
 */
@Getter
public abstract class LoaderBase implements ILoader {
    protected static final String UNKNOWN_VERSION = "0.0.0+unknown";
    private final @NotNull @Getter String name;
    private final @NotNull @Getter String version;
    protected final @NotNull Capabilities capabilities;
    protected final RequestHelper requestHelper;

    protected final Logger logger;

    protected LoaderBase(
            @NotNull String name,
            @NotNull String version,
            @NotNull Capabilities capabilities
    ) {
        this.name = name;
        this.version = version;
        this.capabilities = capabilities;

        Logger log = LogManager.getLogger(getClass());

        if (version.equalsIgnoreCase(UNKNOWN_VERSION)) {
            log.warn("Jar version is unknown, please report this.");
        }

        Appender appender = capabilities.provideLogAppender();
        if (appender != null) {
            if (!appender.isStarted()) {
                appender.start();
            }
            Configuration config = ((org.apache.logging.log4j.core.Logger) log).getContext().getConfiguration();
            config.getLoggerConfig(log.getName()).addAppender(appender, null, new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (!Objects.equals(event.getLoggerName(), log.getName())) {
                        return Result.DENY;
                    }
                    return super.filter(event);
                }
            });
        }
        this.logger = log;
        this.logger.info("Initializing oneconfig-loader/{} v{} for {}", name, version, capabilities.getEntrypointType().getId());

        this.requestHelper = RequestHelper.tryInitialize(this);
    }
}
