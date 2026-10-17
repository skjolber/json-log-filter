package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonCollection;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonCollectionInputOutputAlignment;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

public class MaxSizeJsonFilterAssertion extends AbstractJsonFilterSymmetryAssertion {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	public static MaxSizeJsonFilterAssertion newInstance() {
		return new MaxSizeJsonFilterAssertion();
	}
	
	private JsonFile inputFile;
	private JsonFilterMetrics metrics;
	private MaxSizeJsonFilterPair maxSizeJsonFilterPair;

	public MaxSizeJsonFilterAssertion withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public MaxSizeJsonFilterAssertion withMaxSizeJsonFilterPair(MaxSizeJsonFilterPair maxSizeJsonFilterPair) {
		this.maxSizeJsonFilterPair = maxSizeJsonFilterPair;
		return this;
	}
	
	public MaxSizeJsonFilterAssertion withInputFile(JsonFile file) {
		this.inputFile = file;
		return this;
	}

	public void filters(JsonFile outputFile) {
		filters(outputFile, JsonComparisonType.LITERAL);
	}
	
	public void filters(JsonFile outputFile, JsonComparisonType comparison) {
		if(inputFile == null) {
			throw new IllegalStateException();
		}
		if(maxSizeJsonFilterPair == null) {
			throw new IllegalStateException();
		}
		if(outputFile == null) {
			throw new IllegalStateException();
		}

		String inputContentAsString = inputFile.getContentAsString();
		byte[] inputContentAsBytes = inputFile.getContentAsBytes();

		String expectedOutputContentAsString = outputFile.getContentAsString();
		byte[] expectedOutputContentAsBytes = outputFile.getContentAsBytes();
		
		JsonFilters infiniteJsonFilter = maxSizeJsonFilterPair.getInfiniteJsonFilter();

		byte[] byteOutput = infiniteJsonFilter.getBytes().process(inputContentAsBytes, metrics);
		String stringOutput = infiniteJsonFilter.getCharacters().process(inputContentAsString, metrics);
		
		if(comparison == JsonComparisonType.LITERAL) {
			assertEquals(inputFile.getSource(), inputContentAsString, stringOutput, expectedOutputContentAsString);
			assertEquals(inputFile.getSource(), inputContentAsBytes, byteOutput, expectedOutputContentAsBytes);
		} else {
			JsonComparator.assertEventsEqual(inputFile.getSource(), inputContentAsString, inputContentAsBytes, stringOutput, byteOutput);
		}
		
		List<MaxSizeJsonCollection> charsInputs = inputFile.getMaxSizeCollections();
		List<MaxSizeJsonCollection> byteInputs = inputFile.getMaxSizeCollections();

		List<MaxSizeJsonCollection> charsOutputs = outputFile.getMaxSizeCollections();
		List<MaxSizeJsonCollection> byteOutputs = outputFile.getMaxSizeCollections();
		
		// output might not correlate directly to input, as output might reduce the number
		// of events because of filtering. But each output has one or more inputs.
		
		MaxSizeJsonCollectionInputOutputAlignment alignment = MaxSizeJsonCollectionInputOutputAlignment.create(charsInputs, charsOutputs, infiniteJsonFilter);

		for(int i = 0; i < charsInputs.size() - 1 && i < charsOutputs.size(); i++) {
			MaxSizeJsonCollection inputCurrent = alignment.getInput(i);
			MaxSizeJsonCollection inputNext = alignment.getInput(i + 1);

			MaxSizeJsonCollection outputCurrent = alignment.getOutput(i);

			// Check only the range that was added between items, i.e. when going from
			//
			// |-----------------------------------------|
			// | {"firstName":"John"}                    | Current
			// | {"firstName":"John","lastName":"Smith"} | Next
			// |                    ,"lastName":"Smith"  | In-scope range
			// |-----------------------------------------|
			
			int k = inputCurrent.getMark();
			while(k < inputNext.getMark()) {
				String charsValue = inputNext.getContentAsString();
				
				int maxByteSize = charsValue.substring(0, k).getBytes(StandardCharsets.UTF_8).length;
				int maxCharSize = k;

				String expectedMaxSizeCharsOutput = outputCurrent.getContentAsString();
				byte[] expectedMaxSizeBytesOutput = expectedMaxSizeCharsOutput.getBytes(StandardCharsets.UTF_8);

				if(expectedMaxSizeCharsOutput.length() > charsValue.length()) {
					
					// Output exceeds input
					//
					// |-----------------------------------------|
					// | ["a", "b"]                              | Input
					// | ["***", "***"]                          | Output
					// |-----------------------------------------|

					// add to the max limit to allow the filter to arrive at the same result
					maxByteSize = expectedMaxSizeCharsOutput.length();
					maxCharSize = expectedMaxSizeBytesOutput.length;
					
				} else if(expectedMaxSizeCharsOutput.length() > maxCharSize) {
					
					maxCharSize = expectedMaxSizeCharsOutput.length();
					maxByteSize = expectedMaxSizeBytesOutput.length;
					
					byte[] bytesValue = charsValue.getBytes(StandardCharsets.UTF_8);
					
					JsonFilter bytesFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxByteSize);
					JsonFilter charsFilter = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxCharSize);
					
					byte[] maxSizeBytesOutput = bytesFilter.process(bytesValue, metrics);
					String maxSizeCharsOutput = charsFilter.process(charsValue, metrics);
					
					assertEquals(inputFile.getSource(), charsValue, maxSizeCharsOutput, expectedMaxSizeCharsOutput);
					assertEquals(inputFile.getSource(), bytesValue, maxSizeBytesOutput, expectedMaxSizeBytesOutput);
				}

				k = inputNext.nextCodePoint(k);
			}
		}
		
		for(int i = 0; i < charsInputs.size() - 1; i++) {
			MaxSizeJsonCollection charsInput = charsInputs.get(i);
			MaxSizeJsonCollection bytesInput = byteInputs.get(i);

			// TODO
		}		

		assertEveryMaxSize(inputFile.getSource(), inputContentAsString, stringOutput);
		assertEveryMaxSize(inputFile.getSource(), inputContentAsBytes, byteOutput);
	}

	/**
	 * Filter with every max size, from 1 up to past the length of both the input and the output without max size.
	 * <br><br>
	 * Each output must be a single JSON value, which is a prefix of the output without max size: see
	 * {@link #getPrefixMismatch(List, List)}. So a value which is anonymized or pruned without max size cannot
	 * appear as-is. The output can only be empty when the max size is too small for an empty object or array.
	 * <br><br>
	 * Max size is not exact: the output can be larger than the max size, or leave out values which would fit.
	 * So the size of the output is not checked, and the output only has to be the output without max size
	 * when the max size is the length of the input and the output without max size together.
	 *
	 * @param source input file, for the failure message
	 * @param input document
	 * @param expectedOutput output without max size
	 */

	protected void assertEveryMaxSize(Path source, String input, String expectedOutput) {
		List<String> expected = getTokens(expectedOutput);
		if(expected == null) {
			fail("Expected a single JSON value without max size for " + source + "\n" + expectedOutput);
		}
		int minimumSize = getLeadingWhitespace(input) + 2;
		for(int maxSize = 1; maxSize <= Math.max(input.length(), expectedOutput.length()) + 1; maxSize++) {
			assertMaxSize(source, input, expectedOutput, expected, maxSize, minimumSize, false);
		}
		assertMaxSize(source, input, expectedOutput, expected, input.length() + expectedOutput.length(), minimumSize, true);
	}

	private void assertMaxSize(Path source, String input, String expectedOutput, List<String> expected, int maxSize, int minimumSize, boolean complete) {
		String output = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxSize).process(input, metrics);
		String failure = getMaxSizeFailure(output, maxSize, minimumSize, expected, complete);
		if(failure != null) {
			fail("Max size " + maxSize + " gave " + failure + " for " + source + "\n" + input + "\nOutput:\n" + output + "\nOutput without max size:\n" + expectedOutput);
		}
	}

	protected void assertEveryMaxSize(Path source, byte[] input, byte[] expectedOutput) {
		List<String> expected = getTokens(expectedOutput);
		if(expected == null) {
			fail("Expected a single JSON value without max size for " + source + "\n" + new String(expectedOutput, StandardCharsets.UTF_8));
		}
		int minimumSize = getLeadingWhitespace(input) + 2;
		for(int maxSize = 1; maxSize <= Math.max(input.length, expectedOutput.length) + 1; maxSize++) {
			assertMaxSize(source, input, expectedOutput, expected, maxSize, minimumSize, false);
		}
		assertMaxSize(source, input, expectedOutput, expected, input.length + expectedOutput.length, minimumSize, true);
	}

	private void assertMaxSize(Path source, byte[] input, byte[] expectedOutput, List<String> expected, int maxSize, int minimumSize, boolean complete) {
		byte[] output = maxSizeJsonFilterPair.getMaxSizeJsonFilter(maxSize).process(input, metrics);
		String failure = getMaxSizeFailure(output, maxSize, minimumSize, expected, complete);
		if(failure != null) {
			fail("Max size " + maxSize + " (bytes) gave " + failure + " for " + source + "\n" + new String(input, StandardCharsets.UTF_8) + "\nOutput:\n" + (output != null ? new String(output,
					StandardCharsets.UTF_8) : null) + "\nOutput without max size:\n" + new String(expectedOutput, StandardCharsets.UTF_8));
		}
	}

	/**
	 * Check the output for a max size.
	 *
	 * @param output output with max size
	 * @param maxSize max size
	 * @param minimumSize smallest max size which fits an empty object or array
	 * @param expected tokens of the output without max size
	 * @param complete whether the output must be the output without max size
	 * @return null if the output is as expected, otherwise a description of the problem
	 */

	protected static String getMaxSizeFailure(String output, int maxSize, int minimumSize, List<String> expected, boolean complete) {
		if(output == null) {
			return "no output";
		}
		if(output.isBlank()) {
			return maxSize < minimumSize ? null : "empty output";
		}
		return getMaxSizeFailure(getTokens(output), expected, complete);
	}

	protected static String getMaxSizeFailure(byte[] output, int maxSize, int minimumSize, List<String> expected, boolean complete) {
		if(output == null) {
			return "no output";
		}
		if(getLeadingWhitespace(output) == output.length) {
			return maxSize < minimumSize ? null : "empty output";
		}
		return getMaxSizeFailure(getTokens(output), expected, complete);
	}

	protected static String getMaxSizeFailure(List<String> actual, List<String> expected, boolean complete) {
		if(actual == null) {
			return "output which is not a single JSON value";
		}
		if(complete) {
			return actual.equals(expected) ? null : "other output than without max size";
		}
		return getPrefixMismatch(actual, expected);
	}

	/**
	 * Check that a document is a prefix of another: the same tokens up to some point, after which the
	 * document only closes the objects and arrays which are open. So values can only be left out at the end
	 * of each object or array, and all other values must be equal.
	 *
	 * @param actual tokens of the document with max size
	 * @param expected tokens of the document without max size
	 * @return null if the document is a prefix, otherwise a description of the first difference
	 */

	protected static String getPrefixMismatch(List<String> actual, List<String> expected) {
		int count = 0;
		while(count < actual.size() && count < expected.size() && actual.get(count).equals(expected.get(count))) {
			count++;
		}
		for(int i = count; i < actual.size(); i++) {
			if(!actual.get(i).startsWith(JsonToken.END_OBJECT.name()) && !actual.get(i).startsWith(JsonToken.END_ARRAY.name())) {
				return "output which is not a prefix of the output without max size: " + actual.get(count) + " instead of " + (count < expected.size() ? expected.get(count) : "nothing") + " at token "
						+ count;
			}
		}
		return null;
	}

	/**
	 * Get the tokens of a document, as token type and text.
	 *
	 * @param json document
	 * @return the tokens, or null if the document is not a single JSON value
	 */

	protected static List<String> getTokens(String json) {
		try {
			return getTokens(JSON_FACTORY.createParser(json));
		} catch(Exception e) {
			return null;
		}
	}

	protected static List<String> getTokens(byte[] json) {
		try {
			return getTokens(JSON_FACTORY.createParser(json));
		} catch(Exception e) {
			return null;
		}
	}

	private static List<String> getTokens(JsonParser parser) {
		List<String> tokens = new ArrayList<>();
		try (parser) {
			int level = 0;
			JsonToken token;
			while((token = parser.nextToken()) != null) {
				if(level == 0 && !tokens.isEmpty()) {
					// more than one value
					return null;
				}
				tokens.add(token.name() + " " + parser.getText());
				if(token.isStructStart()) {
					level++;
				} else if(token.isStructEnd()) {
					level--;
				}
			}
			return level == 0 && !tokens.isEmpty() ? tokens : null;
		} catch(Exception e) {
			return null;
		}
	}

	protected static int getLeadingWhitespace(String json) {
		int count = 0;
		while(count < json.length() && json.charAt(count) <= 0x20) {
			count++;
		}
		return count;
	}

	protected static int getLeadingWhitespace(byte[] json) {
		int count = 0;
		while(count < json.length && json[count] <= 0x20 && json[count] >= 0) {
			count++;
		}
		return count;
	}


	
	
}
