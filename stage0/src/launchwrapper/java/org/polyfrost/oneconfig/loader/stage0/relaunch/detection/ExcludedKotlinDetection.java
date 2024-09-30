package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import lombok.Getter;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.polyfrost.oneconfig.loader.stage0.relaunch.Relaunch;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Getter
@Setter
@Log4j2
public class ExcludedKotlinDetection implements Detection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;
	@Override
	public void checkRelaunch(String id, List<URL> urls, Field classLoaderExceptionsField, Set<String> classLoaderExceptions, Field transformerExceptionsField, Set<String> transformerExceptions, Field resourceCacheField, Map<String, byte[]> resourceCache, Field negativeResourceCacheField, Set<String> negativeResourceCache) {
		if (!id.startsWith("org.jetbrains.kotlin")) {
			return;
		}
		detectedUrls = urls;
		// Some mods (BetterFoliage) will exclude kotlin from transformations, thereby voiding our preloading.
		boolean kotlinExcluded = Stream.concat(classLoaderExceptions.stream(), transformerExceptions.stream())
				.anyMatch(prefix -> prefix.startsWith("kotlin"));
		if (kotlinExcluded && !Relaunch.HAPPENED) {
			log.warn("Found Kotlin to be excluded from LaunchClassLoader transformations. This may cause issues.");
			log.debug("classLoaderExceptions:");
			for (String classLoaderException : classLoaderExceptions) {
				log.debug("  - {}", classLoaderException);
			}
			log.debug("transformerExceptions:");
			for (String transformerException : transformerExceptions) {
				log.debug("  - {}", transformerException);
			}
			relaunch = true;
		}
	}
}
