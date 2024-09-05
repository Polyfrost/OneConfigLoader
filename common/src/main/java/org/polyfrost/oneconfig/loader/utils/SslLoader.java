package org.polyfrost.oneconfig.loader.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

class SslLoader {

	private CertificateFactory certificateFactory;
	private KeyStore keyStore;

	{
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (CertificateException | KeyStoreException e) {
			throw new RuntimeException(e);
		}
	}

	void loadPath(Path path) {
		try {
			keyStore.load(Files.newInputStream(path), null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void loadResource(String path) {
		InputStream resourceStream;
		Throwable suppressedException = null;

		try {
			resourceStream = getClass().getResourceAsStream(path);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

		if (resourceStream == null) {
			throw new RuntimeException("Resource not found: " + path);
		}

		try {
			InputStream certStream = new BufferedInputStream(resourceStream);
			Certificate cert = certificateFactory.generateCertificate(certStream);
			keyStore.setCertificateEntry("polyfrost", cert);
		} catch (Throwable t) {
			suppressedException = t;
		} finally {
			try {
				resourceStream.close();
			} catch (Throwable t) {
				if (suppressedException != null) {
					t.addSuppressed(suppressedException);
				}
			}
		}
	}

	SSLContext create() {
		try {
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

			return sslContext;
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}

}
