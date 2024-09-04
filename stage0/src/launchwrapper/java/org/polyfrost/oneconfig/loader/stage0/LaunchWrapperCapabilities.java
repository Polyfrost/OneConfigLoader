package org.polyfrost.oneconfig.loader.stage0;

import lombok.Getter;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * LaunchWrapper Capabilities for OneConfig Loader.
 * <p>
 * LaunchWrapper loading is surprisingly simple, as it only requires the
 * addition of URLs to the classpath, both for libraries and mods.
 *
 * @see LaunchWrapperRuntimeAccess
 * @see LaunchWrapperGameMetadata
 *
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class LaunchWrapperCapabilities implements Capabilities {
	private final RuntimeAccess runtimeAccess = LaunchWrapperRuntimeAccess.INSTANCE;
	private final GameMetadata gameMetadata = LaunchWrapperGameMetadata.INSTANCE;
}
