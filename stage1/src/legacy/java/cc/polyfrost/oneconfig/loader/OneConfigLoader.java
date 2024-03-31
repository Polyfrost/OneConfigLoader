package cc.polyfrost.oneconfig.loader;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.polyfrost.oneconfig.loader.stage1.LegacyCapabilities;
import org.polyfrost.oneconfig.loader.stage1.Stage1Loader;

import java.io.File;
import java.util.List;

/**
 * Entry point for legacy stage0 wrappers that target the wrong class.
 *
 * @author xtrm
 * @since 1.1.0
 *
 * @see LegacyCapabilities
 */
@Deprecated
public class OneConfigLoader extends Stage1Loader implements ITweaker {
    public OneConfigLoader() {
        super(new LegacyCapabilities());
    }

    static {
        LogManager.getLogger(OneConfigLoader.class)
                .warn("One of your mods is using the legacy OneConfigLoader " +
                        "class, due to having an outdated stage0 wrapper " +
                        "dependency.");
    }

    //@formatter:off
    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}
    @Override public void injectIntoClassLoader(LaunchClassLoader classLoader) {}
    @Override public String getLaunchTarget() { return null; }
    @Override public String[] getLaunchArguments() { return new String[0]; }
    //@formatter:on
}
