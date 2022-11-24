package cc.polyfrost.oneconfigloader;

/**
 * The old package for OneConfigLoader. This is here for backwards compatibility with older wrapper versions.
 * @deprecated Use {@link cc.polyfrost.oneconfig.loader.OneConfigLoader} instead.
 */
@Deprecated
public class OneConfigLoader extends cc.polyfrost.oneconfig.loader.OneConfigLoader {
    static {
        System.out.println("One of your mods are using an outdated version of OneConfigWrapper. Please update to the latest version.");
    }
}