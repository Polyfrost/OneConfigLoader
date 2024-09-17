package org.polyfrost.oneconfig.loader.stage1;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import lombok.Getter;
import net.minecraftforge.common.ForgeVersion;
import org.jetbrains.annotations.NotNull;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * {@link Capabilities} implementation for initialization through legacy entrypoints, which all use
 * <a href="https://github.com/Mojang/LegacyLauncher">LaunchWrapper</a> through MinecraftForge 1.8.9 or 1.12.2.
 *
 * @deprecated This is a legacy entrypoint, and will be removed in the future.
 *
 * @author xtrm
 * @since 1.1.0
 */
@Getter
@Deprecated
public class LegacyCapabilities implements Capabilities {
	private final RuntimeAccess runtimeAccess;
	private final GameMetadata gameMetadata;

	public LegacyCapabilities(File gameDir) {
		this.runtimeAccess = fetchRuntimeAccess();
		if (gameDir == null) {
			gameDir = new File(".");
		}
		this.gameMetadata = fetchGameMetadata(gameDir.toPath().toAbsolutePath());
	}

	public RuntimeAccess fetchRuntimeAccess() {
		return new RuntimeAccess() {
			@Override
			public void appendToClassPath(boolean mod, @NotNull URL @NotNull ... urls) {
				for (@NotNull URL url : urls) {
					Launch.classLoader.addURL(url);
				}
			}

			@Override
			public ClassLoader getClassLoader() {
				return Launch.classLoader;
			}
		};
	}

	public GameMetadata fetchGameMetadata(Path gameDir) {
		return new GameMetadata() {
			@Override
			public Path getGameDir() {
				return gameDir;
			}

			@Override
			public String getGameVersion() {
				return ForgeVersion.mcVersion;
			}

			@Override
			public String getLoaderName() {
				return "forge";
			}
		};
	}
}
