package org.polyfrost.oneconfig.loader.stage1.dependency.cache;

import java.nio.file.Path;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface CachingSolution {
    boolean canBeCached(Path path);
}
