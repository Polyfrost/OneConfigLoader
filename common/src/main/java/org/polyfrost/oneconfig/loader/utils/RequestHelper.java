package org.polyfrost.oneconfig.loader.utils;

import org.apache.logging.log4j.LogManager;
import org.polyfrost.oneconfig.loader.IMetaHolder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class RequestHelper {
    private static final String SSL_STORE_PATH = "/assets/oneconfig-loader/ssl/polyfrost.der";
    private static IMetaHolder metaHolder;
    private static SSLContext sslContext;

    private HttpURLConnection establishConnection(URL url) throws IOException {
        String protocol = url.getProtocol();

        HttpURLConnection connection;
        if (protocol.equalsIgnoreCase("https")) {
            connection = (HttpsURLConnection) url.openConnection();
            ((HttpsURLConnection) connection).setSSLSocketFactory(
                    sslContext.getSocketFactory());
        } else if (protocol.equalsIgnoreCase("http")) {
            connection = (HttpURLConnection) url.openConnection();
        } else {
            throw new UnsupportedOperationException("Unknown protocol: '" + protocol + "'. Please use http/https for the moment.");
        }
        connection.setConnectTimeout(15000);
        connection.setRequestProperty("User-Agent", Constants.IDENTIFIER + "/" + metaHolder.getName() + " v" + metaHolder.getVersion());
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
    }
}
