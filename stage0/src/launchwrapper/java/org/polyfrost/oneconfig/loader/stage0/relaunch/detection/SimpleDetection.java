package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SimpleDetection implements Detection {
	@Override
	public void checkRelaunch(String id, List<URL> urls, Field classLoaderExceptionsField, Set<String> classLoaderExceptions, Field transformerExceptionsField, Set<String> transformerExceptions, Field resourceCacheField, Map<String, byte[]> resourceCache, Field negativeResourceCacheField, Set<String> negativeResourceCache) {
		checkRelaunch(id, urls);
	}

	public abstract void checkRelaunch(String id, List<URL> urls);
}
