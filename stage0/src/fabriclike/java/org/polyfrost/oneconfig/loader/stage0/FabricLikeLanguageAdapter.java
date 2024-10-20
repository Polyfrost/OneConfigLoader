package org.polyfrost.oneconfig.loader.stage0;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@SuppressWarnings("unused")
public class FabricLikeLanguageAdapter implements LanguageAdapter {
    static {
		ClassLoader classLoader = FabricLikeLanguageAdapter.class.getClassLoader();
        Capabilities capabilities = new FabricLikeCapabilities(classLoader);
		Stage0Loader loader = new Stage0Loader(capabilities);
		loader.load();
		loader.postLoad();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
