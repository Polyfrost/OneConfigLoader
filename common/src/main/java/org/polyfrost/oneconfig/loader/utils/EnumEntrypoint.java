package org.polyfrost.oneconfig.loader.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
@ToString
public enum EnumEntrypoint {
    LAUNCHWRAPPER("LaunchWrapper"),
    MODLAUNCHER("ModLauncher"),
    FABRICLIKE("Fabric-Like"),
    ;

    private final String id;
}
