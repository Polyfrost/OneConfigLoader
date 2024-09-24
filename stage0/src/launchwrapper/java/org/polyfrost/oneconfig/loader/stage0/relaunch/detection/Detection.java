package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Detection {
	void checkRelaunch(String id, List<URL> urls, Field classLoaderExceptionsField, Set<String> classLoaderExceptions, Field transformerExceptionsField, Set<String> transformerExceptions, Field resourceCacheField, Map<String, byte[]> resourceCache, Field negativeResourceCacheField, Set<String> negativeResourceCache) throws Exception;
	boolean isRelaunch();
	boolean setRelaunch(boolean relaunch);
	@Nullable List<URL> getDetectedUrls();
	default boolean shouldCheck(List<Detection> detectionsRan) {
		return true;
	}
}
