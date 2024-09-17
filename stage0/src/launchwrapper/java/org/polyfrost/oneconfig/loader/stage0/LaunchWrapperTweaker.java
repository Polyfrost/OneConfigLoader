package org.polyfrost.oneconfig.loader.stage0;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * A {@link net.minecraft.launchwrapper.Launch LaunchWrapper}
 * {@link ITweaker Tweaker} for OneConfig Loader.
 *
 * @author xtrm
 */
@SuppressWarnings("unused")
public class LaunchWrapperTweaker implements ITweaker {
	private static final Logger log = LogManager.getLogger(LaunchWrapperTweaker.class);
	static File gameDir;

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		LaunchWrapperTweaker.gameDir = gameDir;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		classLoader.addClassLoaderExclusion("org.polyfrost.oneconfig.loader.");

		new Stage0Loader(new LaunchWrapperCapabilities()).load();
	}

	//@formatter:off
    @Override public String getLaunchTarget() { throw new UnsupportedOperationException("Not supposed to get this far."); }
    @Override public String[] getLaunchArguments() { return new String[0]; }
    //@formatter:on
}
