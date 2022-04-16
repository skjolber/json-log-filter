/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.skjolber.jsonfilter;

/**
 * Interface for filtering JSON. <br>
 *  
 * @author Thomas Rorvik Skjolberg
 * 
 */

public interface JsonFilter {

	// Implementation note: The optimal output format 
	// depends on what will be done with the filtered document, which translates to the the raw JSON append capabilities 
	// of log frameworks; Jackson with Logback-logstash only supports characters (and this is the primary use-case).

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @return a String instance if filtering was successful, null otherwise.
	 */

	String process(char[] chars);

	/**
	 * Filter JSON characters.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @return a StringBuilder instance filtering was successful, null otherwise.
	 */

	String process(String chars);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @param output the buffer to which filtered JSON is appended
	 * @return true if filtering was successful. If false, the output buffer is unaffected.
	 */

	boolean process(String chars, StringBuilder output);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @param offset the offset within the chars where the JSON starts
	 * @param length the length of the JSON within the chars
	 * @param output the buffer to which filtered JSON is appended
	 * @return true if filtering was successful. If false, the output buffer is unaffected.
	 */

	boolean process(char[] chars, int offset, int length, StringBuilder output);

	/**
	 * Filter JSON characters.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @return a byte array instance if filtering was successful, null otherwise.
	 */

	byte[] process(byte[] chars);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be filtered
	 * @param offset the offset within the chars where the JSON starts
	 * @param length the length of the JSON within the chars
	 * @return a byte array instance if filtering was successful, null otherwise.
	 */

	byte[] process(byte[] chars, int offset, int length);

	/**
	 * 
	 * Check if validation
	 * 
	 * @return true if the processing returns true only if the input is valid json
	 */
	
	default boolean isValidating() {
		return false;
	}

	/**
	 * 
	 * Check if compacting, as in not containing any linebreaks.
	 * 
	 * @return true if the processing returns a compacted value
	 */

	default boolean isCompacting() {
		return false;
	}

}
