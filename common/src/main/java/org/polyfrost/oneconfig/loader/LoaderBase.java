package org.polyfrost.oneconfig.loader;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.Map;
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
            try {
                Thread.sleep(550);
            } catch (InterruptedException ignored) {
            }
        }

        RequestHelper.tryInitialize(this);
    }

    static {
        tryPatchJndi((Logger) LogManager.getLogger(LoaderBase.class));
    }

    /**
     * This method is from <a href="https://github.com/FabricMC/fabric-loader/blob/d69cb72d26497e3f387cf46f9b24340b402a4644/minecraft/src/main/java/net/fabricmc/loader/impl/game/minecraft/Log4jLogHandler.java#L112" target="_top">FabricMC/fabric-loader</a>,
     * which is licensed under the Apache License 2.0. It has been slightly modified to change logging.
     */
    private static void tryPatchJndi(Logger logger) {
        LoggerContext context = LogManager.getContext(false);

        try {
            context.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class).invoke(context, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("config")) {
                        removeSubstitutionLookups(logger, true);
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Can't register Log4J2 PropertyChangeListener: %s", e.toString());
        }

        removeSubstitutionLookups(logger, false);
    }

    private static void removeSubstitutionLookups(Logger logger, boolean ignoreMissing) {
        // strip the jndi lookup and then all over lookups from the active org.apache.logging.log4j.core.lookup.Interpolator instance's lookups map

        try {
            LoggerContext context = LogManager.getContext(false);
            if (context.getClass().getName().equals("org.apache.logging.log4j.simple.SimpleLoggerContext")) return; // -> no log4j core

            Object config = context.getClass().getMethod("getConfiguration").invoke(context);
            Object substitutor = config.getClass().getMethod("getStrSubstitutor").invoke(config);
            Object varResolver = substitutor.getClass().getMethod("getVariableResolver").invoke(substitutor);
            if (varResolver == null) return;

            boolean removed = false;

            for (Field field : varResolver.getClass().getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, ?> map = (Map<String, ?>) field.get(varResolver);

                    if (map.remove("jndi") != null) {
                        map.clear();
                        removed = true;
                        break;
                    }
                }
            }

            if (!removed) {
                if (ignoreMissing) return;
                throw new RuntimeException("couldn't find JNDI lookup entry");
            }

            logger.debug("Removed Log4J2 substitution lookups");
        } catch (Exception e) {
            logger.warn("Can't remove Log4J2 JNDI substitution Lookup: {}", e.getMessage());
        }
    }
}
