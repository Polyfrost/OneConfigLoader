package org.polyfrost.oneconfig.loader.stage0.relaunch.detection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.launchwrapper.Launch;

import org.polyfrost.oneconfig.loader.stage0.relaunch.RelaunchVersions;

import java.net.URL;
import java.util.List;

@Getter
@Setter
@Log4j2
public class MixinDetection extends SimpleDetection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;

	@Override
	public void checkRelaunch(String id, List<URL> urls) {
		if (!id.equals("org.spongepowered:mixin") && !id.endsWith(":UniMix")) {
			return;
		}
		this.detectedUrls = urls;
		String loadedVersion = String.valueOf(Launch.blackboard.get("mixin.initialised"));
		String bundledVersion = RelaunchVersions.getMixinVersion(urls);
		log.debug("Found Mixin {} loaded, we bundle {}", loadedVersion, bundledVersion);
		if (RelaunchVersions.compare("mixin", loadedVersion, bundledVersion) < 0) {
			log.warn("Found an old version of Mixin ({}). This may cause issues.", loadedVersion);
			this.relaunch = true;
		}
	}
}
