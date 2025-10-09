package org.polyfrost.oneconfig.loader.stage0;

import org.polyfrost.oneconfig.loader.base.Capabilities;

public class LaunchWrapperCapabilities implements Capabilities {
	@Override
	public RuntimeAccess getRuntimeAccess() {
		return null;
	}

	@Override
	public GameMetadata getGameMetadata() {
		return null;
	}
}
