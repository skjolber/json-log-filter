package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import com.github.skjolber.jsonfilter.JsonFilterMetrics;

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
					Map<File, DefaultJsonFilterMetrics[]> processInputOutput = processInputOutput(inputDirectory, outputDirectory, filter, predicate, transformer);

					result.addFiltered(outputDirectory);
					result.add(processInputOutput);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, filter, predicate, transformer);

				Map<String, File> files = outputDirectory.getFiles();
				Map<File, DefaultJsonFilterMetrics[]> processInputOutput = processInputOutput(filter, files, files, properties.getProperties(), predicate, transformer);

				result.addPassthrough(outputDirectory);
				result.add(processInputOutput);
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
					Map<File, DefaultJsonFilterMetrics[]> processInputOutput = processInputOutput(inputDirectory, outputDirectory, maxSize, infiniteSize, predicate, transformer);

					result.addFiltered(outputDirectory);
					result.add(processInputOutput);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, maxSize, infiniteSize, predicate, transformer);

				Map<String, File> files = outputDirectory.getFiles();
				Map<File, DefaultJsonFilterMetrics[]> processInputOutput = processInputOutput(maxSize, infiniteSize, files, files, properties.getProperties(), predicate, transformer);

				result.addPassthrough(outputDirectory);
				result.add(processInputOutput);
			}
		}

		return result;
	}

	
	private Map<File, DefaultJsonFilterMetrics[]> processInput(JsonFilterInputDirectory inputDirectory, JsonFilter filter, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		return processInputOutput(filter, sourceFiles, sourceFiles, properties, predicate, transformer);
	}
	
	private Map<File, DefaultJsonFilterMetrics[]> processInput(JsonFilterInputDirectory inputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		return processInputOutput(maxSize, infiniteSize, sourceFiles, sourceFiles, properties, predicate, transformer);
	}


	protected Map<File, DefaultJsonFilterMetrics[]> processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, JsonFilter filter, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		return processInputOutput(filter, filteredFiles, sourceFiles, properties, predicate, transformer);
	}

	protected Map<File, DefaultJsonFilterMetrics[]> processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Predicate<String> predicate, Function<String, String> transformer) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		return processInputOutput(maxSize, infiniteSize, filteredFiles, sourceFiles, properties, predicate, transformer);
	}

	
	private Map<File, DefaultJsonFilterMetrics[]> processInputOutput(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Map<String, File> filteredFiles, Map<String, File> sourceFiles, Properties properties, Predicate<String> predicate, Function<String, String> transformer) {

		Map<File, DefaultJsonFilterMetrics[]> results = new HashMap<>(); 		

		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			DefaultJsonFilterMetrics[] metrics = new DefaultJsonFilterMetrics[] {new DefaultJsonFilterMetrics(), new DefaultJsonFilterMetrics()};

			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(maxSize, infiniteSize, sourceFile, sourceFile, properties, predicate, transformer, metrics[0]);
				
				compareBytes(maxSize, infiniteSize, sourceFile, sourceFile, properties, predicate, transformer, metrics[1]);
			} else {
				compareChars(maxSize, infiniteSize, sourceFile, filteredFile, properties, predicate, transformer, metrics[0]);
				
				compareBytes(maxSize, infiniteSize, sourceFile, filteredFile, properties, predicate, transformer, metrics[1]);
			}
			
			results.put(filteredFile, metrics);
		}
		
		return results;
	}

	
	private Map<File, DefaultJsonFilterMetrics[]> processInputOutput(JsonFilter filter, Map<String, File> filteredFiles, Map<String, File> sourceFiles,
			Properties properties, Predicate<String> predicate, Function<String, String> transformer) {
		
		Map<File, DefaultJsonFilterMetrics[]> results = new HashMap<>(); 		
		
		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			DefaultJsonFilterMetrics[] metrics = new DefaultJsonFilterMetrics[] {new DefaultJsonFilterMetrics(), new DefaultJsonFilterMetrics()};
			
			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(filter, sourceFile, sourceFile, properties, predicate, transformer, metrics[0]);
				compareBytes(filter, sourceFile, sourceFile, properties, predicate, transformer, metrics[1]);
			} else {
				compareChars(filter, sourceFile, filteredFile, properties, predicate, transformer, metrics[0]);
				compareBytes(filter, sourceFile, filteredFile, properties, predicate, transformer, metrics[1]);
			}
			
			results.put(filteredFile, metrics);
		}
		
		return results;
	}

	protected void compareChars(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer, JsonFilterMetrics metrics) {
		String from = cache.getFile(sourceFile);
		
		String fromTransformed = transformer.apply(from);
		if(!predicate.test(fromTransformed)) {
			return;
		}
		String expected = cache.getFile(filteredFile);

		String input;
		if(expected.length() < fromTransformed.length()) {
			input = fromTransformed;
		} else {
			input = fromTransformed + spaces(expected.length() - fromTransformed.length() + 1);
		}

		JsonFilter maxSize = maxSizeFunction.apply(expected.length());

		StringBuilder maxSizeOutput = new StringBuilder(fromTransformed.length() * 2);
		if(!maxSize.process(input, maxSizeOutput)) {
			System.out.println(sourceFile);
			System.out.println(input);
			throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + filter);
		}
		
		if(maxSize.process(input, metrics) == null) {
			throw new IllegalArgumentException();
		}

		
		String result = maxSizeOutput.toString();

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile + " with max size " + expected.length());
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
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

	protected void compareChars(JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer, JsonFilterMetrics metrics) {
		String from = cache.getFile(sourceFile);
		
		String fromTransformed = transformer.apply(from);
		
		if(!predicate.test(fromTransformed)) {
			return;
		}

		StringBuilder output = new StringBuilder(fromTransformed.length() * 2);
		if(!filter.process(fromTransformed, output)) {
			System.out.println(sourceFile);
			System.out.println(fromTransformed);
			throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
		}
		
		if(!filter.process(fromTransformed, new StringBuilder(fromTransformed.length() * 2), metrics)) {
			throw new IllegalArgumentException("Unable to process using metrics");
		}

		String result = output.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
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
	
	protected void compareBytes(JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer, JsonFilterMetrics metrics) {
		String from = cache.getFile(sourceFile);
		
		String fromTransformed = transformer.apply(from);
		if(!predicate.test(fromTransformed)) {
			return;
		}
		
		byte[] fromTransformedBytes = fromTransformed.getBytes(StandardCharsets.UTF_8);
		
		byte[] process = filter.process(fromTransformedBytes);
		if(process == null) {
			System.out.println("Unable to process " + sourceFile);
			System.out.println(fromTransformed);

			throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
		}
		
		if(filter.process(fromTransformedBytes, metrics) == null) {
			throw new IllegalArgumentException("Unable to process using metrics");
		}


		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(fromTransformed);

		String result = new String(process);

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expected) : expected;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}


	protected void compareBytes(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties, Predicate<String> predicate, Function<String, String> transformer, JsonFilterMetrics metrics) {
		String from = cache.getFile(sourceFile);
		
		String fromTransformed = transformer.apply(from);
		if(!predicate.test(fromTransformed)) {
			return;
		}
		byte[] fromTransformedBytes = fromTransformed.getBytes(StandardCharsets.UTF_8);

		byte[] expected = cache.getFile(filteredFile).getBytes(StandardCharsets.UTF_8);

		byte[] input;
		if(expected.length < fromTransformedBytes.length) {
			input = fromTransformedBytes;
		} else {
			input = spaces(fromTransformedBytes, expected.length - fromTransformedBytes.length + 1);
		}

		JsonFilter maxSize = maxSizeFunction.apply(expected.length);		

		byte[] maxSizeOutput = maxSize.process(input);
		if(maxSizeOutput == null) {
			System.out.println("Unable to process max size " + sourceFile);
			System.out.println(fromTransformedBytes);
			throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + maxSize);
		}
		
		if(maxSize.process(input, metrics) == null) {
			throw new IllegalArgumentException();
		}

		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(fromTransformed);

		String result = new String(maxSizeOutput);
		String expectedChars = new String(expected);
		
		if(isWellformed(result, jsonFactory) != isWellformed(expectedChars, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expectedChars);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expectedChars) : expectedChars;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expectedChars);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, fromTransformed, result, expectedChars);
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

	protected void printDiff(JsonFilter filter, Properties properties, File expectedFile, File original, String from, String fromTransformed, String result, String expected) {
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
		System.out.println("Actual:\n" + result);
		System.out.println("(size " + expected.length() + " vs " + result.length() + ")");

		for(int k = 0; k < Math.min(expected.length(), result.length()); k++) {
			if(expected.charAt(k) != result.charAt(k)) {
				System.out.println("Diff at " + k + ": " + expected.charAt(k) + " vs + " + result.charAt(k));

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
