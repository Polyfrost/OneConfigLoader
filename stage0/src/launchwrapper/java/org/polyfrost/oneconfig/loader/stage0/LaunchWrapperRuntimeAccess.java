package org.polyfrost.oneconfig.loader.stage0;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Log4j2
public class LaunchWrapperRuntimeAccess implements Capabilities.RuntimeAccess {
	public static final LaunchWrapperRuntimeAccess INSTANCE = new LaunchWrapperRuntimeAccess();

	private final Map<String, List<URL>> ourUrls = new HashMap<>();

	@Override
	@SneakyThrows
	public void appendToClassPath(String id, boolean mod, @NotNull URL @NotNull ... urls) {
		if (id.startsWith("org.ow2.asm") && id.contains("9.2")) {
			log.warn("Detected ASM 9.2, removing... THIS SHOULD BE REMOVED LATER BY THE ONECONFIG DEVS...");
			return;
		}

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

	public Map<String, List<URL>> getAppendedUrls() {
		return ourUrls;
	}
}
