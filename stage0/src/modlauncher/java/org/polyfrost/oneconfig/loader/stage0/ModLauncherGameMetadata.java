package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;

import net.neoforged.neoforgedspi.Environment;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
public enum ModLauncherGameMetadata implements Capabilities.GameMetadata {
	INSTANCE;

	@Override
	public Path getGameDir() {
		return Launcher.INSTANCE.environment()
				.getProperty(IEnvironment.Keys.GAMEDIR.get())
				.orElseThrow(() -> new IllegalStateException(
						"Game directory key not found in ModLauncher environment"
				));
	}

	@Override
	public String getGameVersion() {
		return Launcher.INSTANCE.environment()
				.getProperty(IEnvironment.Keys.VERSION.get())
				.orElseThrow(() -> new IllegalStateException(
						"Game version key not found in ModLauncher environment"
				));
	}

	@Override
	public String getLoaderName() {
		try {
			//noinspection ResultOfMethodCallIgnored
			Environment.class.hashCode();
			return "neoforge";
		} catch (Throwable ignored) {
		}
		return "forge";
	}
}
