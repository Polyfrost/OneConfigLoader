package org.polyfrost.oneconfig.loader.stage0;

import java.net.URL;

import org.jetbrains.annotations.NotNull;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class ModLauncherRuntimeAccess implements Capabilities.RuntimeAccess {
	@Override
	public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
		//TODO: Add to a list/set that's passed on to the TransformationService's runScan thingy
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public ClassLoader getClassLoader() {
		return ModLauncherCapabilities.class.getClassLoader();
	}
}
