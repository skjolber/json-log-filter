package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class AbstractJsonFilterSymmetryAssertion {

	protected void assertEquals(Path source, String input, String outputAsString, byte[] outputAsBytes) {
		if(outputAsString == null && outputAsBytes == null) {
			return;
		}
		if(outputAsString != null && outputAsBytes == null) {
			fail("Expected equal result for " + source + ", but there was no byte output");
		}
		if(outputAsString == null && outputAsBytes != null) {
			fail("Expected equal result for " + source + ", but there was no char output");
		}
		
		String outputAsBytesAsString = new String(outputAsBytes, StandardCharsets.UTF_8);
		if(outputAsBytesAsString.equals(outputAsString)) {
			return;
		}
		
		// for unicode and max string size, bytes and chars are counted somewhat differently
		String outputAsBytesAsNormalizedString = JsonNormalizer.normalize(outputAsBytesAsString);
		String outputAsStringAsNormalizedString = JsonNormalizer.normalize(outputAsString);
		if(outputAsBytesAsNormalizedString.equals(outputAsStringAsNormalizedString)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("Expected equal result\n");
		builder.append(input);
		builder.append("\n");
		
		builder.append(JsonComparator.printDiff(outputAsBytesAsNormalizedString, outputAsStringAsNormalizedString));
		
		fail(builder.toString());
	}
	
	protected void assertEquals(Path source, String outputAsString1, String outputAsString2) {
		if(outputAsString1 == null && outputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && outputAsString2 == null) {
			fail("Expected symmertic result for " + source + ", but there was no output 2");
		}
		if(outputAsString1 == null && outputAsString2 != null) {
			fail("Expected symmertic result for " + source + ", but there was no output 1");
		}
		
		if(outputAsString1.equals(outputAsString1)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(outputAsString1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(outputAsString2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		String printDiff = JsonComparator.printDiff(outputAsString1, outputAsString2);
		
		fail("Expected symmertic result\n" + printDiff);
	}

	protected void assertEquals(Path source, String input1, byte[] input2, String outputAsString1, byte[] outputAsBytes2) {
		assertEquals(source, input1, new String(input2, StandardCharsets.UTF_8), outputAsString1, new String(outputAsBytes2, StandardCharsets.UTF_8));
	}

	protected void assertEquals(Path source, String input1, String input2, String outputAsString1, String outputAsString2) {
		if(outputAsString1 == null && outputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && outputAsString2 == null) {
			fail("Expected symmertic result for " + source + ", but there was no pretty printed output");
		}
		if(outputAsString1 == null && outputAsString2 != null) {
			fail("Expected symmertic result for " + source + ", but there was no output");
		}
		
		if(outputAsString1.equals(outputAsString1)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(outputAsString1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(outputAsString2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		if(Objects.equals(input1, input2)) {
			builder.append("Expected equal result\n");
			builder.append(input1);
			builder.append("\n");
		} else {
			builder.append("Expected pretty-printed symmertic result\n");
			builder.append(input1);
			builder.append("\n");
			builder.append(input2);
			builder.append("\n");
		}
		
		builder.append(JsonComparator.printDiff(outputAsString1, outputAsString2));
		
		fail(builder.toString());
	}
	
	
	protected void assertEquals(Path source, byte[] input1, byte[] input2, byte[] outputAsString1, byte[] outputAsString2) {
		if(outputAsString1 == null && outputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && outputAsString2 == null) {
			fail("Expected symmertic result for " + source + ", but there was no output 2");
		}
		if(outputAsString1 == null && outputAsString2 != null) {
			fail("Expected symmertic result for " + source + ", but there was no output 1");
		}
		
		if(outputAsString1.equals(outputAsString1)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(new String(outputAsString1, StandardCharsets.UTF_8));
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(new String(outputAsString2, StandardCharsets.UTF_8));
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		if(Arrays.equals(input1, input2)) {
			builder.append("Expected equal result\n");
			builder.append(new String(input1, StandardCharsets.UTF_8));
			builder.append("\n");
		} else {
			builder.append("Expected pretty-printed symmertic result\n");
			builder.append(new String(input1, StandardCharsets.UTF_8));
			builder.append("\n");
			builder.append(new String(input2, StandardCharsets.UTF_8));
			builder.append("\n");
		}
		
		builder.append(JsonComparator.printDiff(outputAsString1, outputAsString2));
		
		fail(builder.toString());
	}

	protected void checkSymmetricPrettyPrinted(Path source, byte[] outputAsString1, byte[] outputAsString2) {
		if(outputAsString1 == null && outputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && outputAsString2 == null) {
			fail("Expected symmertic result for " + source + ", but there was no output 2");
		}
		if(outputAsString1 == null && outputAsString2 != null) {
			fail("Expected symmertic result for " + source + ", but there was no output 1");
		}
		
		if(outputAsString1.equals(outputAsString1)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(new String(outputAsString1, StandardCharsets.UTF_8));
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(new String(outputAsString2, StandardCharsets.UTF_8));
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		String printDiff = JsonComparator.printDiff(outputAsString1, outputAsString2);
		
		fail("Expected symmertic result\n" + printDiff);
	}


	protected static void assertEquals(Path source, byte[] inputContentAsBytes, byte[] byteOutput1, byte[] expectedByteOutput2) {
		if(byteOutput1 == null && expectedByteOutput2 == null) {
			return;
		}
		if(byteOutput1 != null && expectedByteOutput2 == null) {
			fail("Expected result for " + source + ", but there was no output 2");
		}
		if(byteOutput1 == null && expectedByteOutput2 != null) {
			fail("Expected result for " + source + ", but there was no output 1");
		}
		
		if(Arrays.equals(byteOutput1, expectedByteOutput2)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(byteOutput1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(expectedByteOutput2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("Expected equal result\n");
		builder.append(new String(inputContentAsBytes, StandardCharsets.UTF_8));
		builder.append("\n");
		
		builder.append(JsonComparator.printDiff(byteOutput1, expectedByteOutput2));
		
		fail(builder.toString());
	}

	protected static void assertEventsEquals(Path source, byte[] inputContentAsBytes, byte[] byteOutput1, byte[] expectedByteOutput2) {
		if(byteOutput1 == null && expectedByteOutput2 == null) {
			return;
		}
		if(byteOutput1 != null && expectedByteOutput2 == null) {
			fail("Expected result for " + source + ", but there was no output 2");
		}
		if(byteOutput1 == null && expectedByteOutput2 != null) {
			fail("Expected result for " + source + ", but there was no output 1");
		}
		
		if(Arrays.equals(byteOutput1, expectedByteOutput2)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(byteOutput1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(expectedByteOutput2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}

		if(JsonComparator.isEventsEqual(outputAsStringAsNormalizedString1, outputAsStringAsNormalizedString2)) {
			return;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("Expected equal events result\n");
		builder.append(new String(inputContentAsBytes, StandardCharsets.UTF_8));
		builder.append("\n");
		
		builder.append(JsonComparator.printDiff(byteOutput1, expectedByteOutput2));
		
		fail(builder.toString());
	}

	protected static void assertEventsEquals(Path source, String inputContentAsString, String outputAsString1, String expectedOutputAsString2) {
		if(outputAsString1 == null && expectedOutputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && expectedOutputAsString2 == null) {
			fail("Expected result for " + source + ", but there was no output 2");
		}
		if(outputAsString1 == null && expectedOutputAsString2 != null) {
			fail("Expected result for " + source + ", but there was no output 1");
		}
		
		if(outputAsString1.equals(expectedOutputAsString2)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(outputAsString1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(expectedOutputAsString2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}

		if(JsonComparator.isEventsEqual(outputAsStringAsNormalizedString1, outputAsStringAsNormalizedString2)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("Expected equal events result\n");
		builder.append(inputContentAsString);
		builder.append("\n");
		
		builder.append(JsonComparator.printDiff(outputAsString1, expectedOutputAsString2));
		
		fail(builder.toString());

	}

	protected static void assertEquals(Path source, String inputContentAsString, String outputAsString1, String expectedOutputAsString2) {
		if(outputAsString1 == null && expectedOutputAsString2 == null) {
			return;
		}
		if(outputAsString1 != null && expectedOutputAsString2 == null) {
			fail("Expected result for " + source + ", but there was no output 2");
		}
		if(outputAsString1 == null && expectedOutputAsString2 != null) {
			fail("Expected result for " + source + ", but there was no output 1");
		}
		
		if(outputAsString1.equals(expectedOutputAsString2)) {
			return;
		}
		
		String outputAsStringAsNormalizedString1 = JsonNormalizer.normalize(outputAsString1);
		String outputAsStringAsNormalizedString2 = JsonNormalizer.normalize(expectedOutputAsString2);
		
		if(outputAsStringAsNormalizedString1.equals(outputAsStringAsNormalizedString2)) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(source.toString());
		builder.append("\n");
		builder.append("Expected equal result\n");
		builder.append(inputContentAsString);
		builder.append("\n");
		
		builder.append(JsonComparator.printDiff(outputAsString1, expectedOutputAsString2));
		
		fail(builder.toString());
	}

}
