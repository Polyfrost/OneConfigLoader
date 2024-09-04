package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

import net.fabricmc.loader.api.ModContainer;

import net.fabricmc.loader.api.Version;

import org.polyfrost.oneconfig.loader.base.Capabilities;

/**
 * @author xtrm
 * @since 1.1.0
 */
@Getter
public enum FabricLikeGameMetadata implements Capabilities.GameMetadata {
	INSTANCE;

	private final String loaderName;
	private final String gameVersion;

	FabricLikeGameMetadata() {
		this.loaderName = "fabric";
		ModContainer minecraftContainer = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(() -> new RuntimeException("Minecraft mod container not found"));
		Version version = minecraftContainer.getMetadata().getVersion();
		this.gameVersion = version.getFriendlyString();
	}

	@Override
	public Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}
}
