package org.polyfrost.oneconfig.loader.relaunch.detection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.util.List;

@Getter
@Setter
@Log4j2
public class Lwjgl2Detection extends SimpleDetection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;

	@Override
	public void checkRelaunch(String id, List<URL> urls) {
		if (!id.startsWith("org.lwjgl.lwjgl")) {
			return;
		}
		this.detectedUrls = urls;
		this.relaunch = true;
		log.warn("Relaunching with Legacy-Fabric's LWJGL fork...");
	}
}
