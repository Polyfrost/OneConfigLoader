package cc.polyfrost.oneconfig.loader.stage0;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A transformation service provided by oneconfig-loader/stage0.
 *
 * @author xtrm
 */
@ParametersAreNonnullByDefault
public class ModLoaderTransformationService implements ITransformationService {
    @Override
    public @NotNull String name() {
        return "oneconfig-loader";
    }

    @Override
    public void initialize(IEnvironment environment) {

    }

    @Override
    public void beginScanning(IEnvironment environment) {

    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @SuppressWarnings({"rawtypes"})
    @NotNull
    @Override
    public List<ITransformer> transformers() {
        return new ArrayList<>();
    }
}
