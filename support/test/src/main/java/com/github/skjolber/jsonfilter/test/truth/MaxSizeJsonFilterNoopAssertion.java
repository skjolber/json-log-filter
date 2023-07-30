package com.github.skjolber.jsonfilter.test.truth;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonCollection;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonItem;

public class MaxSizeJsonFilterNoopAssertion extends AbstractJsonFilterSymmetryAssertion {

	public static MaxSizeJsonFilterNoopAssertion newInstance() {
		return new MaxSizeJsonFilterNoopAssertion();
	}

	private JsonFile input;
	private JsonFilterMetrics metrics;
	private MaxSizeJsonFilterPair maxSizeJsonFilterPair;

	public MaxSizeJsonFilterNoopAssertion withInput(JsonFile input) {
		this.input = input;
		return this;
	}
	
	public MaxSizeJsonFilterNoopAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public MaxSizeJsonFilterNoopAssertion withMaxSizeJsonFilterPair(MaxSizeJsonFilterPair maxSizeJsonFilterPair) {
		this.maxSizeJsonFilterPair = maxSizeJsonFilterPair;
		return this;
	}

	public void isPassthrough() {
		if(input == null) {
			throw new IllegalStateException();
		}
		if(maxSizeJsonFilterPair == null) {
			throw new IllegalStateException();
		}
		if(metrics == null) {
			throw new IllegalStateException();
		}
		
		String contentAsString = input.getContentAsString();
		byte[] contentAsBytes = input.getContentAsBytes();
		
		JsonFilter infiniteJsonFilter = maxSizeJsonFilterPair.getInfiniteJsonFilter();
		
		byte[] bytesOutput = infiniteJsonFilter.process(contentAsBytes); // no metrics
		String charsOutput = infiniteJsonFilter.process(contentAsString); // no metrics
		
		assertEquals(input.getSource(), contentAsString, charsOutput, bytesOutput);
		
		List<MaxSizeJsonCollection> charsInputs = input.getMaxSizeCollections();
		
		boolean removingWhitespace = maxSizeJsonFilterPair.isRemovingWhitespace();
		
		for(int i = 0; i < charsInputs.size() - 1; i++) {
			MaxSizeJsonCollection current = charsInputs.get(i);
			MaxSizeJsonCollection next = charsInputs.get(i + 1);

			// Check only the range that was added between items, i.e. when going from
			//
			// |-----------------------------------------|
			// | {"firstName":"John"}                    | Current
			// | {"firstName":"John","lastName":"Smith"} | Next
			// |                    ,"lastName":"Smith"  | In-scope range
			// |-----------------------------------------|
			
			int k = current.getMark();
			while(k < next.getMark()) {
				String charsValue = next.getContentAsString();
				
				int maxByteSize = charsValue.substring(0, k).getBytes(StandardCharsets.UTF_8).length;
				int maxCharSize = k;
				
				byte[] bytesValue = charsValue.getBytes(StandardCharsets.UTF_8);
				
				JsonFilter bytesFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxByteSize);
				JsonFilter charsFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxCharSize);
				
				byte[] maxSizeBytesOutput = bytesFilter.process(bytesValue, metrics);
				String maxSizeCharsOutput = charsFilter.process(charsValue, metrics);
				
				assertEquals(input.getSource(), current.getContentAsString(), maxSizeCharsOutput, maxSizeBytesOutput);
				
				// if the filter is removing whitespace, filter the input with various
				// variations of pretty-printing and compare to the output from the filtering
				// of the original input
				
				k = next.nextCodePoint(k);
			}
		}
		
		// filter all the pretty-printed variants
		if(removingWhitespace) {
			// expect to see the original inputs (without pretty-printing)
			
			for(int i = 0; i < charsInputs.size() - 1; i++) {
				MaxSizeJsonCollection current = charsInputs.get(i);

				contentAsString = input.getContentAsString();
				contentAsBytes = input.getContentAsBytes();
				
				// pretty-printed inputs is going to exceed the current max byte/char size
				// so just use max size same as the input length
				for (MaxSizeJsonItem maxSizeJsonItem : current.getPrettyPrinted()) {
					String prettyPrintedAsString = maxSizeJsonItem.getContentAsString();
					byte[] prettyPrintedAsBytes = prettyPrintedAsString.getBytes(StandardCharsets.UTF_8);

					JsonFilter charsFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(contentAsString.length());
					JsonFilter bytesFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(contentAsBytes.length);
					
					String stringOutput2 = charsFilter.process(prettyPrintedAsString, metrics);
					assertEquals(input.getSource(), prettyPrintedAsString, contentAsString, stringOutput2);
					
					byte[] byteOutput2 = bytesFilter.process(prettyPrintedAsBytes, metrics);
					assertEquals(input.getSource(), prettyPrintedAsBytes, contentAsBytes, byteOutput2);
				}
			}
		} else {
			// filter all the pretty-printed variants, 
			// expect to see the correspondingly pretty-printed input
			
			for(int i = 1; i < charsInputs.size() - 1; i++) {
				MaxSizeJsonCollection current = charsInputs.get(i);
				
				// pretty-printed inputs is going to exceed the current max byte/char size
				// so just use max size same as the input length
				for (MaxSizeJsonItem maxSizeJsonItem : current.getPrettyPrinted()) {
					String prettyPrintedAsString = maxSizeJsonItem.getContentAsString();
					byte[] prettyPrintedAsBytes = prettyPrintedAsString.getBytes(StandardCharsets.UTF_8);

					int maxCharSize = maxSizeJsonItem.getMark() + maxSizeJsonItem.getLevel();
					int maxByteSize = prettyPrintedAsString.substring(0, maxSizeJsonItem.getMark()).getBytes(StandardCharsets.UTF_8).length + maxSizeJsonItem.getLevel();
					
					JsonFilter charsFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxCharSize);
					JsonFilter bytesFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxByteSize);
					
					String stringOutput2 = charsFilter.process(prettyPrintedAsString, metrics);
					assertEventsEquals(input.getSource(), prettyPrintedAsString, stringOutput2, prettyPrintedAsString);
					
					byte[] byteOutput2 = bytesFilter.process(prettyPrintedAsBytes, metrics);
					assertEventsEquals(input.getSource(), prettyPrintedAsBytes, byteOutput2, prettyPrintedAsBytes);
				}
			}
		}
	}

}
