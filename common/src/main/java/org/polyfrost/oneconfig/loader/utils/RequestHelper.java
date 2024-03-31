package org.polyfrost.oneconfig.loader.utils;

import org.apache.logging.log4j.LogManager;
import org.polyfrost.oneconfig.loader.IMetaHolder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class RequestHelper {
    private static final String CONNECTION_IDENTIFIER = "oneconfig-loader";
    private static final String SSL_STORE_PATH = "/assets/oneconfig-loader/ssl/polyfrost.der";
    private static IMetaHolder metaHolder;
    private static SSLContext sslContext;
    private static SSLSocketFactory sslSocketFactory;

    protected URLConnection establishConnection(URL url) throws IOException {
        return establishConnection(url, "application/json");
    }

    protected URLConnection establishConnection(URL url, String requestedType) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(
                    sslSocketFactory
            );
        }
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setRequestMethod("GET");
        }
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", requestedType);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (" + CONNECTION_IDENTIFIER + " " + metaHolder.getName() + "/" + metaHolder.getVersion() + ")");
        connection.setRequestProperty("Cache-Control", "no-cache");
        return connection;
    }

    public static void tryInitialize(IMetaHolder metaHolder) {
        try {
            initialize();
            RequestHelper.metaHolder = metaHolder;
        } catch (RuntimeException e) {
            LogManager.getLogger(RequestHelper.class).error(e);
            ErrorHandler.displayError(metaHolder, e.getMessage());
        }
    }

    private static void initialize() {
        if (sslContext != null) {
            return;
        }
        SSLStore sslStore;
        try {
            sslStore = new SSLStore();
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new RuntimeException("An error occurred while initializing the Polyfrost keystore.", e);
        }
        try {
            sslStore.load(SSL_STORE_PATH);
        } catch (CertificateException | KeyStoreException | IOException e) {
            throw new RuntimeException("An error occurred while initializing the Polyfrost SSL certificate.", e);
        }
        try {
            sslContext = sslStore.finish();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("An error occurred while initializing the Polyfrost SSL context.", e);
        }
        sslSocketFactory = sslContext.getSocketFactory();
    }
}
