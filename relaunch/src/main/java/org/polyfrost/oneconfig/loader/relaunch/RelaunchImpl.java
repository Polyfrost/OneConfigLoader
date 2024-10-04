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
	private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";

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

	@Override
	public void attemptInjectMixin() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		@SuppressWarnings("unchecked")
		List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");

		// If the MixinTweaker is already queued (because of another mod), then there's nothing we need to to
		if (tweakClasses.contains(MIXIN_TWEAKER)) {
			// Except we do need to initialize the MixinTweaker immediately so we can add containers
			// for our mods.
			// This is idempotent, so we can call it without adding to the tweaks list (and we must not add to
			// it because the queued tweaker will already get added and there is nothing we can do about that).
			initMixinTweaker();
			return;
		}

		// If it is already booted, we're also good to go
		if (Launch.blackboard.get("mixin.initialised") != null) {
			return;
		}

		System.out.println("Injecting MixinTweaker from OneConfig Loader");

		// Otherwise, we need to take things into our own hands because the normal way to chainload a tweaker
		// (by adding it to the TweakClasses list during injectIntoClassLoader) is too late for Mixin.
		// Instead, we instantiate the MixinTweaker on our own and add it to the current Tweaks list immediately.
		@SuppressWarnings("unchecked")
		List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");
		tweaks.add(initMixinTweaker());
	}

	@Override
	public void fixTweakerLoading() {
		List<SourceFile> sourceFiles = getSourceFiles();
		if (sourceFiles.isEmpty()) {
			log.error("Not able to determine current file... Mod will NOT work!");
			return;
		}

		for (SourceFile sourceFile : sourceFiles) {
			try {
				log.warn("Attempting to fix tweaker loading for {}", sourceFile.file);
				setupSourceFile(sourceFile);
			} catch (Exception e) {
				log.error("Failed to setup source file {}", sourceFile.file, e);
			}
		}
	}

	private ITweaker initMixinTweaker() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Launch.classLoader.addClassLoaderExclusion(MIXIN_TWEAKER.substring(0, MIXIN_TWEAKER.lastIndexOf('.')));
		return (ITweaker) Class.forName(MIXIN_TWEAKER, true, Launch.classLoader).newInstance();
	}

	private List<SourceFile> getSourceFiles() {
		List<SourceFile> sourceFiles = new ArrayList<>();
		for (URL url : Launch.classLoader.getSources()) {
			try {
				URI uri = url.toURI();
				if (!"file".equals(uri.getScheme())) {
					continue;
				}
				File file = new File(uri);
				if (!file.exists() || !file.isFile()) {
					continue;
				}
				String tweakClass = null;
				String coreMod = null;
				boolean isMixin = false;
				try (JarFile jar = new JarFile(file)) {
					if (jar.getManifest() != null) {
						Attributes attributes = jar.getManifest().getMainAttributes();
						tweakClass = attributes.getValue("TweakClass");
						coreMod = attributes.getValue("FMLCorePlugin");
						isMixin = attributes.getValue("MixinConfigs") != null;
					}
				}

				if (Objects.equals(tweakClass, "cc.polyfrost.oneconfigwrapper.OneConfigWrapper") || Objects.equals(tweakClass, "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")) {
					sourceFiles.add(new SourceFile(file, coreMod, isMixin));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sourceFiles;
	}

	@SuppressWarnings("unchecked")
	private void setupSourceFile(SourceFile sourceFile) throws Exception {
		Class<?> coreModManagerClz = Class.forName(CORE_MOD_MANAGER);

		// Forge will by default ignore a mod file if it contains a tweaker
		// So we need to remove ourselves from that exclusion list
		Field ignoredModFile = coreModManagerClz.getDeclaredField("ignoredModFiles");
		ignoredModFile.setAccessible(true);
		((List<String>) ignoredModFile.get(null)).remove(sourceFile.file.getName());

		// And instead add ourselves to the mod candidate list
		List<String> reparseableCoremods = (List<String>) coreModManagerClz.getDeclaredMethod("getReparseableCoremods").invoke(null);
		reparseableCoremods.add(sourceFile.file.getName());

		// FML will not load CoreMods if it finds a tweaker, so we need to load the coremod manually if present
		// We do this to reduce the friction of adding our tweaker if a mod has previously been relying on a
		// coremod (cause ordinarily they would have to convert their coremod into a tweaker manually).
		// Mixin takes care of this as well, so we mustn't if it will.
		String coreMod = sourceFile.coreMod;
		if (coreMod != null && !sourceFile.isMixin) {
			Method loadCoreMod = coreModManagerClz.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
			loadCoreMod.setAccessible(true);
			ITweaker tweaker = (ITweaker) loadCoreMod.invoke(null, Launch.classLoader, coreMod, sourceFile.file);
			((List<ITweaker>) Launch.blackboard.get("Tweaks")).add(tweaker);
		}

		// If they declared our tweaker but also want to use mixin, then we'll inject the mixin tweaker
		// for them.
		if (sourceFile.isMixin) {
			// Mixin will only look at jar files which declare the MixinTweaker as their tweaker class, so we need
			// to manually add our source files for inspection.
			try {
				attemptInjectMixin();

				Class<?> MixinBootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
				Class<?> MixinPlatformManager = Class.forName("org.spongepowered.asm.launch.platform.MixinPlatformManager");
				Object platformManager = MixinBootstrap.getDeclaredMethod("getPlatform").invoke(null);
				Method addContainer;
				Object arg;
				try {
					// Mixin 0.7
					addContainer = MixinPlatformManager.getDeclaredMethod("addContainer", URI.class);
					arg = sourceFile.file.toURI();
				} catch (NoSuchMethodException ignored) {
					// Mixin 0.8
					Class<?> IContainerHandle = Class.forName("org.spongepowered.asm.launch.platform.container.IContainerHandle");
					Class<?> ContainerHandleURI = Class.forName("org.spongepowered.asm.launch.platform.container.ContainerHandleURI");
					addContainer = MixinPlatformManager.getDeclaredMethod("addContainer", IContainerHandle);
					arg = ContainerHandleURI.getDeclaredConstructor(URI.class).newInstance(sourceFile.file.toURI());
				}
				addContainer.invoke(platformManager, arg);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
