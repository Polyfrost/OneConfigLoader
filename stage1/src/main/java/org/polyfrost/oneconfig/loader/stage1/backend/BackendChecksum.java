package org.polyfrost.oneconfig.loader.stage1.backend;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.polyfrost.polyio.util.HashingHelper;

import java.nio.file.Path;
import java.security.MessageDigest;

@Log4j2
public class BackendChecksum {

	public final String type, hash;
	private transient MessageDigest digest;

	public BackendChecksum(String type, String hash) {
		this.type = type;
		this.hash = hash;
		ensureDigest();
	}

	@SneakyThrows
	public boolean isMatching(Path path) {
		ensureDigest();

		String pathHash = HashingHelper.hash(path, digest);
		System.out.println("path: " + path + "|" + "hash: " + hash + " | pathHash: " + pathHash);
		return hash.equals(pathHash);
	}

	private void ensureDigest() {
		MessageDigest digest;

		try {
			digest = MessageDigest.getInstance(type);
		} catch (Exception e) {
			log.error("Failed to create checksum digest, falling back to SHA-256", e);

			try {
				digest = MessageDigest.getInstance("SHA-256");
			} catch (Exception e2) {
				log.error("Something went horribly wrong, SHA-256 is not available", e2);
				throw new RuntimeException(e2);
			}
		}

		this.digest = digest;
	}

}
