package org.polyfrost.oneconfig.loader.base;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public abstract class LoaderBase {

	protected static final String UNKNOWN_VERSION = "0.0.0+unknown";

	private final @NotNull String name;
	private final @NotNull String version;
	private final @NotNull Capabilities capabilities;

	private final @NotNull Logger logger;
	private final @NotNull RequestHelper requestHelper;

	protected LoaderBase(
			@NotNull String name,
			@NotNull String version,
			@NotNull Capabilities capabilities
	) {
		this.name = name;
		this.version = version;
		this.capabilities = capabilities;

		Logger logger = LogManager.getLogger(getClass());
		if (version.equalsIgnoreCase(UNKNOWN_VERSION)) {
			logger.warn("Jar version is unknown, please report this.");
		}

		Appender appender = capabilities.provideLogAppender();
		if (appender != null) {
			if (!appender.isStarted()) {
				appender.start();
			}

			Configuration config = ((org.apache.logging.log4j.core.Logger) logger).getContext().getConfiguration();
			config.getLoggerConfig(logger.getName()).addAppender(appender, null, new AbstractFilter() {
				@Override
				public Result filter(LogEvent event) {
					if (!Objects.equals(event.getLoggerName(), logger.getName())) {
						return Result.DENY;
					}
					return super.filter(event);
				}
			});
		}

		this.logger = logger;
		this.logger.info("Initializing oneconfig-loader/{} v{} for {}", name, version, capabilities.getEntrypointType().getId());

		this.requestHelper = RequestHelper.tryInitialize(this);
	}

    /**
     * Initializes and runs the current loader.
     */
    public abstract void load();

	/**
	 * Handoff object that'll be used to delegate mod-loader/platform specific
	 * capabilities and metadata.
	 *
	 * @author xtrm
	 * @since 1.1.0
	 */
	public interface Capabilities {

		EnumEntrypoint getEntrypointType();

		void appendToClassPath(boolean mod, @NotNull URL @NotNull... urls);

		ClassLoader getClassLoader();

		Path getGameDir();

		String getModLoaderName();

		String getGameVersion();

		@Nullable
		default Appender provideLogAppender() {
			return null;
		}

	}

}
