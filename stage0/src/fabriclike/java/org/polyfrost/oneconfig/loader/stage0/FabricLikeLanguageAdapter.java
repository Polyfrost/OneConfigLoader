package org.polyfrost.oneconfig.loader.stage0;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import org.polyfrost.oneconfig.loader.ILoader;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class FabricLikeLanguageAdapter implements LanguageAdapter {
    static {
        ILoader.Capabilities capabilities = new FabricLikeCapabilities(
                FabricLikeLanguageAdapter.class.getClassLoader()
        );
        new Stage0Loader(capabilities).load();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
