package org.polyfrost.oneconfig.loader.stage0;

import org.polyfrost.oneconfig.loader.ILoader;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

/**
 * A {@link net.minecraft.launchwrapper.Launch LaunchWrapper}
 * {@link ITweaker Tweaker} for OneConfig Loader.
 *
 * @author xtrm
 */
public class LaunchWrapperTweaker implements ITweaker {
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        ILoader.Capabilities capabilities = new LaunchWrapperCapabilities(classLoader);
        new Stage0Loader(capabilities).load();
    }

    //@formatter:off
    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}
    @Override public String getLaunchTarget() { throw new UnsupportedOperationException("Not supposed to get this far."); }
    @Override public String[] getLaunchArguments() { return new String[0]; }
    //@formatter:on
}
