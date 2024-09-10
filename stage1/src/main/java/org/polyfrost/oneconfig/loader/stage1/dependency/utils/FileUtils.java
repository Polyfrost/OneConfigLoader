package org.polyfrost.oneconfig.loader.stage1.dependency.utils;

import java.net.URI;
import java.net.URLEncoder;

public class FileUtils {

	public static URI encodePath(String path) {
		try {
			StringBuilder builder = new StringBuilder();

			for (String part : path.split("/")) {
				builder.append(URLEncoder.encode(part, "UTF-8")).append("/");
			}

			return URI.create(builder.toString());
		} catch (Throwable t) {
			throw new RuntimeException("Could not encode path " + path, t);
		}
	}

}
