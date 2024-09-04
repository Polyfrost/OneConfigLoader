package org.polyfrost.oneconfig.loader.base;

import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.Appender;
import org.jetbrains.annotations.NotNull;

/**
 * Handoff object that'll be used to delegate mod-loader/platform specific
 * capabilities and metadata.
 *
 * @author xtrm
 * @since 1.1.0
 */
public interface Capabilities {
	RuntimeAccess getRuntimeAccess();
	GameMetadata getGameMetadata();

	default void provideLogAppender(Consumer<Appender> appenderConsumer) {
	}

	/**
	 * Access to the game's environment, or specific mod-loader features to mutate it.
	 *
	 * @author xtrm
	 * @since 1.1.0
	 */
	interface RuntimeAccess {
		void appendToClassPath(boolean mod, @NotNull URL @NotNull... urls);

		ClassLoader getClassLoader();
	}

	/**
	 * Provides information about the game runtime.
	 *
	 * @author xtrm
	 * @since 1.1.0
	 */
	interface GameMetadata {
		Path getGameDir();

		String getGameVersion();
		String getLoaderName();

		default @NotNull String getTargetSpecifier() {
			return String.format("%s-%s", getGameVersion(), getLoaderName());
		}
	}
}
