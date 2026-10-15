package com.github.skjolber.jsonfilter.jackson;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class JacksonPathMaxSizeMaxStringLengthJsonFilterTest extends AbstractDefaultJacksonJsonFilterTest {

	private static class MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter extends JacksonPathMaxSizeMaxStringLengthJsonFilter {

		public MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize,
				String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
			super(maxStringLength, maxSize, anonymizes, prunes, jsonFactory);
		}

		public MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize,
				String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage,
				String truncateMessage, JsonFactory jsonFactory) {
			super(maxStringLength, maxSize, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
		}

		public MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize,
				String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage,
				String truncateMessage) {
			super(maxStringLength, maxSize, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		public MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize,
				String[] anonymizes, String[] prunes) {
			super(maxStringLength, maxSize, anonymizes, prunes);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public JacksonPathMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null));
	}

	@Test
	public void passthrough_success() throws Exception {		
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, null)).hasPassthrough();
		
		maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasPassthrough();
	}

	@Test
	public void testRootScalarIsKept() throws Exception {
		// dropping the root value would leave an empty (invalid) document
		String json = "\"" + "s".repeat(28) + "\"";
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter = new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 10, new String[]{"/secret"}, null);
		assertEquals(json, filter.process(json));
		assertEquals(json, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}
	
	@Test
	public void anonymize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH}, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH}, null)).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH}, new String[]{PASSTHROUGH_XPATH})).hasAnonymized(DEFAULT_PATH);
		
		maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEEP_PATH1}, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{DEEP_PATH1}, null)).hasAnonymized(DEEP_PATH1);
	}
	
	@Test
	public void anonymizeWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_WILDCARD_PATH}, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_WILDCARD_PATH}, null)).hasAnonymized(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void anonymizeAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, new String[]{DEFAULT_ANY_PATH}, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, new String[]{DEFAULT_ANY_PATH}, null)).hasAnonymized(DEFAULT_ANY_PATH);
	}

	@Test
	public void prune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_PATH, PASSTHROUGH_XPATH})).hasPruned(DEFAULT_PATH);
		
		maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size, null, new String[]{DEEP_PATH3});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, new String[]{DEEP_PATH3})).hasPruned(DEEP_PATH3);
	}

	@Test
	public void pruneWildcard() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_WILDCARD_PATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_WILDCARD_PATH})).hasPruned(DEFAULT_WILDCARD_PATH);
	}
	
	@Test
	public void pruneAny() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, size,  null, new String[]{DEFAULT_ANY_PATH});
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(-1, null, new String[]{DEFAULT_ANY_PATH})).hasPruned(DEFAULT_ANY_PATH);
	}	

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, null, null);
		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, null, null)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void maxStringLengthAnonymizePrune() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size, new String[]{"/key1"}, new String[]{"/key3"});

		assertThat(maxSize, new JacksonPathMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, new String[]{"/key1"}, new String[]{"/key3"}))
			.hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH)
			.hasPruned("/key3")
			.hasAnonymized("/key1");
	}

	@Test
	public void testMaxSizeIgnoresExistingOutput() throws Exception {
		// only what the filter writes counts against the max size, not what is already in the output
		String json = "{\"a\":\"hello\",\"b\":[1,2,3],\"c\":true}";
		String expected = "{\"a\":\"hello\",\"b\":[1,2,3]}";
		String prefix = "P".repeat(100);

		JacksonPathMaxSizeMaxStringLengthJsonFilter filter = new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 30, new String[]{"/secret"}, null);

		char[] chars = json.toCharArray();
		StringBuilder builder = new StringBuilder(prefix);
		assertTrue(filter.process(chars, 0, chars.length, builder));
		assertEquals(prefix + expected, builder.toString());

		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		builder = new StringBuilder(prefix);
		assertTrue(filter.process(bytes, 0, bytes.length, builder));
		assertEquals(prefix + expected, builder.toString());

		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(256);
		output.write(prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8), 0, prefix.length());
		assertTrue(filter.process(bytes, 0, bytes.length, output));
		assertEquals(prefix + expected, new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
	}

	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
			}, 
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
			},
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}				
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			}
		);
		
		testConvenienceMethods(
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
			}, 
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
			},
			new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1, null, null) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			}			
		);
	}

	@Test
	public void testProcessByteArrayToStringBuilderPassthrough() throws Exception {
		// When maxSize >= input length, process(byte[], StringBuilder) delegates to super via mustConstrainMaxSize
		byte[] json = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, null, null);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json, 0, json.length, sb, null));
		assertEquals("{}", sb.toString());
	}

	@Test
	public void testPruneMessagesLongerThanValues() throws Exception {
		// The document fits within max size, but the output does not: each prune message is longer than
		// the value it replaces (46 chars in, 96 chars out without max size). The output is rolled back
		// and the document is filtered again with max size.
		String json = "[{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0}]";
		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		String expected = "[{\"id\":\"[removed]\"},{\"id\":\"[removed]\"},{}]";

		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, json.length(), null, new String[]{"//id"});

		// output written before the document is kept
		StringBuilder sb = new StringBuilder("[1,");
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb, null));
		assertEquals("[1," + expected, sb.toString());

		sb = new StringBuilder("[1,");
		assertTrue(filter.process(bytes, 0, bytes.length, sb, null));
		assertEquals("[1," + expected, sb.toString());

		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(128);
		output.write("[1,".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertEquals("[1," + expected, output.toString(java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	public void testMetricsForPruneMessagesLongerThanValues() throws Exception {
		// The document is first filtered without max size, and again with max size if that output is too large.
		// The metrics then include both passes.
		// Pruned objects are counted (51 chars in, 96 chars out without max size).
		String json = "[{\"id\":{}},{\"id\":{}},{\"id\":{}},{\"id\":{}},{\"id\":{}}]";

		// output too large, filtered again with max size: five objects pruned in the first pass, and two in the second
		assertPruneMetrics(new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, json.length(), null, new String[]{"//id"}), json, 7);

		// output within max size, kept: all five objects pruned
		assertPruneMetrics(new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 96, null, new String[]{"//id"}), json, 5);
	}

	private static void assertPruneMetrics(JacksonPathMaxSizeMaxStringLengthJsonFilter filter, String json, int pruned) {
		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

		com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics metrics =
				new com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder(), metrics));
		assertEquals(pruned, metrics.getPrune());

		metrics = new com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics();
		assertTrue(filter.process(bytes, 0, bytes.length, new StringBuilder(), metrics));
		assertEquals(pruned, metrics.getPrune());

		metrics = new com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics();
		assertTrue(filter.process(bytes, 0, bytes.length, new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(128), metrics));
		assertEquals(pruned, metrics.getPrune());
	}

	@Test
	public void testAnonymizeObjectValue() throws Exception {
		// When the anonymized field has an object value (not scalar), anonymizeChildren is called.
		// The JSON has "/key" -> object {"nested":"value","other":"data"} which exercises
		// the field-name tracking and scalar anonymization inside anonymizeChildren.
		String json = "{\"key\":{\"nested\":\"value\",\"other\":\"data\"}}";
		char[] chars = json.toCharArray();
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, new String[]{"/key"}, null);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, sb, null));
		assertEquals("{\"key\":{\"nested\":\"*\",\"other\":\"*\"}}", sb.toString());
	}

	@Test
	public void testAnonymizeObjectValueExceedsMaxSize() throws Exception {
		// Test anonymizeChildren when the accumulated output size exceeds maxSize,
		// triggering the metrics.onMaxSize branch and early return from anonymizeChildren.
		// The output contains the opened object for "key" but none of its children,
		// because the size limit was hit before any child value could be written.
		String json = "{\"key\":{\"nested\":\"value\",\"other\":\"more_data\"}}";
		char[] chars = json.toCharArray();
		// Small maxSize causes size limit to be exceeded inside anonymizeChildren
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 15, new String[]{"/key"}, null);
		com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics metrics =
				new com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics();
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, sb, metrics));
		assertEquals("{\"key\":{}}", sb.toString());
		assertEquals(-1, metrics.getMaxSize());
	}

	@Test
	public void testAnonymizeFieldAtHigherIndex() throws Exception {
		// An anonymized scalar field after other fields in its parent object; the comma
		// before it is counted like any other separator.
		String json = "{\"a\":\"x\",\"b\":\"y\",\"key\":\"value\"}";
		char[] chars = json.toCharArray();
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, new String[]{"/key"}, null);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, sb, null));
		assertEquals("{\"a\":\"x\",\"b\":\"y\",\"key\":\"*\"}", sb.toString());
	}

	@Test
	public void testAnonymizeObjectValueAtHigherIndex() throws Exception {
		// An object-valued field after other fields in its parent object, anonymized
		// within; the comma before it is counted like any other separator.
		String json = "{\"a\":\"x\",\"b\":\"y\",\"key\":{\"nested\":\"value\"}}";
		char[] chars = json.toCharArray();
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, new String[]{"/key"}, null);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, sb, null));
		assertEquals("{\"a\":\"x\",\"b\":\"y\",\"key\":{\"nested\":\"*\"}}", sb.toString());
	}

	@Test
	public void testMaxSizeOfAnonymizedValue() throws Exception {
		// separator, quoted field name and colon count, and the field name is two bytes in UTF-8
		String[] anonymize = new String[]{"/æ"};

		// {"a":1,"æ":"*"} is 15 characters, 16 bytes
		String scalar = "{\"a\":1,\"æ\":\"secretvalue\",\"c\":3}";
		assertMaxSize(scalar, 15, anonymize, null, "{\"a\":1,\"æ\":\"*\"}", "{\"a\":1}");
		assertMaxSize(scalar, 14, anonymize, null, "{\"a\":1}", "{\"a\":1}");

		// {"a":1,"æ":{}} is 14 characters, 15 bytes
		String object = "{\"a\":1,\"æ\":{\"x\":2},\"c\":3}";
		assertMaxSize(object, 14, anonymize, null, "{\"a\":1,\"æ\":{}}", "{\"a\":1}");
		assertMaxSize(object, 13, anonymize, null, "{\"a\":1}", "{\"a\":1}");
	}

	@Test
	public void testMaxSizeOfPrunedValue() throws Exception {
		// separator, quoted field name and colon count, and the field name is two bytes in UTF-8
		String[] prune = new String[]{"/æ"};

		// {"a":1,"æ":"[removed]"} is 23 characters, 24 bytes
		for(String json : new String[] {"{\"a\":1,\"æ\":\"secretvalue\",\"c\":3}", "{\"a\":1,\"æ\":{\"x\":2},\"c\":3}"}) {
			assertMaxSize(json, 23, null, prune, "{\"a\":1,\"æ\":\"[removed]\"}", "{\"a\":1}");
			assertMaxSize(json, 22, null, prune, "{\"a\":1}", "{\"a\":1}");
		}
	}

	private static void assertMaxSize(String json, int maxSize, String[] anonymizes, String[] prunes, String expectedChars, String expectedBytes) {
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter = new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, maxSize, anonymizes, prunes);

		assertEquals(expectedChars, filter.process(json), json + " " + maxSize);
		byte[] bytes = filter.process(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertEquals(expectedBytes, new String(bytes, java.nio.charset.StandardCharsets.UTF_8), json + " " + maxSize + " UTF-8");
	}

	@Test
	public void testAnonymizeChildrenMaxSize() throws Exception {
		// separator, quoted field name and colon count, and the second field name is two bytes in UTF-8.
		// {"a":"*","æ":"*" is 16 characters, 17 bytes; the caller reserves room for the closing bracket
		String json = "{\"a\":1,\"æ\":2}";

		assertEquals("{\"a\":\"*\",\"æ\":\"*\"}", anonymizeChildren(json, 16, false));
		assertEquals("{\"a\":\"*\"}", anonymizeChildren(json, 16, true));
		assertEquals("{\"a\":\"*\"}", anonymizeChildren(json, 15, false));
	}

	private static String anonymizeChildren(String json, long maxSize, boolean utf8) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		StringBuilder chars = new StringBuilder();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (
			JsonParser parser = jsonFactory.createParser(json);
			JsonGenerator generator = utf8 ? jsonFactory.createGenerator(bytes) : jsonFactory.createGenerator(new StringBuilderWriter(chars))
			) {
			parser.nextToken();
			generator.writeStartObject();

			LongSupplier outputSize = () -> generator.streamWriteOutputBuffered() + (utf8 ? bytes.size() : chars.length());
			JacksonPathMaxSizeMaxStringLengthJsonFilter.anonymizeChildren(parser, generator, maxSize, outputSize, "\"*\"".toCharArray(), null);
		}
		// closing the generator also closes the object
		return utf8 ? new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8) : chars.toString();
	}

	@Test
	public void testProcessByteArrayResizableStreamPassthrough() throws Exception {
		// When input length <= maxSize, process(byte[], ResizableByteArrayOutputStream, metrics)
		// delegates to super without engaging the size-constraint logic.
		// This previously caused infinite recursion via the interface default method.
		byte[] json = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, null, null);
		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(json, 0, json.length, output, null));
		assertEquals("{}", new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	public void testProcessByteArrayResizableStreamException() throws Exception {
		// Verify catch block in process(byte[], ResizableByteArrayOutputStream) via broken factory
		tools.jackson.core.json.JsonFactory jsonFactory = mock(tools.jackson.core.json.JsonFactory.class);
		when(jsonFactory.createGenerator(any(com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 1, null, null, jsonFactory);
		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(64);
		byte[] json = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		assertFalse(filter.process(json, 0, json.length, output, null));
	}

	@Test
	public void testMultipleRootValuesAreRejected() {
		// max size below the input size, so that the input is not passed on to the path filter
		assertMultipleRootValuesRejected(new JacksonPathMaxSizeMaxStringLengthJsonFilter(-1, multipleRootValues.length() - 1, new String[]{DEFAULT_PATH}, null));
	}

}
