package cc.polyfrost.oneconfig.loader;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * @author xtrm
 */
public abstract @Data class LoaderBase implements ILoader {
    private final String name;
    private final String version;

    @NotNull
    public abstract ILoader getNextLoader();

    @Override
    public void load() {
        ILoader nextLoader = getNextLoader();
    }
}
