package org.polyfrost.oneconfig.loader.stage1.backend;

import cc.polyfrost.polyio.util.PolyHashing;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;
import java.security.MessageDigest;

@Log4j2
public class BackendChecksum {

	public final String type, hash;
	private transient final MessageDigest digest;

	public BackendChecksum(String type, String hash) {
		this.type = type;
		this.hash = hash;

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

	@SneakyThrows
	public boolean isMatching(Path path) {
		return hash.equals(PolyHashing.hash(path, digest));
	}

}
