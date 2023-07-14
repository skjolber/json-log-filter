package com.github.skjolber.jsonfilter.test.truth;

import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;

/**
 * 
 * Check that output is the same for chars and bytes, including pretty-printing. If the filter is removing whitespace, check
 * that the whitespace is removed correspondingly.
 */

public class JsonFilterNoopAssertion extends AbstractJsonFilterSymmetryAssertion {

	public static JsonFilterNoopAssertion newInstance() {
		return new JsonFilterNoopAssertion();
	}
	
	private JsonFile input;
	private JsonFilterMetrics metrics;
	private JsonFilter filter;

	public JsonFilterNoopAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public JsonFilterNoopAssertion withInput(JsonFile input) {
		this.input = input;
		return this;
	}
	
	public JsonFilterNoopAssertion withFilter(JsonFilter filter) {
		this.filter = filter;
		return this;
	}
	
	public void isNoop() {
		if(input == null) {
			throw new IllegalStateException();
		}
		if(filter == null) {
			throw new IllegalStateException();
		}
		if(metrics == null) {
			throw new IllegalStateException();
		}

		String contentAsString = input.getContentAsString();
		byte[] contentAsBytes = input.getContentAsBytes();
		
		String stringOutput = filter.process(contentAsString, metrics);
		byte[] byteOutput = filter.process(contentAsBytes, metrics);
		
		assertEquals(input.getSource(), contentAsString, stringOutput, byteOutput);

		for(int i = 0; i < input.getPrettyPrintedSize(); i++) {
			String prettyPrintedAsString = input.getPrettyPrintedAsString(i);
			byte[] prettyPrintedAsBytes = input.getPrettyPrintedAsBytes(i);
			
			String prettyPrintStringOutput = filter.process(prettyPrintedAsString, metrics);
			byte[] prettyPrintBytesOutput = filter.process(prettyPrintedAsBytes, metrics);
			
			assertEquals(input.getSource(), prettyPrintedAsString, prettyPrintedAsBytes, prettyPrintStringOutput, prettyPrintBytesOutput);

			if(filter.isRemovingWhitespace()) {
				if(!Objects.equals(stringOutput, prettyPrintStringOutput)) {
					assertEquals(input.getSource(), contentAsString, prettyPrintedAsString, stringOutput, prettyPrintStringOutput);
				}
				if(!Arrays.equals(byteOutput, prettyPrintBytesOutput)) {
					assertEquals(input.getSource(), contentAsBytes, prettyPrintedAsBytes, byteOutput, prettyPrintBytesOutput);
				}
			} else {
				JsonComparator.assertEventsEqual(input.getSource(), contentAsString, prettyPrintedAsString, stringOutput, prettyPrintStringOutput);
			}
		}
	}


}
