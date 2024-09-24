package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.stage0.relaunch.Relaunch;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Log4j2
public class SignedMixinDetection implements Detection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;

	@Override
	public void checkRelaunch(String id, List<URL> urls, Field classLoaderExceptionsField, Set<String> classLoaderExceptions, Field transformerExceptionsField, Set<String> transformerExceptions, Field resourceCacheField, Map<String, byte[]> resourceCache, Field negativeResourceCacheField, Set<String> negativeResourceCache) throws IOException {
		// Some mods include signatures for all the classes in their jar, including Mixin. As a result, if any other
		// mod ships a Mixin version different from theirs (we likely do), it'll explode because of mis-matching
		// signatures.
		if (!id.equals("org.spongepowered:mixin")) {
			return;
		}
		detectedUrls = urls;
		String signedMixinMod = findSignedMixin();
		if (signedMixinMod != null && !Relaunch.HAPPENED) {
			// To work around that, we'll re-launch. That works because our relaunch class loader does not implement
			// signature loading.
			log.warn("Found {}. This mod includes signatures for its bundled Mixin and will explode if " +
					"a different Mixin version (even a more recent one) is loaded.", signedMixinMod);
			if (Relaunch.ENABLED) {
				log.warn("Trying to work around the issue by re-launching which will ignore signatures.");
			} else {
				log.warn("Cannot apply workaround because re-launching is disabled.");
			}
			relaunch = true;
		}
	}

	private static String findSignedMixin() throws IOException {
		if (hasClass("net.darkhax.surge.Surge")) return "Surge";
		if (hasClass("me.jellysquid.mods.phosphor.core.PhosphorFMLLoadingPlugin")) return "Phosphor";
		return null;
	}

	private static boolean hasClass(String name) throws IOException {
		return Launch.classLoader.getClassBytes(name) != null;
	}
}
