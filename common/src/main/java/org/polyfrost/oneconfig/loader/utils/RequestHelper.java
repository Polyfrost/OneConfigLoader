package org.polyfrost.oneconfig.loader.utils;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;

import org.polyfrost.oneconfig.loader.base.LoaderBase;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

/**
 * A helper class for establishing connections to remote resources.
 *
 * @author xtrm
 * @author ty
 * @author pauliesnug
 * @since 1.1.0
 */
@RequiredArgsConstructor
public class RequestHelper {
    /**
     * <p>As minecraft 1.8.9 is very old, an unfortunate consequence is that the java version is
     * missing many notable root CA certificates used by modern websites (notably, the polyfrost API). Due to this, in order
     * for java to trust websites using certificates issued by providers like LetsEncrypt, the adjacent keystore (
     * password <code>polyfrost</code>) must be manually loaded and used to override the default trust store.</p>
     * <p>NOTE: To reduce file size, this keystore DOES NOT contain any of the certificates added by default in JRE 1.8u51. When
     * loaded in the code, this keystore must be merged with the default CA certificate store (located
     * at <code>JRE_ROOT/lib/security/cacerts</code>).</p>
     *
     * <h1>Editing</h1>
     * <p>In order to modify this keystore, the <code>keytool</code> binary from JRE 1.8u51 can and MUST be used to ensure compatibility. To
     * add a new
     * certificate: <code>keytool -keystore polyfrost.jks -storepass polyfrost -alias CERT_ALIAS_HERE -import -file CERT_FILE_HERE</code>.</p>
     *
     * <h1>Contained certificates</h1>
     * <table>
     * <thead>
     * <tr>
     * <th>Certificate name &amp; link</th>
     * <th>SHA1 fingerprint</th>
     * <th>Reason for addition</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td><a href="https://letsencrypt.org/certs/isrgrootx1.der">ISRG Root X1</a></td>
     * <td><code>CA:BD:2A:79:A1:07:6A:31:F2:1D:25:36:35:CB:03:9D:43:29:A5:E8</code></td>
     * <td>LetsEncrypt&#39;s main root certificate, issued by cloudflare (and thus used by the polyfrost API)</td>
     * </tr>
     * <tr>
     * <td><a href="https://letsencrypt.org/certs/isrg-root-x2.der">ISRG Root X2</a></td>
     * <td><code>BD:B1:B9:3C:D5:97:8D:45:C6:26:14:55:F8:DB:95:C7:5A:D1:53:AF</code></td>
     * <td>LetsEncrypt&#39;s elliptic curve root certiciate, will likely be used eventually for LetsEncrypt issuances</td>
     * </tr>
     * <tr>
     * <td><a href="https://pki.goog/repo/certs/gtsr1.der">GTS Root R1</a></td>
     * <td><code>E5:8C:1C:C4:91:3B:38:63:4B:E9:10:6E:E3:AD:8E:6B:9D:D9:81:4A</code></td>
     * <td>Google Trust Services&#39; main root certificate, issued by cloudflare (and thus used by the polyfrost API)</td>
     * </tr>
     * <tr>
     * <td><a href="https://cacerts.digicert.com/DigiCertGlobalRootG2.crt">DigiCert Global Root G2</a></td>
     * <td><code>DF:3C:24:F9:BF:D6:66:76:1B:26:80:73:FE:06:D1:CC:8D:4F:82:A4</code></td>
     * <td>Used by textures.minecraft.net, added just in case</td>
     * </tr>
     * <tr>
     * <td><a href="http://www.microsoft.com/pkiops/certs/Microsoft%20RSA%20Root%20Certificate%20Authority%202017.crt">Microsoft RSA Root Certificate Authority 2017</a></td>
     * <td><code>73:A5:E6:4A:3B:FF:83:16:FF:0E:DC:CC:61:8A:90:6E:4E:AE:4D:74</code></td>
     * <td>Used as a backup for DigiCert Global Root G2</td>
     * </tr>
     * </tbody>
     * </table>
     */
    private static final String SSL_STORE_PATH = "/assets/oneconfig-loader/ssl/polyfrost.jks";
    private static final String CONNECTION_IDENTIFIER = "oneconfig-loader";
    private static SSLSocketFactory sslSocketFactory;

    private final LoaderBase loader;

    public URLConnection establishConnection(URL url) throws IOException {
        return establishConnection(url, "application/json");
    }

    public URLConnection establishConnection(URL url, String requestedType) throws IOException {
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
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (" + CONNECTION_IDENTIFIER + " " + loader.getName() + "/" + loader.getVersion() + ")");
        connection.setRequestProperty("Cache-Control", "no-cache");
        return connection;
    }

    public static RequestHelper tryInitialize(LoaderBase loader) {
        try {
            if (sslSocketFactory == null) {
                sslSocketFactory = createSSLSocketFactory();
            }
        } catch (RuntimeException e) {
            LogManager.getLogger(RequestHelper.class).error(e);
            ErrorHandler.displayError(loader, "An error occured while constructing SSLSocketFactory");
        }

        return new RequestHelper(loader);
    }

    /**
     * @see #SSL_STORE_PATH
     * @return An {@link SSLSocketFactory} that will trust the extra root CA certificates in the Polyfrost keystore
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        // Initialize a keystore with the java installation's default CA certificates
//        KeyStore keyStore;
//
//        try {
//            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            Path keyStorePath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
//            keyStore.load(Files.newInputStream(keyStorePath), null);
//			System.out.println("Loaded default keystore");
//        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
//            throw new RuntimeException("An error occurred while initializing the Polyfrost keystore with default certificates.", e);
//        }
//
		// Turn the keystore into a factory that can be used with HTTPS requests
//        try {
//            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            trustManagerFactory.init(keyStore);
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
//            return sslContext.getSocketFactory();
//        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
//            throw new RuntimeException("An error occurred while initializing the Polyfrost SSL context.", e);
//        }

		// Deftu - Stolen from some of my other projects because y'all's code was NOT working.

		SslLoader sslLoader = new SslLoader();
		sslLoader.loadPath(Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts").toAbsolutePath());
		sslLoader.loadResource(SSL_STORE_PATH);
		return sslLoader.create().getSocketFactory();
    }
}
