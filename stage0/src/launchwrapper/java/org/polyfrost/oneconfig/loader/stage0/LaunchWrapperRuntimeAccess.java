package org.polyfrost.oneconfig.loader.stage0;

import java.lang.reflect.Field;
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
		for (@NotNull URL url : urls) {
			Launch.classLoader.addURL(url);

			ClassLoader parentClassLoader = Launch.classLoader.getClass().getClassLoader();
			addUrlToClassLoader(parentClassLoader, url);
			ourUrls.computeIfAbsent(id, k -> new ArrayList<>()).add(url);
		}
	}

	@SneakyThrows
	private void addUrlToClassLoader(ClassLoader loader, URL url) {
		if (loader instanceof URLClassLoader) {
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(loader, url);
		} else {
			Field ucpField;
			try {
				// Java 8-11
				ucpField = loader.getClass().getDeclaredField("ucp");
			} catch (NoSuchFieldException e) {
				// Java 17
				ucpField = loader.getClass().getSuperclass().getDeclaredField("ucp");
			}
			ucpField.setAccessible(true);
			// URLClassPath is in different packages in different Java versions, so we use Object.
			final Object ucp = ucpField.get(loader);
			final Method urlAdder = ucp.getClass().getDeclaredMethod("addURL", URL.class);
			urlAdder.invoke(ucp, url);
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
