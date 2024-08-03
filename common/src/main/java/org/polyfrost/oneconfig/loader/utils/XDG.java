package org.polyfrost.oneconfig.loader.utils;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility around the base directories for each operating-system to standardize storage.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class XDG {
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final List<String> UNIX_ALIASES =
			Arrays.asList("cygwin", "nix", "nux", "aix", "bsd", "sunos");
	private static final boolean IS_UNIX = UNIX_ALIASES.stream().anyMatch(OS_NAME::contains);
	private static final boolean IS_MAC = OS_NAME.contains("mac");
	private static final boolean IS_WINDOWS = OS_NAME.contains("win");

	private static final Lazy<Path> USER_HOME = Lazy.of(XDG::fetchUserHome);
	private static final Lazy<Path> DATA_DIR = Lazy.of(XDG::fetchDataDir);
	private static final Lazy<Path> CACHE_DIR = Lazy.of(XDG::fetchCacheDir);

	private XDG() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	public static ApplicationStore provideApplicationStore(String applicationName) throws IOException {
		return new ApplicationStore(
				provideCacheDir(applicationName),
				provideDataDir(applicationName),
				Files.createTempDirectory(applicationName)
		).ensureCreated();
	}

	public static Path provideDataDir(String applicationName) {
		return DATA_DIR.get().resolve(applicationName);
	}

	public static Path provideCacheDir(String applicationName) {
		return CACHE_DIR.get().resolve(applicationName);
	}

	private static Path fetchUserHome() {
		Map<String, String> env = System.getenv();

		Path userHome = null;
		if (IS_WINDOWS) {
			userHome = findFromEnv("USERPROFILE");
			if (userHome == null) {
				String homePath = env.get("HOMEPATH");
				if (homePath != null) {
					userHome = ensureExists(Paths.get(
							env.getOrDefault("HOMEDRIVE", "C:") + homePath
					));
				}
			}
		}

		//@formatter:off
        if (userHome == null) { userHome = findFromEnv("HOME"); }
        if (userHome == null) { userHome = ensureExists(Paths.get(System.getProperty("user.home"))); }
        if (userHome == null) { throw new IllegalStateException("Could not find user home directory"); }
        //@formatter:on

		return userHome;
	}

	private static Path fetchDataDir() {
		Path userDirectory = USER_HOME.get();

		if (IS_UNIX || IS_MAC) {
			Path xdgDataHome = findFromEnv("XDG_DATA_HOME");
			if (xdgDataHome != null) {
				return xdgDataHome;
			}
		}

		Path dataDirectory = null;

		if (IS_WINDOWS) {
			dataDirectory = findFromEnv("APPDATA");
			if (dataDirectory == null) {
				dataDirectory = userDirectory.resolve("AppData/Roaming");
			}
		} else if (IS_MAC) {
			dataDirectory = userDirectory.resolve("Library/Application Support");
		} else if (IS_UNIX) {
			dataDirectory = userDirectory.resolve(".local/share");
		}

		if (dataDirectory == null) {
			dataDirectory = userDirectory;
		}

		return dataDirectory;
	}

	private static Path fetchCacheDir() {
		if (IS_UNIX || IS_MAC) {
			Path xdgCacheHome = findFromEnv("XDG_CACHE_HOME");
			if (xdgCacheHome != null) {
				return xdgCacheHome;
			}
		}
		if (IS_UNIX) {
			return USER_HOME.get().resolve(".cache");
		}

		return DATA_DIR.get();
	}

	private static @Nullable Path findFromEnv(String env) {
		String path = System.getenv(env);
		if (path != null && !path.isEmpty()) {
			return ensureExists(Paths.get(path));
		}
		return null;
	}

	private static @Nullable Path ensureExists(Path potential) {
		if (Files.exists(potential)) {
			return potential;
		}
		return null;
	}

	public static @Data class ApplicationStore {
		private final Path cacheDir;
		private final Path dataDir;
		private final Path tmpDir;

		private ApplicationStore ensureCreated() throws IOException {
			Files.createDirectories(cacheDir);
			Files.createDirectories(dataDir);
			return this;
		}
	}
}
