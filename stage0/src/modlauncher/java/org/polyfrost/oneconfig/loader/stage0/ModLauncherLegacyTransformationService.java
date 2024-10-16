package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;

/**
 * A ModLauncher transformation service provided by oneconfig-loader/stage0.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class ModLauncherLegacyTransformationService implements ITransformationService, Runnable {
    static {
        System.out.println("Loaded ModLauncherLegacyTransformationService");
        ModLauncherHijack.INSTANCE.injectLaunchPluginService();
    }

    @Override
    public void run() {
        Stage0Loader loader = new Stage0Loader(ModLauncherCapabilities.INSTANCE);
		loader.load();
		loader.postLoad();
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        // NeoForge uses fml (for now at least)
        String[] required = {"fmlclient", "forge_client", "fmlclientuserdev", "fml"};
        for (String service : required) {
            if (otherServices.contains(service)) {
                return;
            }
        }

        throw new IncompatibleEnvironmentException(
                String.format(
                        "Missing one of the required launch handlers: [%s], only [%s] found.",
                        String.join(", ", required),
                        String.join(", ", otherServices)
                )
        );
    }

    @Override
    public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
        System.out.println("runScan");
        return ITransformationService.super.runScan(environment);
//        return Collections.singletonList(
//                new AbstractMap.SimpleEntry<>(
//                        "oneconfig-loader",
//                        Paths.get("oneconfig-loader.jar")
//                )
//        );
    }

    //@formatter:off
    @Override public @NotNull String name() { return "oneconfig-loader"; }
    @Override public void initialize(IEnvironment environment) {}
    @SuppressWarnings("rawtypes") @Override public @NotNull List<ITransformer> transformers() { return new ArrayList<>(); }
    @Override public void beginScanning(IEnvironment environment) { System.out.println("beginScanning"); run(); }
    //@formatter:on
}
