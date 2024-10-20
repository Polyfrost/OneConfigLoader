package org.polyfrost.oneconfig.loader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for IO operations.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class IOUtils {
	private static final Pattern VERSION_REGEX = Pattern.compile("(?<major>\\d+).(?<minor>\\d+).?(?<patch>\\d+)?");

    private IOUtils() {
        throw new IllegalStateException("This class cannot be instantiated.");
    }

    public static @NotNull String provideImplementationVersion(
            Class<?> clazz, String unknownVersion
    ) {
        String packageVersion = clazz.getPackage().getImplementationVersion();
        if (packageVersion != null) {
            return packageVersion;
        }

        // Fabric / old Quilt don't currently support this, so we'll parse the Manifest
        URL manifestUrl = clazz.getResource("/META-INF/MANIFEST.MF");
        if (manifestUrl == null) {
            return unknownVersion;
        }

        Throwable error = null;
        InputStream is = null;
        try {
            is = manifestUrl.openStream();
            Manifest manifest = new Manifest(is);
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version == null) {
                return unknownVersion;
            }
            return version;
        } catch (IOException e) {
            error = e;
            throw new RuntimeException("Error while reading Jar manifest file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (error != null) {
                        error.addSuppressed(e);
                    } else {
                        throw new RuntimeException("Failed to close InputStream", e);
                    }
                }
            }
        }
    }

	@SneakyThrows
	public static int getJarVersion(URL jarFile) {
		try (JarInputStream inputStream = new JarInputStream(jarFile.openStream(), false)) {
			Manifest manifest = inputStream.getManifest();
			if (manifest == null) {
				return -1;
			}

			String version = manifest.getMainAttributes().getValue("Implementation-Version");
			if (version == null) {
				return -1;
			}

			return parseVersion(version);
		}
	}

	public static byte[] readFully(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}

		return output.toByteArray();
	}

	private static int parseVersion(String version) {
		Matcher matcher = VERSION_REGEX.matcher(version);
		if (!matcher.matches()) {
			return -1;
		}

		int major = Integer.parseInt(matcher.group("major"));
		int minor = Integer.parseInt(matcher.group("minor"));
		int patch = matcher.group("patch") == null ? 0 : Integer.parseInt(matcher.group("patch"));
		return major * 10000 + minor * 100 + patch;
	}
}
