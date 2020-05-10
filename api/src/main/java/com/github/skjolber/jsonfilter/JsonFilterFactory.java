/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.github.skjolber.jsonfilter;

public interface JsonFilterFactory {

	public static final String MAX_STRING_LENGTH = "com.skjolberg.jsonfilter.maxStringLength";
	public static final String PRUNE = "com.skjolberg.jsonfilter.prune";
	public static final String ANONYMIZE = "com.skjolberg.jsonfilter.anonymize";
	public static final String MAX_PATH_MATCHES = "com.skjolberg.jsonfilter.maxPathMatches";
	
	/**
	 * Create new {@linkplain JsonFilter} instance.
	 * 
	 * @return newly created {@linkplain JsonFilter}
	 */

	JsonFilter newJsonFilter();

	/**
	 * Allows the user to set specific feature/property on the underlying
	 * implementation. The underlying implementation is not required to support
	 * every setting of every property in the specification and may use
	 * IllegalArgumentException to signal that an unsupported property may not be
	 * set with the specified value.
	 * 
	 * @param name The name of the property (may not be null)
	 * @param value The value of the property
	 * @throws java.lang.IllegalArgumentException if the property is not supported
	 */

	void setProperty(java.lang.String name, Object value);

	/**
	 * Query the set of properties that this factory supports.
	 *
	 * @param name The name of the property (may not be null)
	 * @return true if the property is supported and false otherwise
	 */
	boolean isPropertySupported(String name);

}
