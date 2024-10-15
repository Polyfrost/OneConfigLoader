package org.polyfrost.oneconfig.loader.stage0;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * A {@link net.minecraft.launchwrapper.Launch LaunchWrapper}
 * {@link ITweaker Tweaker} for OneConfig Loader.
 *
 * @author xtrm
 */
@SuppressWarnings("unused")
public class LaunchWrapperTweaker implements ITweaker {
	public LaunchWrapperTweaker() {
		LaunchClassLoader classLoader = Launch.classLoader;

		classLoader.addClassLoaderExclusion("org.polyfrost.oneconfig.loader.base.");
		classLoader.addClassLoaderExclusion("org.polyfrost.oneconfig.loader.utils.");

		new Stage0Loader(new LaunchWrapperCapabilities()).load();
	}

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    //@formatter:off
    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}
    @Override public String getLaunchTarget() { throw new UnsupportedOperationException("Not supposed to get this far."); }
    @Override public String[] getLaunchArguments() { return new String[0]; }
    //@formatter:on
}
