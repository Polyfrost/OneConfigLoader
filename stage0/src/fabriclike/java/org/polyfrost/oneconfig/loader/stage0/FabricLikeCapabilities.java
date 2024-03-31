package org.polyfrost.oneconfig.loader.stage0;

import lombok.Data;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.ILoader;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;
import org.quiltmc.loader.impl.launch.common.QuiltLauncherBase;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * @author xtrm
 * @since 1.1.0
 */
public @Data class FabricLikeCapabilities implements ILoader.Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.FABRICLIKE;
    private final ClassLoader classLoader;
    private Consumer<URL> appender;

    @Override
    public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
        if (appender == null) {
            initializeAppender();
        }
        for (URL url : urls) {
            appender.accept(url);
        }
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @SuppressWarnings("deprecation")
    private void initializeAppender() {
        //TODO: maybe find a quilt api to do this?
        Consumer<Path> appenderMiddleware;
        try {
            appenderMiddleware = QuiltLauncherBase.getLauncher()::addToClassPath;
        } catch (NoClassDefFoundError ignored) {
            try {
                appenderMiddleware = FabricLauncherBase.getLauncher()::addToClassPath;
            } catch (NoClassDefFoundError ignored2) {
                try {
                    appender = net.fabricmc.loader.launch.common.FabricLauncherBase.getLauncher()::propose;
                    return;
                } catch (NoClassDefFoundError ignored3) {
                    scanForAppender();
                    return;
                }
            }
        }
        final Consumer<Path> finalAppenderMiddleware = appenderMiddleware;
        appender = url -> {
            try {
                System.out.println("Appending URL to classpath: " + url);
                finalAppenderMiddleware.accept(Paths.get(url.toURI()));
            } catch (Exception ex) {
                throw new RuntimeException("Error while appending URL", ex);
            }
        };
    }

    private void scanForAppender() {
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
            appender = url -> {
                try {
                    handle.invoke(classLoader, url);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to append URL to classpath (lookup)", t);
                }
            };
        } catch (Throwable t) {
            appender = url -> {
                try {
                    finalAddUrlMethod.invoke(classLoader, url);
                } catch (Throwable t2) {
                    throw new RuntimeException("Failed to append URL to classpath (reflect)", t);
                }
            };
        }
    }
}
