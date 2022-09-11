package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterRunner {

	private static final String[] SPACES;
	
	static {
		SPACES = new String[128];
		
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < SPACES.length; i++) {
			SPACES[i] = b.toString();
			b.append(' ');
		}
	}
	
	private JsonFileCache cache;
	private JsonFilterPropertiesFactory jsonFilterPropertiesFactory;
	private JsonFilterOutputDirectoriesFactory jsonOutputDirectoriesFactory;
	private File directory;
	private List<JsonFilterOutputDirectory> outputDirectories;
	private boolean literal;
	private JsonFactory jsonFactory;

	public JsonFilterRunner(List<?> nullable, File directory, JsonFilterPropertiesFactory jsonFilterPropertiesFactory) throws Exception {
		this(nullable, directory, jsonFilterPropertiesFactory, true);
	}

	public JsonFilterRunner(List<?> nullable, File directory, JsonFilterPropertiesFactory jsonFilterPropertiesFactory, boolean literal) throws Exception {
		this(nullable, directory, jsonFilterPropertiesFactory, literal, JsonFileCache.getInstance());
	}

	public JsonFilterRunner(List<?> nullable, File directory, JsonFilterPropertiesFactory jsonFilterPropertiesFactory, boolean literal, JsonFileCache cache) throws Exception {
		this.directory = directory;
		this.jsonOutputDirectoriesFactory = new JsonFilterOutputDirectoriesFactory(nullable);
		this.jsonFilterPropertiesFactory = jsonFilterPropertiesFactory;
		this.literal = literal;
		this.cache = cache;

		this.jsonFactory = new JsonFactory();
		this.outputDirectories = jsonOutputDirectoriesFactory.create(directory);
	}

	public JsonFilterResult process(JsonFilter filter, Predicate<String> predicate) throws Exception {
		return process(filter, predicate, Function.identity());
	}

	public JsonFilterResult process(JsonFilter filter, Predicate<String> predicate, Function<String, String> transformer) throws Exception {
		JsonFilterResult result = new JsonFilterResult();

		JsonFilterProperties properties = jsonFilterPropertiesFactory.createInstance(filter);

		if(!properties.isNoop()) {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();
				if(inputDirectory.matches(properties)) {
					processInputOutput(inputDirectory, outputDirectory, filter, predicate, transformer);

					result.addFiltered(outputDirectory);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, filter, predicate, transformer);

				Map<String, File> files = outputDirectory.getFiles();
				processInputOutput(filter, files, files, properties.getProperties(), predicate, transformer);

				result.addPassthrough(outputDirectory);
			}
		}

		return result;
	}

	public JsonFilterResult process(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) throws Exception {
		return process(maxSize, infiniteSize, (p) -> true, Function.identity());
	}
	
	public JsonFilterResult process(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Function<String, String> transformer) throws Exception {
		return process(maxSize, infiniteSize, (p) -> true, transformer);
	}

	public JsonFilterResult process(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> predicate, Function<String, String> transformer) throws Exception {
		JsonFilterResult result = new JsonFilterResult();

		JsonFilterProperties properties = jsonFilterPropertiesFactory.createInstance(infiniteSize);
		
		if(!properties.isNoop()) {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();
				if(inputDirectory.matches(properties)) {
					processInputOutput(inputDirectory, outputDirectory, maxSize, infiniteSize, predicate, transformer);

					result.addFiltered(outputDirectory);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, maxSize, infiniteSize, predicate, transformer);

				Map<String, File> files = outputDirectory.getFiles();
				processInputOutput(maxSize, infiniteSize, files, files, properties.getProperties(), predicate, transformer);

				result.addPassthrough(outputDirectory);
			}
		}

		return result;
	}

	
	private void processInput(JsonFilterInputDirectory inputDirectory, JsonFilter filter, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(filter, sourceFiles, sourceFiles, properties, predicate, transformer);
	}
	
	private void processInput(JsonFilterInputDirectory inputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(maxSize, infiniteSize, sourceFiles, sourceFiles, properties, predicate, transformer);
	}


	protected void processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, JsonFilter filter, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(filter, filteredFiles, sourceFiles, properties, predicate, transformer);
	}

	protected void processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(maxSize, infiniteSize, filteredFiles, sourceFiles, properties, predicate, transformer);
	}

	
	private void processInputOutput(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Map<String, File> filteredFiles, Map<String, File> sourceFiles, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {

		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(maxSize, infiniteSize, sourceFile, sourceFile, properties, predicate, transformer);
				
				compareBytes(maxSize, infiniteSize, sourceFile, sourceFile, properties, predicate, transformer);
			} else {
				compareChars(maxSize, infiniteSize, sourceFile, filteredFile, properties, predicate, transformer);
				
				compareBytes(maxSize, infiniteSize, sourceFile, filteredFile, properties, predicate, transformer);
			}
		}
		
	}

	
	private void processInputOutput(JsonFilter filter, Map<String, File> filteredFiles, Map<String, File> sourceFiles,
			Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(filter, sourceFile, sourceFile, properties, predicate, transformer);
				compareBytes(filter, sourceFile, sourceFile, properties, predicate, transformer);
			} else {
				compareChars(filter, sourceFile, filteredFile, properties, predicate, transformer);
				compareBytes(filter, sourceFile, filteredFile, properties, predicate, transformer);
			}
		}
	}

	protected void compareChars(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		String original = cache.getFile(sourceFile);
		
		String from = transformer.apply(original);
		if(!predicate.test(from)) {
			return;
		}
		String expected = cache.getFile(filteredFile);

		String input;
		if(expected.length() < from.length()) {
			input = from;
		} else {
			input = from + spaces(expected.length() - from.length() + 1);
		}

		JsonFilter maxSize = maxSizeFunction.apply(expected.length());

		StringBuilder maxSizeOutput = new StringBuilder(from.length() * 2);
		if(!maxSize.process(input, maxSizeOutput)) {
			System.out.println(sourceFile);
			System.out.println(input);
			throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + filter);
		}
		
		String result = maxSizeOutput.toString();

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, input, result, original, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, input, result, original, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile + " with max size " + expected.length());
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, input, result, original, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile + " size " + expected.length());
			}
		}
	}

	private byte[] spaces(byte[] content, int length) {
		byte[] c = new byte[content.length + length];
		
		System.arraycopy(content, 0, c, 0, content.length);
		for(int i = 0; i < length; i++) {
			c[content.length + i] = ' ';
		}
		
		return c;
	}

	private String spaces(int length) {
		if(length < SPACES.length) {
			return SPACES[length];
		}
		
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < length; i++) {
			builder.append(' ');
		}
		
		return builder.toString();
	}

	protected void compareChars(JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		String original = cache.getFile(sourceFile);
		
		String from = transformer.apply(original);
		
		if(!predicate.test(from)) {
			return;
		}

		StringBuilder output = new StringBuilder(from.length() * 2);
		if(!filter.process(from, output)) {
			System.out.println(sourceFile);
			System.out.println(from);
			throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
		}
		
		if(!filter.process(from, output, new DefaultJsonFilterMetrics())) {
			throw new IllegalArgumentException("Unable to process using metrics");
		}

		String result = output.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}

	private boolean isSurrogates(String from) {
		for(int i = 0; i < from.length(); i++) {
			if(Character.isHighSurrogate(from.charAt(i))) {
				return true;
			}
		}
		return false;
	}
	
	protected void compareBytes(JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		String original = cache.getFile(sourceFile);
		
		String from = transformer.apply(original);
		if(!predicate.test(from)) {
			return;
		}
		
		byte[] process = filter.process(from.getBytes(StandardCharsets.UTF_8));
		if(process == null) {
			System.out.println("Unable to process " + sourceFile);
			System.out.println(from);

			throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
		}
		
		if(filter.process(from.getBytes(StandardCharsets.UTF_8), new DefaultJsonFilterMetrics()) == null) {
			throw new IllegalArgumentException("Unable to process using metrics");
		}


		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(from);

		String result = new String(process);

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expected) : expected;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, original, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}


	protected void compareBytes(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		String original = cache.getFile(sourceFile);
		
		String chars = transformer.apply(original);
		if(!predicate.test(chars)) {
			return;
		}
		byte[] from = chars.getBytes(StandardCharsets.UTF_8);

		byte[] expected = cache.getFile(filteredFile).getBytes(StandardCharsets.UTF_8);

		byte[] input;
		if(expected.length < from.length) {
			input = from;
		} else {
			input = spaces(from, expected.length - from.length + 1);
		}

		JsonFilter maxSize = maxSizeFunction.apply(expected.length);		

		byte[] maxSizeOutput = maxSize.process(input);
		if(maxSizeOutput == null) {
			System.out.println("Unable to process max size " + sourceFile);
			System.out.println(from);
			throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + maxSize);
		}

		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(chars);

		String result = new String(maxSizeOutput);
		String expectedChars = new String(expected);
		
		if(isWellformed(result, jsonFactory) != isWellformed(expectedChars, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, chars, original, result, expectedChars);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expectedChars) : expectedChars;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, filteredExpected, original, filteredResult, expectedChars);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, filteredExpected, original, filteredResult, expectedChars);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}

	public static String filterSurrogates(String result) {
		// ...TRUNCATED BY 

		Pattern patt = Pattern.compile("(TRUNCATED BY )([0-9]+)");
		Matcher m = patt.matcher(result);
		StringBuffer sb = new StringBuffer(result.length());
		while (m.find()) {
			String text = m.group(1);
			m.appendReplacement(sb, text + "XX");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private boolean parseCompare(String expected, String result) {

		try (
				JsonParser expectedParser = jsonFactory.createParser(expected);
				JsonParser resultParser = jsonFactory.createParser(result)) {

			while(true) {
				JsonToken expectedToken = expectedParser.nextToken();
				JsonToken resultToken = resultParser.nextToken();
				if(expectedToken == null) {
					break;
				}

				if(!expectedToken.equals(resultToken)) {
					return false;
				}
				
				
				switch(expectedToken)  {
					case FIELD_NAME: {
						if(!expectedParser.getCurrentName().equals(resultParser.getCurrentName())) {
							return false;
						}
						break;
					}
					case VALUE_FALSE:
					case VALUE_TRUE: {
						if(expectedParser.getBooleanValue() != resultParser.getBooleanValue()) {
							return false;
						}
						break;
					}
					case VALUE_STRING: {
						if(!expectedParser.getText().equals(resultParser.getText())) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_INT: {
						if(expectedParser.getIntValue() != resultParser.getIntValue()) {
							return false;
						}
						break;
					}
					case VALUE_NUMBER_FLOAT: {
						if(expectedParser.getFloatValue() != resultParser.getFloatValue()) {
							return false;
						}
						break;
					}
				}
				

			}
		} catch(Exception e) {
			return false;
		}

		return true;
	}

	protected void printDiff(JsonFilter filter, Properties properties, File expectedFile, File original, String from, String fromTransformed, String to, String expected) {
		System.out.println("Processing using: " + filter);
		System.out.println("Properties: " + properties);
		if(expectedFile.equals(original)) {
			System.out.println("File input/output: " + original);
		} else {
			System.out.println("File input: " + original + " size " + original.length());
			System.out.println("File expected output: " + expectedFile + " size " + expectedFile.length());
		}
		System.out.println("From: \n" + from);
		if(!from.equals(fromTransformed)) {
			System.out.println("Transformed: \n" + fromTransformed);
		}
		System.out.println("Expected:\n" + expected);
		System.out.println("Actual:\n" + to);
		System.out.println("(size " + expected.length() + " vs " + to.length() + ")");

		for(int k = 0; k < Math.min(expected.length(), to.length()); k++) {
			if(expected.charAt(k) != to.charAt(k)) {
				System.out.println("Diff at " + k + ": " + expected.charAt(k) + " vs + " + to.charAt(k));

				break;
			}
		}
	}
	public static boolean isWellformed(String s) {
		return isWellformed(s, new JsonFactory());
	}

	public static boolean isWellformed(String s, JsonFactory jsonFactory) {
		try (JsonParser parser = jsonFactory.createParser(new StringReader(s))) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			return false;
		}
		return true;			
	}

	public File getDirectory() {
		return directory;
	}

}
