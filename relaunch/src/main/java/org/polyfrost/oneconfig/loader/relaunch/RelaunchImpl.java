package org.polyfrost.oneconfig.loader.relaunch;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import lombok.extern.log4j.Log4j2;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

import net.minecraft.launchwrapper.Launch;

import org.jetbrains.annotations.NotNull;

import org.polyfrost.oneconfig.loader.relaunch.args.LaunchArgs;

// THE FOLLOWING CODE IS TAKEN AND ADAPTED FROM "EssentialLoader", UNDER THE GPL-3.0 LICENSE.
// https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE

@Log4j2
@SuppressWarnings("LoggingSimilarMessage")
public class RelaunchImpl implements Relaunch {
	private static final String CORE_MOD_MANAGER = "net.minecraftforge.fml.relauncher.CoreModManager";

	static final String FML_TWEAKER = "net.minecraftforge.fml.common.launcher.FMLTweaker";

    private static final String HAPPENED_PROPERTY = "oneconfig.loader.relaunched";
    private static final String ENABLED_PROPERTY = "oneconfig.loader.relaunch";
    public static final String FORCE_PROPERTY = "oneconfig.loader.relaunch.force";

    /** Whether we are currently inside a re-launch due to classpath complications. */
    public static final boolean HAPPENED = Boolean.parseBoolean(System.getProperty(HAPPENED_PROPERTY, "false"));
    /** Whether we should try to re-launch in case of classpath complications. */
    public static final boolean ENABLED = !HAPPENED && Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"));

	public static boolean checkEnabled() {
        if (HAPPENED) {
            return false;
        }

        if (ENABLED) {
            return true;
        }

        log.warn("");
        log.warn("");
        log.warn("");
        log.warn("==================================================================================");
        log.warn("OneConfig can automatically attempt to fix this but this feature has been disabled");
        log.warn("because \"" + ENABLED_PROPERTY + "\" is set to false.");
        log.warn("");
        log.warn("THIS WILL CAUSE ISSUES, PROCEED AT YOUR OWN RISK!");
        log.warn("");
        log.warn("Remove \"-D" + ENABLED_PROPERTY + "=false\" from JVM args to enable re-launching.");
        log.warn("==================================================================================");
        log.warn("");
        log.warn("");
        log.warn("");
        return false;
    }

    public static void relaunch(List<URL> relaunchUrls) {
        log.warn("");
        log.warn("");
        log.warn("");
        log.warn("==================================================================================");
        log.warn("Attempting re-launch to load the newer version instead.");
		log.warn("Relaunch URLs:");
		for (URL url : relaunchUrls) {
			log.warn("    {}", url);
		}
        log.warn("");
        log.warn("If AND ONLY IF you know what you are doing, have fixed the issue manually and need");
        log.warn("to suppress this behavior (did you really fix it then?), you can set the");
        log.warn("\"" + ENABLED_PROPERTY + "\" system property to false.");
        log.warn("==================================================================================");
        log.warn("");
        log.warn("");
        log.warn("");

        // Set marker so we do not end up in a loop
        System.setProperty(HAPPENED_PROPERTY, "true");

        // Clean up certain global state
        cleanupForRelaunch();

        try {
            // Get the system class loader (in prod, this will be the actual system class loader, in tests it will be
            // the IsolatedLaunch classloader), we can rely on it being a URLClassLoader cause Launch does the same.
            URLClassLoader systemClassLoader = (URLClassLoader) Launch.class.getClassLoader();
            // Get the classpath from the system class loader, this will have had various tweaker mods appended to it.
            List<URL> urls = new ArrayList<>(Arrays.asList(systemClassLoader.getURLs()));

            // So we need to make sure OneConfig is on the classpath before any other mod
            urls.removeAll(relaunchUrls);
            urls.addAll(0, relaunchUrls);

            // And because LaunchClassLoader.getSources is buggy and returns a List rather than a Set, we need to try
            // to remove the tweaker jars from the classpath, so we do not end up with duplicate entries in that List.
            // We cannot just remove everything after the first mod jar, cause there are mods like "performant" which
            // reflect into the URLClassPath and move themselves to the beginning.
            // So instead, we remove anything which declares a TweakClass which has in been loaded by the
            // CoreModManager.
            Set<String> tweakClasses = getTweakClasses();
            Iterator<URL> iterator = urls.iterator();
            // skip the URLs we added
			for (int i = 0; i < relaunchUrls.size(); i++) {
				iterator.next();
			}
            while (iterator.hasNext()) {
                URL url = iterator.next();
                if (isTweaker(url, tweakClasses)) {
                    iterator.remove();
                }
            }

            log.debug("Re-launching with classpath:");
            for (URL url : urls) {
                log.debug("    {}", url);
            }

            RelaunchClassLoader relaunchClassLoader = new RelaunchClassLoader(urls.toArray(new URL[0]), systemClassLoader);

            List<String> args = new ArrayList<>(LaunchArgs.guessLaunchArgs());
            String main = args.remove(0);

            Class<?> innerLaunch = Class.forName(main, false, relaunchClassLoader);
            Method innerMainMethod = innerLaunch.getDeclaredMethod("main", String[].class);
            innerMainMethod.invoke(null, (Object) args.toArray(new String[0]));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Unexpected re-launch failure", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            // Clear marker. This only relevant for our tests, production calls System.exit and never returns.
            System.clearProperty(HAPPENED_PROPERTY);
        }
    }

	@Override
	public void maybeRelaunch(DetectionSupplier detectionSupplier, Map<String, List<URL>> urls) {
		List<Detection> detections = detectionSupplier.createDetectionList();
		if (detections.isEmpty()) {
			return; // Somehow, we got a no-op detection supplier despite having a relaunch instance.
		}

		List<URL> relaunchUrls = new ArrayList<>();

		LaunchClassLoaderDataItem<Set<String>> classLoaderExceptionsData = new LaunchClassLoaderDataItem<>("classLoaderExceptions");
		LaunchClassLoaderDataItem<Set<String>> transformerExceptionsData = new LaunchClassLoaderDataItem<>("transformerExceptions");
		LaunchClassLoaderDataItem<Map<String, byte[]>> resourceCacheData = new LaunchClassLoaderDataItem<>("resourceCache");
		LaunchClassLoaderDataItem<Set<String>> negativeResourceCacheData = new LaunchClassLoaderDataItem<>("negativeResourceCache");

		for (Detection detection : detections) {
			for (Map.Entry<String, List<URL>> entry : urls.entrySet()) {
				try {
					detection.checkRelaunch(
							entry.getKey(),
							entry.getValue(),
							classLoaderExceptionsData.field,
							classLoaderExceptionsData.value,
							transformerExceptionsData.field,
							transformerExceptionsData.value,
							resourceCacheData.field,
							resourceCacheData.value,
							negativeResourceCacheData.field,
							negativeResourceCacheData.value
					);

					if (detection.isRelaunch()) {
						List<URL> detectedUrls = detection.getDetectedUrls();
						if (detectedUrls != null) {
							relaunchUrls.addAll(detectedUrls);
						}
						detection.setRelaunch(false);
					}
				} catch (Exception e) {
					throw new RuntimeException("Failed to check relaunch for " + entry.getKey(), e);
				}
			}
		}

		try {
			runFoamFixCompat(resourceCacheData.field, resourceCacheData.value);
		} catch (Exception e) {
			log.error("Failed to run FoamFix compatibility", e);
		}

		if (!relaunchUrls.isEmpty()) {
			if (checkEnabled()) {
				relaunch(relaunchUrls);
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

	private static void cleanupForRelaunch() {
        // https://github.com/MinimallyCorrect/ModPatcher/blob/3a538a5b574546f68d927f3551bf9e61fda4a334/src/main/java/org/minimallycorrect/modpatcher/api/ModPatcherTransformer.java#L43-L51
        System.clearProperty("nallar.ModPatcher.alreadyLoaded");
        // https://github.com/MinimallyCorrect/ModPatcher/blob/3a538a5b574546f68d927f3551bf9e61fda4a334/src/main/java/org/minimallycorrect/modpatcher/api/LaunchClassLoaderUtil.java#L31-L36
        System.clearProperty("nallar.LaunchClassLoaderUtil.alreadyLoaded");

        try {
            cleanupMixinAppender();
        } catch (Throwable t) {
            log.error("Failed to reset mixin appender. INIT-phase mixins may misfunction.", t);
        }
    }

    // Mixin detects the start of the INIT phase by listening to log messages via its Appender. With non-beta log4j2
    // if the inner mixin tries to do the same, its appender will be rejected, and it'll never be able to transition
    // into the INIT phase, skipping all mixins registered for that phase.
    // See MixinPlatformAgentFMLLegacy.MixinAppender
    // To fix that, we remove the outer mixin's appender before relaunching.
    private static void cleanupMixinAppender() {
        Logger fmlLogger = LogManager.getLogger("FML");
        if (fmlLogger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger fmlLoggerImpl = (org.apache.logging.log4j.core.Logger) fmlLogger;
            Appender mixinAppender = fmlLoggerImpl.getAppenders().get("MixinLogWatcherAppender");
            if (mixinAppender != null) {
                fmlLoggerImpl.removeAppender(mixinAppender);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getTweakClasses() {
        try {
            // We derive these from the tweakSorting field as it is common practice for Tweakers to remove themselves
            // from the more direct ignoredModFiles, whereas there is no reason to remove oneself the tweakSorting.
            Field field = Class.forName("net.minecraftforge.fml.relauncher.CoreModManager")
                .getDeclaredField("tweakSorting");
            field.setAccessible(true);
            Map<String, Integer> tweakSorting = (Map<String, Integer>) field.get(null);
            return tweakSorting.keySet();
        } catch (Exception e) {
            log.error("Failed to determine dynamically loaded tweaker classes.", e);
            return Collections.emptySet();
        }
    }

    private static boolean isTweaker(URL url, Set<String> tweakClasses) {
        try {
            URI uri = url.toURI();
            if (!"file".equals(uri.getScheme())) {
                return false;
            }
            File file = new File(uri);
            if (!file.exists() || !file.isFile()) {
                return false;
            }
            try (JarFile jar = new JarFile(file)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    return false;
                }
                return tweakClasses.contains(manifest.getMainAttributes().getValue("TweakClass"));
            }
        } catch (Exception e) {
			log.error("Failed to read manifest from {}:", url, e);
            return false;
        }
    }

	private static class LaunchClassLoaderDataItem<T> {
		public final Field field;
		public final T value;

		@SuppressWarnings("unchecked")
		public LaunchClassLoaderDataItem(String fieldName) {
			try {
				field = LaunchClassLoader.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				value = (T) field.get(Launch.classLoader);
			} catch (Exception e) {
				throw new RuntimeException("Failed to access Launch field " + fieldName, e);
			}
		}
	}

	private static class SourceFile {
		public final File file;
		public final String coreMod;
		public final boolean isMixin;

		private SourceFile(File file, String coreMod, boolean isMixin) {
			this.file = file;
			this.coreMod = coreMod;
			this.isMixin = isMixin;
		}
	}
}
