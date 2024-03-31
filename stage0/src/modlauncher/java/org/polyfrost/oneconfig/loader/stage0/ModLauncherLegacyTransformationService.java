package org.polyfrost.oneconfig.loader.stage0;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.ILoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A ModLauncher transformation service provided by oneconfig-loader/stage0.
 *
 * @author xtrm
 * @since 1.1.0
 */
public class ModLauncherLegacyTransformationService implements ITransformationService, Runnable {
    @Override
    public void run() {
        ILoader.Capabilities capabilities = new ModLauncherCapabilities();
        new Stage0Loader(capabilities).load();
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

    //@formatter:off
    @Override public @NotNull String name() { return "oneconfig-loader"; }
    @Override public void initialize(IEnvironment environment) {}
    @SuppressWarnings("rawtypes") @Override public @NotNull List<ITransformer> transformers() { return new ArrayList<>(); }
    @Override public void beginScanning(IEnvironment environment) { run(); }
    //@formatter:on
}
