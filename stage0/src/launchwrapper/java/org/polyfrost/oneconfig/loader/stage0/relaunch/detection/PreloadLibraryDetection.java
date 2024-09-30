package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.launchwrapper.Launch;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pre-load the resource cache of the launch class loader with the class files of some of our libraries.
// Doing so will allow us to load our version, even if there is an older version already on the classpath
// before our jar. This will of course only work if they have not already been loaded but in that case
// there's really not much we can do about it anyway.
@Getter
@Setter
@Log4j2
public class PreloadLibraryDetection implements Detection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;
	private final String id;
	private final String firstPath;
	private final String[] restOfPath;
	private Path libPath = null;

	public PreloadLibraryDetection(String id, String... path) {
		this.id = id;
		this.firstPath = path[0];
		this.restOfPath = Arrays.copyOfRange(path, 1, path.length);
	}

	@Override
	public void checkRelaunch(String id, List<URL> urls, Field classLoaderExceptionsField, Set<String> classLoaderExceptions, Field transformerExceptionsField, Set<String> transformerExceptions, Field resourceCacheField, Map<String, byte[]> resourceCache, Field negativeResourceCacheField, Set<String> negativeResourceCache) throws Exception {
		if (!id.startsWith(this.id)) {
			return;
		}
		try (FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(urls.get(0).toURI()), null)) {
			libPath = fileSystem.getPath(firstPath, restOfPath);

			if (Files.notExists(libPath)) {
				log.debug("Not pre-loading {} because it does not exist.", libPath);
				return;
			}

			detectedUrls = urls;

			log.debug("Pre-loading {} from {}..", libPath, urls.get(0).getPath());
			long start = System.nanoTime();

			Files.walkFileTree(libPath, new SimpleFileVisitor<Path>() {
				private static final String SUFFIX = ".class";
				private boolean warned;

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
					if (path.getFileName().toString().endsWith(SUFFIX)) {
						String file = path.toString().substring(1);
						String name = file.substring(0, file.length() - SUFFIX.length()).replace('/', '.');
						byte[] bytes = Files.readAllBytes(path);
						byte[] oldBytes = resourceCache.put(name, bytes);
						if (oldBytes != null && !Arrays.equals(oldBytes, bytes) && !warned) {
							warned = true;
							log.warn("Found potentially conflicting version of {} already loaded. This may cause issues.", libPath);
							log.warn("First conflicting class: {}", name);
							try {
								log.warn("Likely source: {}", Launch.classLoader.findResource(file));
							} catch (Throwable t) {
								log.warn("Unable to determine likely source:", t);
							}
							relaunch = true;
						}
						negativeResourceCache.remove(name);
					}
					return FileVisitResult.CONTINUE;
				}
			});

			log.debug("Done after {}ns.", System.nanoTime() - start);
		}
	}
}
