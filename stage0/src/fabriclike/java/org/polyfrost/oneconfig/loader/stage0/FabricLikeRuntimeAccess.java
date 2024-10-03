package org.polyfrost.oneconfig.loader.stage0;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.impl.launch.common.QuiltLauncherBase;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public class FabricLikeRuntimeAccess implements Capabilities.RuntimeAccess {
	private final ClassLoader classLoader;
	private final Consumer<URL> appender = initializeAppender();

	@Override
	public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
		for (URL url : urls) {
			appender.accept(url);
		}
	}


	@SuppressWarnings("deprecation")
	private Consumer<URL> initializeAppender() {
		//TODO: In the future, use a Quilt Loader Plugin for this shit
		Consumer<Path> appenderMiddleware;
		try {
			appenderMiddleware = QuiltLauncherBase.getLauncher()::addToClassPath;
			System.out.println("Found Quilt middleware");
		} catch (Throwable ignored) {
			try {
				appenderMiddleware = FabricLauncherBase.getLauncher()::addToClassPath;
				System.out.println("Found Fabric middleware");
			} catch (Throwable ignored2) {
				try {
					return net.fabricmc.loader.launch.common.FabricLauncherBase.getLauncher()::propose;
				} catch (Throwable ignored3) {
					return scanForAppender();
				}
			}
		}
		final Consumer<Path> finalAppenderMiddleware = appenderMiddleware;
		return url -> {
			try {
				System.out.println("Appending URL to classpath: " + url);
				finalAppenderMiddleware.accept(Paths.get(url.toURI()));
			} catch (Exception ex) {
				throw new RuntimeException("Error while appending URL", ex);
			}
		};
	}

	private Consumer<URL> scanForAppender() {
		//noinspection ExtractMethodRecommender
		Method addUrlMethod = null;
		for (Class<?> clazz = classLoader.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			try {
				addUrlMethod = clazz.getDeclaredMethod("addURL", URL.class);
				break;
			} catch (NoSuchMethodException ignored) {
				for (Method method : clazz.getDeclaredMethods()) {
					if (method.getReturnType() == Void.TYPE && method.getParameterCount() == 1 && method.getParameterTypes()[0] == URL.class) {
						addUrlMethod = method;
						break;
					}
				}
			}
		}
		if (addUrlMethod == null) {
			throw new IllegalStateException("Couldn't find addURL method in class loader (and superclasses)");
		}
		final Method finalAddUrlMethod = addUrlMethod;
		try {
			addUrlMethod.setAccessible(true);
			MethodHandle handle = MethodHandles.lookup().unreflect(addUrlMethod);
			return url -> {
				try {
					handle.invoke(classLoader, url);
				} catch (Throwable t) {
					throw new RuntimeException("Failed to append URL to classpath (lookup)", t);
				}
			};
		} catch (Throwable t) {
			return url -> {
				try {
					finalAddUrlMethod.invoke(classLoader, url);
				} catch (Throwable t2) {
					throw new RuntimeException("Failed to append URL to classpath (reflect)", t);
				}
			};
		}
	}
}
