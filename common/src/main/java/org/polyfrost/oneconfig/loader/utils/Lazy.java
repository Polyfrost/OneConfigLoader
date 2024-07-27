package org.polyfrost.oneconfig.loader.utils;

import java.util.function.Supplier;

/**
 * Lazy provider for a value.
 *
 * @param <T> the type of the value
 *
 * @author xtrm
 * @since 1.1.0
 */
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private transient T value;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public synchronized T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }
}
