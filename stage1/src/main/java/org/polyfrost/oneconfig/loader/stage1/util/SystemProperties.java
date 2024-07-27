package org.polyfrost.oneconfig.loader.stage1.util;

import lombok.Getter;
import org.polyfrost.oneconfig.loader.utils.Lazy;
import org.polyfrost.oneconfig.loader.utils.XDG;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class SystemProperties {
    private static final String PREFIX = "oneconfig.loader.";
    public static final Property<Path> STORAGE_DIRECTORY = property(
            "storageDirectory",
            () -> XDG.provideDataDir("OneConfig")
                    .resolve("loader")
    );

    public static final Property<Boolean> SKIP_MC_LOOKUP =
            property("libraries.skipMinecraftLookup", false);

    // Remove overrides
    public static final Property<String> BRANCH =
            property("branch", () -> "releases");
    public static final Property<URI> REPOSITORY_URL =
            property("repositoryUri", () -> URI.create("https://repo.polyfrost.org/" + BRANCH.get()));

    // Artifact overrides
    public static final Property<String> ONECONFIG_GROUP =
            property("oneconfig.group", "org.polyfrost");
    public static final Property<String> ONECONFIG_ARTIFACT =
            property("oneconfig.artifact", null);
    public static final Property<String> ONECONFIG_VERSION =
            property("oneconfig.version", null);

    private SystemProperties() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @SuppressWarnings("unchecked")
    private static <T> Property<T> property(String key, Supplier<T> defaultSupplier, T... reified) {
        return new Property<>(key, defaultSupplier, (Class<T>) reified.getClass().getComponentType());
    }

    @SuppressWarnings("unchecked")
    private static <T> Property<T> property(String key, T defaultValue, T... reified) {
        return new Property<>(key, defaultValue, (Class<T>) reified.getClass().getComponentType());
    }

    @Getter
    public static class Property<T> implements Supplier<T> {
        private final String key;
        private final Lazy<T> provider;

        public Property(String key, T defaultValue, Class<T> type) {
            this(key, () -> defaultValue, type);
        }

        public Property(String key, Supplier<T> defaultSupplier, Class<T> type) {
            this.key = PREFIX + key;
            if (defaultSupplier == null) {
                defaultSupplier = () -> null;
            }
            final Supplier<T> finalDefaultSupplier = defaultSupplier;
            this.provider = Lazy.of(() -> {
                String value = System.getProperty(key);
                return value == null ? finalDefaultSupplier.get() : parseProperty(value, type);
            });
        }

        public T get() {
            return provider.get();
        }

        private static <T> T parseProperty(String value, Class<T> type) {
            if (type == String.class) {
                return type.cast(value);
            } else if (type == Byte.class || type == Character.class) {
                return type.cast(value.charAt(0));
            } else if (type == Short.class) {
                return type.cast(Short.parseShort(value));
            } else if (type == Integer.class) {
                return type.cast(Integer.parseInt(value));
            } else if (type == Long.class) {
                return type.cast(Long.parseLong(value));
            } else if (type == Boolean.class) {
                return type.cast(Boolean.parseBoolean(value));
            } else if (type == Float.class) {
                return type.cast(Float.parseFloat(value));
            } else if (type == Double.class) {
                return type.cast(Double.parseDouble(value));
            } else if (type == URI.class) {
                return type.cast(URI.create(value));
            } else if (type == URL.class) {
                throw new UnsupportedOperationException("URL is not supported, use URI instead");
            } else if (type == Path.class) {
                return type.cast(Paths.get(value));
            } else if (type == File.class) {
                throw new UnsupportedOperationException("File is not supported, use Path instead");
            } else {
                throw new IllegalArgumentException("Unsupported property type: " + type);
            }
        }
    }
}
