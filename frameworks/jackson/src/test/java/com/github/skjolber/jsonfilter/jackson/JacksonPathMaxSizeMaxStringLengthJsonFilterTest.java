package com.github.skjolber.jsonfilter.jackson;

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
		// A scalar field at index >= 2 in its parent object exercises the `size++` branch
		// for getCurrentIndex() >= 2 in the anon/prune handling.
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
		// An object-valued field at index >= 2 exercises the second `size++` branch
		// for getCurrentIndex() >= 2 when the value is not scalar.
		String json = "{\"a\":\"x\",\"b\":\"y\",\"key\":{\"nested\":\"value\"}}";
		char[] chars = json.toCharArray();
		JacksonPathMaxSizeMaxStringLengthJsonFilter filter =
				new MustContrainJacksonMultiPathMaxSizeMaxStringLengthJsonFilter(-1, 1024, new String[]{"/key"}, null);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, sb, null));
		assertEquals("{\"a\":\"x\",\"b\":\"y\",\"key\":{\"nested\":\"*\"}}", sb.toString());
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
	
}
