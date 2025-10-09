package org.polyfrost.oneconfig.loader.stage1;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.FMLRelaunchLog;

import org.polyfrost.oneconfig.loader.base.LoaderBase;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * When launched in a dev environment via a `--tweakClass` argument, we will load in an earlier cycle than in production
 * (where only the FMLTweaker loads in the first cycle, and it then queues us for the second one). This means that if
 * there is any OneConfig in the mods folder, that those are not yet on the classpath at this time, which in turn means
 * that our platform setup code does not consider them, meaning they won't load as mods.
 * To work around that (and get dev closer to production), if we detect that we are in the first cycle, we will instead
 * queue this tweaker for the second one (as a fake stage0) and then proceed as usual from there.
 *
 * Code taken and adapted from EssentialLoader, under the GPL-3.0 license.
 * https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE
 */

public class DelayedStage0Tweaker implements ITweaker {
	private static final String FML_TWEAKER = "net.minecraftforge.fml.common.launcher.FMLTweaker";
	private static final String COMMAND_LINE_COREMODS_PROP = "fml.coreMods.load";

	private static String[] commandLineCoremods; // we also delay these cause they may depend on our stuff
	private final LoaderBase stage1;

	@SuppressWarnings("unused")
	public DelayedStage0Tweaker() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		(this.stage1 = new Stage1Loader()).load();

		for (String commandLineCoremod : commandLineCoremods) {
			FMLRelaunchLog.info("Found a command line coremod : %s", commandLineCoremod);

			Method loadCoreMod = Class.forName("net.minecraftforge.fml.relauncher.CoreModManager").getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
			loadCoreMod.setAccessible(true);
			ITweaker tweaker = (ITweaker) loadCoreMod.invoke(null, Launch.classLoader, commandLineCoremod, null);

			if (tweaker != null) {
				@SuppressWarnings("unchecked")
				List<ITweaker> tweakers = ((List<ITweaker>) Launch.blackboard.get("Tweaks"));
				tweakers.add(tweaker);
			}
		}
	}

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		if (commandLineCoremods.length > 0) {
			// Temporarily restore these in case we need to re-launch (they do not need to be delayed in that case)
			System.setProperty(COMMAND_LINE_COREMODS_PROP, String.join(",", commandLineCoremods));
		}
		this.stage1.postLoad();
		System.clearProperty(COMMAND_LINE_COREMODS_PROP);
	}

	@Override public String getLaunchTarget() { throw new UnsupportedOperationException("Not supposed to get this far."); }
	@Override public String[] getLaunchArguments() { return new String[0]; }

	public static boolean isRequired() {
		@SuppressWarnings("unchecked")
		List<ITweaker> currentCycle = (List<ITweaker>) Launch.blackboard.get("Tweaks");
		return currentCycle.stream().anyMatch(it -> it.getClass().getName().equals(FML_TWEAKER));
	}

	public static void prepare() {
		String commandLineCoremodsStr = System.getProperty(COMMAND_LINE_COREMODS_PROP, "");
		commandLineCoremods = commandLineCoremodsStr.isEmpty() ? new String[0] : commandLineCoremodsStr.split(",");
		System.clearProperty(COMMAND_LINE_COREMODS_PROP);
	}

	public static void inject() {
		@SuppressWarnings("unchecked")
		List<String> nextCycle = (List<String>) Launch.blackboard.get("TweakClasses");
		nextCycle.add(DelayedStage0Tweaker.class.getName());

		// Tweaker arguments are consumed by Launch.launch, so when relaunching we assume the FMLTweaker to be the only
		// one passed in (as common for production). However, if we end up here, then the OneConfig tweaker has also
		// been passed (as common for dev) next to the FMLTweaker (rather than being chain-loaded by it).
		// If we do not re-add ourselves to the tweaker list when we re-launch, then we may not get called at all (or
		// too late if there are command line supplied coremods relying on us), so we add ourselves to the launchArgs
		// which FMLTweaker makes available (and which we use in Relaunch to take an educated guess at the original
		// arguments).
		@SuppressWarnings("unchecked")
		Map<String, String> launchArgs = (Map<String, String>) Launch.blackboard.get("launchArgs");
		String prevValue = launchArgs.put("--tweakClass", "org.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker");
		if (prevValue != null) {
			throw new UnsupportedOperationException("Cannot re-register OneConfig tweaker because \""
					+ prevValue + "\" was already there. This will require a more complex implementation.");
		}
	}
}
