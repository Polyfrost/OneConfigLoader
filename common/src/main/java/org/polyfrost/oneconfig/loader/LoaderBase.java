package org.polyfrost.oneconfig.loader;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
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
    private final @NotNull String name;
    private final @NotNull String version;
    private final @NotNull Capabilities capabilities;

    protected final org.apache.logging.log4j.Logger logger;

    protected LoaderBase(
            @NotNull String name,
            @NotNull String version,
            @NotNull Capabilities capabilities
    ) {
        this.name = name;
        this.version = version;
        this.capabilities = capabilities;

        Logger log = (Logger) LogManager.getLogger(getClass());
        Appender appender = capabilities.provideLogAppender();
        if (appender != null) {
            if (!appender.isStarted()) {
                appender.start();
            }
            Configuration config = log.getContext().getConfiguration();
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
        this.logger.info("Initializing oneconfig-loader/" + name + " v" + version + " for " + capabilities.getEntrypointType().getId());
        if (appender != null) {
            // c'mon a little suspense never killed anybody
            // plus we're on forge, it's not like a second of waiting is gonna affect game launch speeds
            try {
                Thread.sleep(750);
            } catch (InterruptedException ignored) {
            }
        }

        RequestHelper.tryInitialize(this);
    }
}
