package org.polyfrost.oneconfig.loader.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.jetbrains.annotations.NotNull;

import org.polyfrost.oneconfig.loader.utils.RequestHelper;

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

	@Getter(AccessLevel.NONE)
	protected final @NotNull Logger logger;
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

		List<Appender> appenders = new ArrayList<>();
		capabilities.provideLogAppender(appenders::add);
		if (!appenders.isEmpty()) {
			LoggerConfig config = ((org.apache.logging.log4j.core.Logger) logger).getContext()
					.getConfiguration().getLoggerConfig(logger.getName());
			for (Appender appender : appenders) {
				if (!appender.isStarted()) {
					appender.start();
				}

				config.addAppender(appender, null, new AbstractFilter() {
					@Override
					public Result filter(LogEvent event) {
						if (!Objects.equals(event.getLoggerName(), logger.getName())) {
							return Result.DENY;
						}
						return super.filter(event);
					}
				});
			}
		}

		this.logger = logger;
		String fullTargetSpecifier = capabilities.getGameMetadata().getTargetSpecifier();
		this.logger.info("Initializing oneconfig-loader/{} v{} for {}", name, version, fullTargetSpecifier);

		this.requestHelper = RequestHelper.tryInitialize(this);
	}

	/**
	 * Initializes and runs the current loader.
	 */
	public abstract void load();
}
