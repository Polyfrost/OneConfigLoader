package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public enum LaunchWrapperGameMetadata implements Capabilities.GameMetadata {
	INSTANCE;

	private final String loaderName;
	private final String gameVersion;

	LaunchWrapperGameMetadata() {
		boolean isFabric = false;
		try {
			//noinspection ResultOfMethodCallIgnored
			FabricLoader.class.hashCode();
			isFabric = true;
		} catch (Throwable ignored) {
		}
		this.loaderName = isFabric ? "fabric" : "forge";
		try {
			this.gameVersion = fetchGameVersion(isFabric);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to fetch game version", t);
		}
	}

	@Override
	public Path getGameDir() {
		return Launch.minecraftHome.toPath();
	}

	private static String fetchGameVersion(boolean isFabric) {
		if (isFabric) {
			FabricLoader loader = FabricLoader.getInstance();
			ModContainer container = loader.getModContainer("minecraft").orElseThrow(() -> new RuntimeException("Failed to find 'minecraft' fabric mod container"));
			Version version = container.getMetadata().getVersion();
			return version.getFriendlyString();
		} else {
			try {
				// 1.8.8 +
				return ForgeVersion.mcVersion;
			} catch (Throwable t) {
				try {
					// 1.7.x - 1.8
					return MinecraftForge.MC_VERSION;
				} catch (Throwable t2) {
					throw new RuntimeException("Failed to fetch game version", t);
				}
			}
		}
	}
}
