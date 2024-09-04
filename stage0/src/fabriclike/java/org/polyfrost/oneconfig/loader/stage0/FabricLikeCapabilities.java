package org.polyfrost.oneconfig.loader.stage0;

import lombok.Getter;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class FabricLikeCapabilities implements Capabilities {
    private final ClassLoader classLoader;
	private final RuntimeAccess runtimeAccess;
	private final GameMetadata gameMetadata = FabricLikeGameMetadata.INSTANCE;

	public FabricLikeCapabilities(ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.runtimeAccess = new FabricLikeRuntimeAccess(classLoader);
	}
}
