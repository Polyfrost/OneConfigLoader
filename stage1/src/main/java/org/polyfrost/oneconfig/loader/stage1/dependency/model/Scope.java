package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum Scope {
    RUNTIME("runtime"),
    COMPILE("compile"),
    PROVIDED("provided"),
    TEST("test"),
    UNKNOWN("unknown");

    private final String title;

    public static Scope fromTitle(String title) {
        for (Scope scope : values()) {
            if (scope.title.equalsIgnoreCase(title)) {
                return scope;
            }
        }
        return UNKNOWN;
    }
}
