package org.polyfrost.oneconfig.loader.relaunch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public interface DetectionSupplier {

	/**
	 * Will try to get an instance of {@link DetectionSupplier} using a service loader, otherwise will return a no-op instance.
	 *
	 * @author Deftu
	 * @since 1.1.0-alpha.x
	 */
	static DetectionSupplier maybeCreate() {
		ServiceLoader<DetectionSupplier> loader = ServiceLoader.load(DetectionSupplier.class);
		Iterator<DetectionSupplier> iterator = loader.iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		}

		return DetectionSupplier.DetectionSupplierNoOp.INSTANCE;
	}

	List<Detection> createDetectionList();

	class DetectionSupplierNoOp implements DetectionSupplier {

		public static final DetectionSupplierNoOp INSTANCE = new DetectionSupplierNoOp();

		@Override
		public List<Detection> createDetectionList() {
			return new ArrayList<>(); // No-op
		}

	}

}
