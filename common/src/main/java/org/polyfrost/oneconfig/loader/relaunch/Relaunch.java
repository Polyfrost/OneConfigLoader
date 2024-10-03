package org.polyfrost.oneconfig.loader.relaunch;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author Deftu
 * @since 1.1.0-alpha.x
 */
public interface Relaunch {

	/**
	 * Will try to get an instance of {@link Relaunch} using a service loader, otherwise will return a no-op instance.
	 *
	 * @author Deftu
	 * @since 1.1.0-alpha.x
	 */
	static Relaunch maybeCreate() {
		ServiceLoader<Relaunch> loader = ServiceLoader.load(Relaunch.class);
		Iterator<Relaunch> iterator = loader.iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		}

		return RelaunchNoOp.INSTANCE;
	}

	/**
	 * @author Deftu
	 * @since 1.1.0-alpha.x
	 */
	void maybeRelaunch(DetectionSupplier detectionSupplier, Map<String, List<URL>> urls);

	/**
	 * @author Deftu
	 * @since 1.1.0-alpha.x
	 */
	class RelaunchNoOp implements Relaunch {

		public static final RelaunchNoOp INSTANCE = new RelaunchNoOp();

		@Override
		public void maybeRelaunch(DetectionSupplier detectionSupplier, Map<String, List<URL>> urls) {
			// No-op
		}

	}

}
