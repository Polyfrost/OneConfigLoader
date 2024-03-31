package org.polyfrost.oneconfig.loader.stage0.j9;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class ModLauncherTransformationService implements ITransformationService {
    private static final String LEGACY_SERVICE_CLASS =
            "org.polyfrost.oneconfig.loader.stage0.ModLauncherLegacyTransformationService";
    private final ITransformationService delegate;

    public ModLauncherTransformationService() {
        ITransformationService delegate;
        try {
            delegate = (ITransformationService) Class.forName(LEGACY_SERVICE_CLASS)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate legacy transformation service", e);
        }
        this.delegate = delegate;
    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        ((Runnable) delegate).run();
        return ITransformationService.super.beginScanning(environment);
    }

    @Override
    public @NotNull String name() {
        return delegate.name();
    }

    @Override
    public void initialize(IEnvironment iEnvironment) {
        delegate.initialize(iEnvironment);
    }

    @Override
    public void onLoad(IEnvironment iEnvironment, Set<String> set) throws IncompatibleEnvironmentException {
        delegate.onLoad(iEnvironment, set);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        return delegate.transformers();
    }
}
