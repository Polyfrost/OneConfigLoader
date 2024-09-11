package org.polyfrost.oneconfig.loader.stage0;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class LaunchWrapperRuntimeAccess implements Capabilities.RuntimeAccess {
	public static final LaunchWrapperRuntimeAccess INSTANCE = new LaunchWrapperRuntimeAccess();

	@Override
	@SneakyThrows
	public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
		for (@NotNull URL url : urls) {
			Launch.classLoader.addURL(url);

			ClassLoader parentClassLoader = Launch.classLoader.getClass().getClassLoader();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(parentClassLoader, url);
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return Launch.classLoader;
	}
}
