package cc.polyfrost.oneconfig.loader.stage0;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A ModLauncher transformation service provided by oneconfig-loader/stage0.
 *
 * @author xtrm
 */
@SuppressWarnings("NullableProblems")
public class ModLauncherTransformationService implements ITransformationService {
    @Override
    public String name() {
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
    @Override
    public List<ITransformer> transformers() {
        return new ArrayList<>();
    }
}
