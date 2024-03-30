package org.polyfrost.oneconfig.loader.utils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * SSLStore attempts to work around the limitations of legacy Minecraft versions in their
 * collection of certificates. Since many legacy versions including 1.8 use legacy versions
 * of Java, these distributions tend to have outdated or missing CA certificates.
 * <p>
 * This class loads and injects a new CA Root certificate into the normal Java KeyStore
 * without having to restart the Java runtime. This allows us to load the certificate
 * from Stage 0 (Wrapper), before any HTTP requests are made. We use this ability to
 * take the context that SSLStore creates and use it for all future HTTP requests
 * during the lifetime of the Java runtime (including for all other stages and mods).
 *
 * @author pauliesnug
 * @see KeyManager
 * @see SSLContext
 */
public class SSLStore {
    private final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    private final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

    public SSLStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        Path keyStorePath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        this.keyStore.load(Files.newInputStream(keyStorePath), null);
    }

    /**
     * Loads the specified SSL certificate.
     *
     * @param sslFile A resource path to a {@code .der} file.
     *
     * @throws IllegalArgumentException If the certificate resource is not found.
     * @throws KeyStoreException If the KeyStore fails to load the certificate.
     * @throws CertificateException If the CertificateFactory fails to generate the certificate.
     * @throws IOException If a problem occurs with I/O operations.
     */
    public void load(String sslFile)
            throws IllegalArgumentException, KeyStoreException, CertificateException, IOException
    {
        InputStream certificateResource = SSLStore.class.getResourceAsStream(sslFile);
        if (certificateResource == null) {
            throw new IllegalArgumentException("Certificate resource not found: " + sslFile);
        }
        Throwable sslThrowable = null;

        // Try to gen and load the certificate
        try {
            InputStream certStream = new BufferedInputStream(certificateResource);
            Certificate generatedCertificate = this.certificateFactory.generateCertificate(certStream);

            this.keyStore.setCertificateEntry(sslFile, generatedCertificate);
        } catch (KeyStoreException | CertificateException sslException) {
            sslThrowable = sslException;
            throw sslException;
        } finally {
            try {
                certificateResource.close();
            } catch (IOException closeException) {
                if (sslThrowable == null) {
                    throw closeException;
                }
                sslThrowable.addSuppressed(closeException);
            }
        }
    }

    /**
     * Generates and returns the SSLContext after the new cert has been added with SSLStore.load().
     *
     * @return The SSLContext generated after init.
     * @throws NoSuchAlgorithmException If the SSLContext or the TrustManagerFactory fail to construct.
     * @throws KeyStoreException If the TrustManagerFactory fails accept the keystore.
     * @throws KeyManagementException If the SSLContext fails to initialize.
     */
    public SSLContext finish() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Initialize TrustManagerFactory with the new KeyStore once the new cert has been added
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(this.keyStore);

        // Return the SSLContext after init.
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
}