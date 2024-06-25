package org.polyfrost.oneconfig.loader.stage1.dependency.model;

import java.net.URI;

/**
 * @author xtrm
 * @since 1.1.0
 */
public interface RemoteResolver {
    URI resolve(Artifact artifact);
}
