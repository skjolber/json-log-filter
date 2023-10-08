package com.github.skjolber.jsonfilter.path.properties;

/**
 * Strategy for dealing with whitespace.
 * Whitespace removal comes at a performance cost and is usually not necessary for response payloads.
 */

public enum WhitespaceStrategy {
	
	/** Never remove whitespace **/
	NEVER,
	/** Always remove whitespace. **/
	ALWAYS,
	/** Remove whitespace if max size is exceeded or if the payload contains a newline character */
	ON_DEMAND
	;
	
}
