package org.polyfrost.oneconfig.loader.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Utility class for IO operations.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class IOUtils {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private IOUtils() {
        throw new IllegalStateException("This class cannot be instantiated.");
    }

    public static void readInto(InputStream input, OutputStream output) throws IOException {
        readInto(input, output, 8192);
    }

    public static void readInto(InputStream input, OutputStream output, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return readNBytes(inputStream, Integer.MAX_VALUE);
    }

    public static byte[] readNBytes(InputStream inputStream, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = inputStream.read(buf, nread, Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n == 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total
                    ? result
                    : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
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
