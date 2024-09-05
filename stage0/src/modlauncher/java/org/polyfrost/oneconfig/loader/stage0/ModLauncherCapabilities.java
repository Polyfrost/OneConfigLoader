package org.polyfrost.oneconfig.loader.stage0;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import lombok.Getter;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class ModLauncherCapabilities implements Capabilities {
	public static final ModLauncherCapabilities INSTANCE = new ModLauncherCapabilities();

    static final int MODLAUNCHER_VERSION;
	private final RuntimeAccess runtimeAccess = new ModLauncherRuntimeAccess();
	private final GameMetadata gameMetadata = ModLauncherGameMetadata.INSTANCE;

	@Override
    public void provideLogAppender(Consumer<Appender> appenderConsumer) {
		Appender appender;
        try {
            appender = StartupNotificationManager.modLoaderConsumer()
                    .map(consumer -> new BaseAppender("neoforged", consumer))
                    .orElse(null);
        } catch (NoClassDefFoundError ignored) {
            try {
                appender = StartupMessageManager.modLoaderConsumer()
                        .map(consumer -> new BaseAppender("fml", consumer))
                        .orElse(null);
            } catch (NoClassDefFoundError ignored2) {
                try {
                    appender = net.minecraftforge.fml.loading.progress.StartupNotificationManager.modLoaderConsumer()
                            .map(consumer -> new BaseAppender("fml", consumer))
                            .orElse(null);
                } catch (NoClassDefFoundError ignored3) {
                    // might be 1.13.x
                    LogManager.getLogger(getClass()).warn("No startup message manager found");
					return;
                }
            }
        }
		appenderConsumer.accept(appender);
    }

    static {
        int version;
        try {
            version = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MLSPEC_VERSION.get())
                    .map(Object::toString)
                    .map(it -> it.split(Pattern.quote("."))[0])
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new IllegalStateException("ModLauncher version not found"));
        } catch (NoClassDefFoundError ex2) {
            try {
                version = Integer.parseInt(IEnvironment.class.getPackage().getSpecificationVersion().split(Pattern.quote("."))[0]);
            } catch (Exception ex) {
                throw new IllegalStateException("Unknown ModLauncher version", ex2);
            }
        }
        MODLAUNCHER_VERSION = version;
    }
}
