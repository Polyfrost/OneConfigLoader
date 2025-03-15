package org.polyfrost.oneconfig.loader.relaunch;

import org.polyfrost.oneconfig.loader.relaunch.detection.AsmDetection;
import org.polyfrost.oneconfig.loader.relaunch.detection.ExcludedKotlinDetection;
import org.polyfrost.oneconfig.loader.relaunch.detection.MixinDetection;
import org.polyfrost.oneconfig.loader.relaunch.detection.PreloadLibraryDetection;
import org.polyfrost.oneconfig.loader.relaunch.detection.SignedMixinDetection;

import java.util.ArrayList;
import java.util.List;

public class DetectionSupplierImpl implements DetectionSupplier {

	public List<Detection> createDetectionList() {
		return new ArrayList<Detection>() {
			{
				add(new AsmDetection());
				add(new ExcludedKotlinDetection());
				add(new SignedMixinDetection());
				addAll(createComplexPreloadLibraryDetectionList());
				add(new PreloadLibraryDetection("polymixin-", "org", "spongepowered"));
				add(new MixinDetection());
			}
		};
	}

	private List<Detection> createComplexPreloadLibraryDetectionList() {
		return new ArrayList<Detection>() {
			{
				String[][][] paths = {
						{{"kotlin-"}, {"kotlin"}},
						{{"kotlinx-coroutines-"}, {"kotlinx", "coroutines"}},
						{{"dev.deftu:omnicore-"}, {"dev", "deftu", "omnicore"}},
						{{"polyui-"}, {"org", "polyfrost", "polyui"}}
				};

				for (String[][] path : paths) {
					add(new PreloadLibraryDetection(path[0][0], path[1]));
				}
			}
		};
	}

}
