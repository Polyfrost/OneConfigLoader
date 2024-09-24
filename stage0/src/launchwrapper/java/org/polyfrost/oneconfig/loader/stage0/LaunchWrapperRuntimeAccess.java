package org.polyfrost.oneconfig.loader.stage0;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.polyfrost.oneconfig.loader.base.Capabilities;
import org.polyfrost.oneconfig.loader.stage0.relaunch.Relaunch;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.AsmDetection;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.Detection;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.ExcludedKotlinDetection;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.MixinDetection;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.PreloadLibraryDetection;
import org.polyfrost.oneconfig.loader.stage0.relaunch.detection.SignedMixinDetection;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
public class LaunchWrapperRuntimeAccess implements Capabilities.RuntimeAccess {
	public static final LaunchWrapperRuntimeAccess INSTANCE = new LaunchWrapperRuntimeAccess();

	@Override
	@SneakyThrows
	public void appendToClassPath(String id, boolean mod, @NotNull URL @NotNull ... urls) {
		for (@NotNull URL url : urls) {
			Launch.classLoader.addURL(url);

			ClassLoader parentClassLoader = Launch.classLoader.getClass().getClassLoader();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(parentClassLoader, url);
			ourUrls.computeIfAbsent(id, k -> new ArrayList<>()).add(url);
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return Launch.classLoader;
	}

	// THE FOLLOWING CODE IS TAKEN AND ADAPTED FROM "EssentialLoader", UNDER THE GPL-3.0 LICENSE.
	// https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE

	private final Map<String, ArrayList<URL>> ourUrls = new HashMap<>();
	private final List<Detection> detections = new ArrayList<Detection>() {
		{

			add(new AsmDetection());
			add(new ExcludedKotlinDetection());
			add(new SignedMixinDetection());
			String[][][] paths = {
					{{"org.jetbrains.kotlin"}, {"kotlin"}},
					{{"org.jetbrains.kotlinx"}, {"kotlinx", "coroutines"}},
					{{"org.polyfrost:universalcraft"}, {"org", "polyfrost", "universal"}},
					{{"org.polyfrost:polyui"}, {"org", "polyfrost", "polyui"}}
			};
			for (String[][] path : paths) {
				add(new PreloadLibraryDetection(path[0][0], path[1]));
			}
			add(new PreloadLibraryDetection("org.spongepowered:mixin", "org", "spongepowered"));
			add(new MixinDetection());
		}
	};

	/**
	 * Eventually this has to be moved out to a hypothetical stage2... but for now, it's here.
	 */
	@Override
	public void doRelaunchShitREMOVETHISLATER() {
		// time for some FUN!
		List<URL> relaunchUrls = new ArrayList<>();
		try {
			Field classLoaderExceptionsField = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
			classLoaderExceptionsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Set<String> classLoaderExceptions = (Set<String>) classLoaderExceptionsField.get(Launch.classLoader);

			Field transformerExceptionsField = LaunchClassLoader.class.getDeclaredField("transformerExceptions");
			transformerExceptionsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Set<String> transformerExceptions = (Set<String>) transformerExceptionsField.get(Launch.classLoader);

			Field resourceCacheField = LaunchClassLoader.class.getDeclaredField("resourceCache");
			resourceCacheField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, byte[]> resourceCache = (Map<String, byte[]>) resourceCacheField.get(Launch.classLoader);

			Field negativeResourceCacheField = LaunchClassLoader.class.getDeclaredField("negativeResourceCache");
			negativeResourceCacheField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Set<String> negativeResourceCache = (Set<String>) negativeResourceCacheField.get(Launch.classLoader);

			List<Detection> detectionsRan = new ArrayList<>(this.detections); // Detections that have already been run. TODO is this actually needed or nah

			for (Detection detection : detections) {
				detectionsRan.add(detection);
				if (!detection.shouldCheck(detectionsRan)) {
					continue;
				}
				for (Map.Entry<String, ArrayList<URL>> entry : ourUrls.entrySet()) {
					detection.checkRelaunch(entry.getKey(), entry.getValue(), classLoaderExceptionsField, classLoaderExceptions, transformerExceptionsField, transformerExceptions, resourceCacheField, resourceCache, negativeResourceCacheField, negativeResourceCache);
					if (detection.isRelaunch()) {
						List<URL> detectedUrls = detection.getDetectedUrls();
						if (detectedUrls != null) {
							relaunchUrls.addAll(detectedUrls);
						}
						detection.setRelaunch(false);
					}
				}
			}

			runFoamFixCompat(resourceCacheField, resourceCache);
		} catch (Exception e) {
			log.error("Failed to pre-load dependencies: ", e);
		}
		if (!relaunchUrls.isEmpty()) {
			if (Relaunch.checkEnabled()) {
				Relaunch.relaunch(relaunchUrls);
			}
		}
	}

	private void runFoamFixCompat(Field resourceCacheField, Map<String, byte[]> resourceCache) throws Exception {
		if (Launch.classLoader.getClassBytes("pl.asie.foamfix.coremod.FoamFixCore") != null) {
			// FoamFix will by default replace the resource cache map with a weak one, thereby negating our hack.
			// To work around that, we preempt its replacement and put in a map which will throw an exception when
			// iterated.
			log.info("Detected FoamFix, locking LaunchClassLoader.resourceCache");
			resourceCacheField.set(Launch.classLoader, new ConcurrentHashMap<String,byte[]>(resourceCache) {
				// FoamFix will call this before overwriting the resourceCache field
				@Override
				public @NotNull Set<Entry<String, byte[]>> entrySet() {
					throw new RuntimeException("Suppressing FoamFix LaunchWrapper weak resource cache.") {
						// It'll then catch the exception and print it, which we can make less noisy.
						@Override
						public void printStackTrace() {
							log.info(this.getMessage());
						}
					};
				}
			});
		}
	}
}
