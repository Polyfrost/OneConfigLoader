/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polyfrost.oneconfig.loader.stage0.version;

/**
 * Represents a version of a mod.
 */
public interface Version extends Comparable<Version> {
	/**
	 * Returns the user-friendly representation of this version.
	 */
	String getFriendlyString();

	/**
	 * Parses a version from a string notation.
	 *
	 * @param s the string notation of the version
	 * @return the parsed version
	 */
	static Version parse(String s) throws IllegalArgumentException {
		if (s == null || s.isEmpty()) {
			throw new IllegalArgumentException("Version must be a non-empty string!");
		}

		return new SemanticVersionImpl(s, false);
	}
}
