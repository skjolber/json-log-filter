package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterRunner {

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

	public JsonFilterResult process(JsonFilter filter) throws Exception {
		JsonFilterResult result = new JsonFilterResult();

		JsonFilterProperties properties = jsonFilterPropertiesFactory.createInstance(filter);
		if(!properties.isNoop()) {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();
				if(inputDirectory.matches(properties)) {
					processInputOutput(inputDirectory, outputDirectory, filter);

					result.addFiltered(outputDirectory);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, filter);

				Map<String, File> files = outputDirectory.getFiles();
				processInputOutput(filter, files, files, properties.getProperties());

				result.addPassthrough(outputDirectory);
			}
		}

		return result;
	}

	public JsonFilterResult process(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) throws Exception {
		JsonFilterResult result = new JsonFilterResult();

		JsonFilterProperties properties = jsonFilterPropertiesFactory.createInstance(infiniteSize);
		if(!properties.isNoop()) {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();
				if(inputDirectory.matches(properties)) {
					processInputOutput(inputDirectory, outputDirectory, maxSize, infiniteSize);

					result.addFiltered(outputDirectory);
				}
			}
		} else {
			for(JsonFilterOutputDirectory outputDirectory : outputDirectories) {
				JsonFilterInputDirectory inputDirectory = outputDirectory.getInputDirectories();

				processInput(inputDirectory, maxSize, infiniteSize);

				Map<String, File> files = outputDirectory.getFiles();
				processInputOutput(maxSize, infiniteSize, files, files, properties.getProperties());

				result.addPassthrough(outputDirectory);
			}
		}

		return result;
	}

	
	private void processInput(JsonFilterInputDirectory inputDirectory, JsonFilter filter) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(filter, sourceFiles, sourceFiles, properties);
	}
	
	private void processInput(JsonFilterInputDirectory inputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) {
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(maxSize, infiniteSize, sourceFiles, sourceFiles, properties);
	}


	protected void processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, JsonFilter filter) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(filter, filteredFiles, sourceFiles, properties);
	}

	protected void processInputOutput(JsonFilterInputDirectory inputDirectory, JsonFilterOutputDirectory outputDirectory, Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize) {
		Map<String, File> filteredFiles = outputDirectory.getFiles();
		Map<String, File> sourceFiles = inputDirectory.getFiles();

		Properties properties = inputDirectory.getProperties();

		processInputOutput(maxSize, infiniteSize, filteredFiles, sourceFiles, properties);
	}

	
	private void processInputOutput(Function<Integer, JsonFilter> maxSize, JsonFilter infiniteSize, Map<String, File> filteredFiles, Map<String, File> sourceFiles, Properties properties) {

		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(maxSize, infiniteSize, sourceFile, sourceFile, properties);
				
				compareBytes(maxSize, infiniteSize, sourceFile, sourceFile, properties);
			} else {
				compareChars(maxSize, infiniteSize, sourceFile, filteredFile, properties);
				
				compareBytes(maxSize, infiniteSize, sourceFile, filteredFile, properties);
			}
		}
		
	}

	
	private void processInputOutput(JsonFilter filter, Map<String, File> filteredFiles, Map<String, File> sourceFiles,
			Properties properties) {
		for (Entry<String, File> entry : sourceFiles.entrySet()) {
			File sourceFile = entry.getValue();

			File filteredFile = filteredFiles.get(entry.getKey());

			if(filteredFile == null) {
				// no filtered file, so expect passthrough
				compareChars(filter, sourceFile, sourceFile, properties);
				compareBytes(filter, sourceFile, sourceFile, properties);
			} else {
				compareChars(filter, sourceFile, filteredFile, properties);
				compareBytes(filter, sourceFile, filteredFile, properties);
			}
		}
	}

	protected void compareChars(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties) {
		
		String from = cache.getFile(sourceFile);

		StringBuilder infiniteOutput = new StringBuilder(from.length() * 2);
		if(!filter.process(from, infiniteOutput)) {
			System.out.println(sourceFile);
			System.out.println(from);
			throw new IllegalArgumentException("Unable to process infinite size " + sourceFile + " using " + filter);
		}
		
		JsonFilter maxSize = maxSizeFunction.apply(infiniteOutput.length());

		StringBuilder maxSizeOutput = new StringBuilder(from.length() * 2);
		if(!maxSize.process(from, maxSizeOutput)) {
			System.out.println(sourceFile);
			System.out.println(from);
			throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + filter);
		}

		String result = maxSizeOutput.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile + " with max size " + infiniteOutput.length());
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}

	protected void compareChars(JsonFilter filter, File sourceFile, File filteredFile, Properties properties) {
		String from = cache.getFile(sourceFile);

		StringBuilder output = new StringBuilder(from.length() * 2);
		if(!filter.process(from, output)) {
			System.out.println(sourceFile);
			System.out.println(from);
			throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
		}

		String result = output.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		if(literal) {
			if(!new String(expected).equals(result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(expected), result)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
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
	
	protected void compareBytes(JsonFilter filter, File sourceFile, File filteredFile, Properties properties) {
		String from = cache.getFile(sourceFile);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			if(!filter.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), output)) {
				System.out.println("Unable to process " + sourceFile);
				System.out.println(from);

				filter.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), output);

				throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(from);

		String result = output.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expected) : expected;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		}
	}


	protected void compareBytes(Function<Integer, JsonFilter> maxSizeFunction, JsonFilter filter, File sourceFile, File filteredFile, Properties properties) {
		
		String from = cache.getFile(sourceFile);

		ByteArrayOutputStream infiniteOutput = new ByteArrayOutputStream();
		try {
			if(!filter.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), infiniteOutput)) {
				System.out.println("Unable to process " + sourceFile);
				System.out.println(from);

				filter.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), infiniteOutput);

				throw new IllegalArgumentException("Unable to process " + sourceFile + " using " + filter);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		JsonFilter maxSize = maxSizeFunction.apply(infiniteOutput.size());

		ByteArrayOutputStream maxSizeOutput = new ByteArrayOutputStream();
		try {
			if(!maxSize.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), maxSizeOutput)) {
				System.out.println("Unable to process max size " + sourceFile);
				System.out.println(from);

				filter.process(new ByteArrayInputStream(from.getBytes(StandardCharsets.UTF_8)), maxSizeOutput);

				throw new IllegalArgumentException("Unable to process max size " + sourceFile + " using " + maxSize);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
		// this will break "truncated by XX"
		// because it is bytes vs chars
		boolean surrogates = isSurrogates(from);

		String result = infiniteOutput.toString();

		String expected = cache.getFile(filteredFile);

		if(isWellformed(result, jsonFactory) != isWellformed(expected, jsonFactory)) {
			printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
			throw new IllegalArgumentException("Unexpected result for " + sourceFile);
		}

		String filteredResult = surrogates ? filterSurrogates(result) : result;
		String filteredExpected = surrogates ? filterSurrogates(expected) : expected;

		if(literal) {
			if(!new String(filteredExpected).equals(filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
				throw new IllegalArgumentException("Unexpected result for " + sourceFile);
			}
		} else {
			// compare events
			if(!parseCompare(new String(filteredExpected), filteredResult)) {
				printDiff(filter, properties, filteredFile, sourceFile, from, result, expected);
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

			}
		} catch(Exception e) {
			return false;
		}

		return true;
	}

	protected void printDiff(JsonFilter filter, Properties properties, File expectedFile, File original, String from, String to, String expected) {
		System.out.println("Processing using: " + filter);
		System.out.println("Properties: " + properties);
		if(expectedFile.equals(original)) {
			System.out.println("File input/output: " + original);
		} else {
			System.out.println("File input: " + original);
			System.out.println("File expected output: " + expectedFile);
		}
		System.out.println("From: \n" + from);
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
