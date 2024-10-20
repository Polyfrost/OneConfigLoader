package org.polyfrost.oneconfig.loader.stage1.backend;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.utils.RequestHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class BackendArtifact {

	public final String group, name, url;
	public final BackendChecksum checksum;

	public BackendArtifact(String group, String name, String url, BackendChecksum checksum) {
		this.group = group;
		this.name = name;
		this.url = url;
		this.checksum = checksum;
	}

	@SneakyThrows
	public void downloadTo(RequestHelper requestHelper, Path path, Consumer<Float> progressConsumer) {
		URLConnection connection = requestHelper.establishConnection(URI.create(url).toURL());
		if (!(connection instanceof HttpURLConnection)) {
			throw new IllegalArgumentException("Connection is not an HTTP connection");
		}

		HttpURLConnection httpConnection = (HttpURLConnection) connection;
		httpConnection.connect();

		if (httpConnection.getResponseCode() != 200) {
			throw new RuntimeException("Failed to download artifact: " + httpConnection.getResponseCode());
		}

		try (InputStream inputStream = connection.getInputStream()) {
			long totalSize = connection.getContentLengthLong();

			byte[] buffer = new byte[4096];
			long totalRead = 0;
			int read;

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			while ((read = inputStream.read(buffer)) != -1) {
				output.write(buffer, 0, read);
				totalRead += read;
				progressConsumer.accept((float) totalRead / totalSize);
			}

			Files.write(path, output.toByteArray());
		}
	}

}
