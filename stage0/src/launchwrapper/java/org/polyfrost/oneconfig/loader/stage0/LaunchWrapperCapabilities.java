package org.polyfrost.oneconfig.loader.stage0;

import lombok.Data;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.base.LoaderBase;
import org.polyfrost.oneconfig.loader.utils.EnumEntrypoint;

import java.net.URL;
import java.nio.file.Path;

/**
 * LaunchWrapper Capabilities for OneConfig Loader.
 * <p>
 * LaunchWrapper loading is surprisingly simple, as it only requires the
 * addition of URLs to the classpath, both for libraries and mods.
 *
 * @author xtrm
 * @since 1.1.0
 */
public @Data class LaunchWrapperCapabilities implements LoaderBase.Capabilities {
    private final EnumEntrypoint entrypointType = EnumEntrypoint.LAUNCHWRAPPER;
    private final LaunchClassLoader launchClassLoader;

    @Override
    public void appendToClassPath(boolean mod, URL @NotNull ... urls) {
        for (URL url : urls) {
            launchClassLoader.addURL(url);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return launchClassLoader;
    }

    @Override
    public Path getGameDir() {
        return Launch.minecraftHome.toPath();
    }

	public String getModLoaderName() {
		try {
			// If ForgeVersion is present, we're running Forge.
			Class.forName("net.minecraftforge.common.ForgeVersion");
			return "forge";
		} catch (Throwable ignored) {
			try {
				// If FabricLoader is present, we're running Fabric.
				Class.forName("net.fabricmc.loader.api.FabricLoader");
				return "fabric";
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String getGameVersion() {
		String modLoaderName = getModLoaderName();
		try {
			switch (modLoaderName) {
				case "forge":
					// Get the Minecraft version from ForgeVersion.
					Class<?> forgeVersion = Class.forName("net.minecraftforge.common.ForgeVersion");
					return (String) forgeVersion.getDeclaredField("mcVersion").get(null);

				case "fabric":
					// Get the Minecraft version from FabricLoader.
					Class<?> fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader");
					Object fabricInstance = fabricLoader.getDeclaredMethod("getInstance").invoke(null);
					Object minecraftModContainer = fabricLoader.getDeclaredMethod("getModContainer", String.class).invoke(fabricInstance, "minecraft");
					Object metadata = minecraftModContainer.getClass().getMethod("getMetadata").invoke(minecraftModContainer);
					Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
					return (String) version.getClass().getMethod("getFriendlyString").invoke(version);

				default:
					throw new IllegalStateException("Unknown mod loader: " + modLoaderName);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
