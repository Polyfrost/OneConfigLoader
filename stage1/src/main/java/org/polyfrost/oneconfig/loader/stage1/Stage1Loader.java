package org.polyfrost.oneconfig.loader.stage1;

import lombok.SneakyThrows;
import org.polyfrost.oneconfig.loader.LoaderBase;
import org.polyfrost.oneconfig.loader.stage1.dependency.DependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.maven.MavenDependencyManager;
import org.polyfrost.oneconfig.loader.stage1.dependency.model.Artifact;
import org.polyfrost.oneconfig.loader.stage1.util.SystemProperties;
import org.polyfrost.oneconfig.loader.stage1.util.XDG;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class Stage1Loader extends LoaderBase {
    private final XDG.ApplicationStore applicationStore;
    private final DependencyManager<MavenArtifact> dependencyManager;

    @SneakyThrows
    public Stage1Loader(Capabilities capabilities) {
        super(
                "stage1",
                Stage1Loader.class.getPackage().getImplementationVersion(),
                capabilities
        );
        this.applicationStore = XDG.provideApplicationStore("Polyfrost/OneConfig/loader");
        this.dependencyManager = new MavenDependencyManager(
                applicationStore,
                SystemProperties.REPOSITORY_URL.get()
        );
    }

    @Override
    public void load() {
        // Fetch oneconfig version info
        Artifact oneconfigArtifact = dependencyManager.fetchArtifact();
        // Lookup dependencies metadata
        // Download to cache
        // Delegate everything to OneConfig
    }
}
