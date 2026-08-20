package com.github.skjolber.jsonfilter.simdjson;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class SimdJsonJsonFilterFactoryTest {

	@Test
	public void testDefaultFilter() {
		SimdJsonJsonFilterFactory factory = SimdJsonJsonFilterFactory.newInstance();
		JsonFilter filter = factory.newJsonFilter();
		assertNotNull(filter);
		assertTrue(filter instanceof DefaultJsonFilter);
	}

	@Test
	public void testMaxStringLengthFilter() {
		SimdJsonJsonFilterFactory factory = SimdJsonJsonFilterFactory.newInstance();
		factory.setMaxStringLength(10);
		JsonFilter filter = factory.newJsonFilter();
		assertTrue(filter instanceof SimdJsonMaxStringLengthJsonFilter);
	}

	@Test
	public void testAnyPathFilter() {
		SimdJsonJsonFilterFactory factory = SimdJsonJsonFilterFactory.newInstance();
		factory.setAnonymize(new java.util.ArrayList<>(java.util.Arrays.asList("$..password")));
		JsonFilter filter = factory.newJsonFilter();
		assertTrue(filter instanceof SimdJsonAnyPathMaxStringLengthJsonFilter);
	}

	@Test
	public void testPathFilter() {
		SimdJsonJsonFilterFactory factory = SimdJsonJsonFilterFactory.newInstance();
		factory.setAnonymize(new java.util.ArrayList<>(java.util.Arrays.asList("/user/password")));
		JsonFilter filter = factory.newJsonFilter();
		assertTrue(filter instanceof SimdJsonPathMaxStringLengthJsonFilter);
	}

	@Test
	public void testBuilderAnonymizeKeys() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.anonymizeKeys("password", "ssn");
		assertNotNull(filter);
	}

	@Test
	public void testBuilderPruneKeys() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.pruneKeys("sensitive");
		assertNotNull(filter);
	}

	@Test
	public void testBuilderAnonymizeKeysSet() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.anonymizeKeys(java.util.Set.of("password"));
		assertNotNull(filter);
	}

	@Test
	public void testBuilderAnonymizeKeysWithMaxStringLength() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.anonymizeKeys(java.util.Set.of("password"), 128);
		assertNotNull(filter);
	}

	@Test
	public void testBuilderPruneKeysSet() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.pruneKeys(java.util.Set.of("meta"));
		assertNotNull(filter);
	}

	@Test
	public void testBuilderPruneKeysWithMaxStringLength() {
		JsonFilter filter = SimdJsonJsonLogFilterBuilder.pruneKeys(java.util.Set.of("meta"), 64);
		assertNotNull(filter);
	}

}
