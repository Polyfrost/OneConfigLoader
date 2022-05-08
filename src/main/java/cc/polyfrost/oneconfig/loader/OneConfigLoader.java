package cc.polyfrost.oneconfig.loader;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;


public class OneConfigLoader implements IFMLLoadingPlugin {
    private IFMLLoadingPlugin loader = null;

    @Override
    public String[] getASMTransformerClass() {
        return loader.getASMTransformerClass();
    }

    @Override
    public String getModContainerClass() {
        return loader.getModContainerClass();
    }

    @Override
    public String getSetupClass() {
        return loader.getSetupClass();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        loader.injectData(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return loader.getAccessTransformerClass();
    }
}
