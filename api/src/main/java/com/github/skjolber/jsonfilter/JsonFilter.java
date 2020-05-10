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

import java.io.IOException;
import java.io.Reader;

/**
 * Interface for filtering JSON. <br>
 *  
 * @author Thomas Rorvik Skjolberg
 * 
 */

public interface JsonFilter {
	
	// Implementation note: Support for `byte[]` on the input is possible, but the optimal output format 
	// depends on what will be done with the filtered document, which translates to the the raw JSON append capabilities 
	// of log frameworks; Jackson with Logback-logstash only supports characters (and this is the primary use-case).
	
	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be pretty printed
	 * @return a StringBuilder instance filtering was successful, null otherwise.
	 */

	String process(char[] chars);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be pretty printed
	 * @return a StringBuilder instance filtering was successful, null otherwise.
	 */

	String process(String chars);
	
	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be pretty printed
	 * @param output the buffer to which indented JSON is appended
	 * @return true if filtering was successful. If false, the output buffer is unaffected.
	 */

	boolean process(String chars, StringBuilder output);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param chars characters containing JSON to be pretty printed
	 * @param offset the offset within the chars where the JSON starts
	 * @param length the length of the JSON within the chars
	 * @param output the buffer to which indented JSON is appended
	 * @return true if filtering was successful. If false, the output buffer is unaffected.
	 */

	boolean process(char[] chars, int offset, int length, StringBuilder output);

	/**
	 * Filter JSON characters to an output StringBuilder.
	 * 
	 * @param reader reader containing JSON characters to be pretty printed
	 * @param length the number of characters within the reader
	 * @param output the buffer to which indented JSON is appended
	 * @throws IOException from reader
	 * @return true if filtering was successful. If false, the output buffer is unaffected.
	 */

	boolean process(Reader reader, int length, StringBuilder output) throws IOException;
}
