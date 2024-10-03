package org.polyfrost.oneconfig.loader.relaunch.detection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.polyfrost.oneconfig.loader.relaunch.RelaunchVersions;

import java.net.URL;
import java.util.List;

@Getter
@Setter
@Log4j2
public class AsmDetection extends SimpleDetection {
	private boolean relaunch = false;
	private List<URL> detectedUrls = null;

	@Override
	public void checkRelaunch(String id, List<URL> urls) {
		if (!id.startsWith("org.ow2.asm")) {
			return;
		}
		this.detectedUrls = urls;
		String loadedVersion = org.objectweb.asm.ClassWriter.class.getPackage().getImplementationVersion();
		String bundledVersion = RelaunchVersions.getAsmVersion(urls);
		log.debug("Found ASM {} loaded, we bundle {}", loadedVersion, bundledVersion);
		if (RelaunchVersions.compare("ASM", loadedVersion, bundledVersion) < 0) {
			this.relaunch = true;
			log.warn("Found an old version of ASM ({}). This may cause issues.", loadedVersion);
		}
	}
}
