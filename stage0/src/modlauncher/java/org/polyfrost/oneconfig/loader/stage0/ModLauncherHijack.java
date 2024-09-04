package org.polyfrost.oneconfig.loader.stage0;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

/**
 * Bunch of hooks into ModLauncher.
 *
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
public enum ModLauncherHijack {
    INSTANCE;

    private final MethodHandle getLaunchService;
    private final MethodHandle getPluginsMap;

    ModLauncherHijack() {
        getLaunchService = findGetter(Launcher.class, "launchPlugins");
        getPluginsMap = findGetter(LaunchPluginHandler.class, "plugins");
    }

    public void injectLaunchPluginService() {
        LaunchPluginHandler launchService;
        Map<String, ILaunchPluginService> plugins;
        try {
            launchService = (LaunchPluginHandler)
                    getLaunchService.bindTo(Launcher.INSTANCE).invoke();
            //noinspection unchecked
            plugins = (Map<String, ILaunchPluginService>)
                    getPluginsMap.bindTo(launchService).invoke();
        } catch (Throwable e) {
            throw new RuntimeException("Couldn't get ModLauncher internal variable", e);
        }
        injectLaunchPluginService(plugins, new ModLauncherLaunchPluginService());
    }

    private void injectLaunchPluginService(Map<String, ILaunchPluginService> plugins, ILaunchPluginService service) {
        plugins.put(service.name(), service);
        log.info("Injected launch plugin service: {}", service.name());
        try {
            Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get())
                    .ifPresent(mods -> {
                        Map<String, String> modData = new HashMap<>(3);
                        modData.put("name", service.name());
                        modData.put("type", "PLUGINSERVICE");
                        try {
							URL url = getServiceUrl(service);
							Path path = Paths.get(url.toURI());
                            modData.put("file", path.getFileName().toString());
                        } catch (Throwable ignored) {
                            modData.put("file", "MISSING FILE");
                        }
                        mods.add(modData);
                    });
        } catch (Throwable ignored) {
            // probably an old modlauncher version
        }
        Launcher.INSTANCE.environment()
                .findLaunchPlugin(service.name())
                .ifPresent(launchPlugin -> launchPlugin.offerResource(
                        null,
                        "oneconfig-loader:modlauncher_loaded"
                ));
    }

	private static @NotNull URL getServiceUrl(ILaunchPluginService service) {
		ProtectionDomain protectionDomain = service.getClass().getProtectionDomain();
		if (protectionDomain == null) {
			throw new IllegalStateException("Protection domain is null");
		}
		CodeSource codeSource = protectionDomain.getCodeSource();
		if (codeSource == null) {
			throw new IllegalStateException("Code source is null");
		}
		URL url = codeSource.getLocation();
		if (url == null) {
			throw new IllegalStateException("URL is null");
		}
		return url;
	}

	private MethodHandle findGetter(Class<?> targetClass, String... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("No names provided");
        }
        Throwable first = null;
        for (String name : names) {
            try {
                Field field = targetClass.getDeclaredField(name);
                field.setAccessible(true);
                return MethodHandles.lookup().unreflectGetter(field);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        throw new RuntimeException("Failed to find getter for " + names[0], first);
    }
}
