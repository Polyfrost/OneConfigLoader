package cc.polyfrost.oneconfig.loader;

/**
 * A loader metadata container trait.
 *
 * @author xtrm
 */
public interface IMetaHolder {
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

    /**
     * Returns the loader's name.
     *
     * @return the loader's name
     */
    String getName();

    /**
     * Returns the loader's version.
     *
     * @return the loader's version
     */
    String getVersion();
}
