package org.polyfrost.oneconfig.loader;

/**
 * A loader metadata trait, describing information about a specific loader and/or loader stage.
 * <p>
 * Note that this is regarding OneConfig Loader's metadata, not an underlying game modloader.
 *
 * @author xtrm
 */
public interface IMetaHolder {
    /**
     * @return the loader's name
     */
    String getName();

    /**
     * @return the loader's version
     */
    String getVersion();

    /**
     * Creates a {@link IMetaHolder} instance from the given params.
     *
     * @param name    the loader's name
     * @param version the loader's version
     * @return a new {@link IMetaHolder} instance
     */
    static IMetaHolder of(String name, String version) {
        return new IMetaHolder() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getVersion() {
                return version;
            }
        };
    }
}
