package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;

import lombok.Getter;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public class LaunchWrapperGameMetadata implements Capabilities.GameMetadata {
	public static final LaunchWrapperGameMetadata INSTANCE = new LaunchWrapperGameMetadata();

	private final String loaderName;
	private final String gameVersion;

	LaunchWrapperGameMetadata() {
		this.loaderName = "forge";
		try {
			this.gameVersion = fetchGameVersion();
		} catch (Throwable t) {
			throw new RuntimeException("Failed to fetch game version", t);
		}
	}

	@Override
	public Path getGameDir() {
		return Launch.minecraftHome.toPath();
	}

	private String fetchGameVersion() {
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
