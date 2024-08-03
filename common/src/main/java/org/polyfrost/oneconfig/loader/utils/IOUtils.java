package org.polyfrost.oneconfig.loader.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * Utility class for IO operations.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class IOUtils {
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
}
