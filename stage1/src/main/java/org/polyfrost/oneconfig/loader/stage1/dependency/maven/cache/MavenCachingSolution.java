package org.polyfrost.oneconfig.loader.stage1.dependency.maven.cache;

import lombok.RequiredArgsConstructor;
import org.polyfrost.oneconfig.loader.stage1.dependency.cache.CachingSolution;
import org.polyfrost.oneconfig.loader.utils.RequestHelper;
import org.polyfrost.oneconfig.loader.utils.XDG;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

/**
 * @author xtrm
 * @since 1.1.0
 */
@RequiredArgsConstructor
public class MavenCachingSolution extends RequestHelper implements CachingSolution {
    private static final String[] CHECKSUM_EXT = new String[]{"sha512", "sha256", "sha1", "md5"};
    private final XDG.ApplicationStore store;
    private final URI remoteUrl;

    @Override
    public boolean canBeCached(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".pom") && !fileName.endsWith(".jar")) {
            return false;
        }
        // check on the remote server if any signature file exist
        for (String checksumExtension : CHECKSUM_EXT) {
            Path checksumPath = path.resolveSibling(fileName + "." + checksumExtension);
            URI checksumUri = remoteUrl.resolve(checksumPath.toString());
            // if the checksum file exists, then the file can be cached
            URL url;
            try {
                url = checksumUri.toURL();
            } catch (Exception e) {
                continue;
            }
            if (urlExists(url)) {
                System.out.println("File " + path + " can be cached (found " + url + ")");
                return true;
            }
        }
        System.out.println("File " + path + " cannot be cached");
        return false;
    }

    private boolean urlExists(URL url) {
        try {
            URLConnection connection = establishConnection(url);
            connection.connect();
            long contentLength = connection.getContentLengthLong();
            int responseCode = 200;
            if (connection instanceof HttpURLConnection) {
                responseCode = ((HttpURLConnection) connection).getResponseCode();
            }
            connection.getInputStream().close();
            connection.getOutputStream().close();
            return (responseCode >= 200 && responseCode < 300) && contentLength > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
