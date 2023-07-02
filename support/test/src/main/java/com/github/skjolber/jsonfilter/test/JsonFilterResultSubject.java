package com.github.skjolber.jsonfilter.test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.util.Arrays;

import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterResult} subjects.
 *
 */
public class JsonFilterResultSubject extends Subject {

	// User-defined entry point
	public static JsonFilterResultSubject assertThat(JsonFilterResult result) {
		return assertAbout(JSON_FILTER_SUBJECT_FACTORY).that(result);
	}

	public static Factory<JsonFilterResultSubject, JsonFilterResult> jsonFilterResults() {
		return JsonFilterResultSubject::new;
	}	

	// Static method for getting the subject factory (for use with assertAbout())
	public static Subject.Factory<JsonFilterResultSubject, JsonFilterResult> employees() {
		return JSON_FILTER_SUBJECT_FACTORY;
	}

	// Boiler-plate Subject.Factory for EmployeeSubject
	private static final Subject.Factory<JsonFilterResultSubject, JsonFilterResult> JSON_FILTER_SUBJECT_FACTORY = JsonFilterResultSubject::new;	

	protected final JsonFilterResult actual;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonFilterResultSubject(FailureMetadata metadata, JsonFilterResult map) {
		super(metadata, map);
		this.actual = map;
	}

	@Override
	public final void isEqualTo(Object other) {
		if (Objects.equal(actual, other)) {
			return;
		}

		failWithActual(simpleFact("expected to be equal"));
	}

	/** Fails if the map is not empty. */
	public final void isEmpty() {
		if (!actual.isEmpty()) {
			failWithActual(simpleFact("expected to be empty"));
		}
	}

	/** Fails if the map is empty. */
	public final void isNotEmpty() {
		if (actual.isEmpty()) {
			failWithoutActual(simpleFact("expected not to be empty"));
		}
	}

	/** Fails if the map does not have the given size.
	 * 
	 * @param expectedSize expected size
	 * @return this  
	 */
	public final JsonFilterResultSubject hasSize(int expectedSize) {
		checkArgument(expectedSize >= 0, "expectedSize (%s) must be >= 0", expectedSize);
		check("size()").that(actual.size()).isEqualTo(expectedSize);
		
		return this;
	}

	public final JsonFilterResultSubject hasProperty(String key, String value) {
		if (!actual.hasPropertyKeyValue(key, value)) {
			failWithoutActual(simpleFact("expected " + key + " " + value));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasProperties(String ... keyValues) {
		if (!actual.hasPropertyKeyValues(keyValues)) {
			failWithoutActual(simpleFact("expected " + Arrays.asList(keyValues)));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasMaxStringLength(int size) {
		if (!actual.hasPropertyKeyValue("maxStringLength", Integer.toString(size))) {
			failWithoutActual(simpleFact("expected maxStringLength " + size));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasMaxStringLengthMetrics() {
		if (!actual.hasMaxStringLengthMetrics()) {
			failWithoutActual(simpleFact("expected maxStringLength"));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasMaxSize(int size) {
		if (!actual.hasPropertyKeyValue("maxSize", Integer.toString(size))) {
			failWithoutActual(simpleFact("expected maxSize " + size));
		}
		return this;
	}

	public final JsonFilterResultSubject hasMaxSizeMetrics() {
		if (!actual.hasMaxSizeMetrics()) {
			failWithoutActual(simpleFact("expected maxSize"));
		}
		return this;
	}

	public final JsonFilterResultSubject hasPruned(String ... paths) {
		Object value;
		if(paths.length == 1) {
			value = paths[0];
		} else {
			value = Arrays.asList(paths);
		}
		if (!actual.hasPropertyKeyValue("pruneFilters", value)) {
			failWithoutActual(simpleFact("expected prune filters " + value));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasPruneMetrics() {
		if (!actual.hasPruneMetrics()) {
			failWithoutActual(simpleFact("expected prune"));
		}
		return this;
	}

	public final JsonFilterResultSubject hasNotPruned() {
		if (actual.hasPropertyKeyValue("pruneFilters", null)) {
			failWithoutActual(simpleFact("expected no prune filters"));
		}

		return this;
	}

	public final JsonFilterResultSubject hasNotAnonymized() {
		if (actual.hasPropertyKeyValue("anonymizeFilters", null)) {
			failWithoutActual(simpleFact("expected no anonymize filters"));
		}

		return this;
	}

	public final JsonFilterResultSubject hasAnonymized(String ... paths) {
		Object value;
		if(paths.length == 1) {
			value = paths[0];
		} else {
			value = Arrays.asList(paths);
		}
		if (!actual.hasPropertyKeyValue("anonymizeFilters", value)) {
			failWithoutActual(simpleFact("expected anonymize filters " + value));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasAnonymizeMetrics() {
		if (!actual.hasAnonymizeMetrics()) {
			failWithoutActual(simpleFact("expected anonymize"));
		}
		return this;
	}
	
	public final JsonFilterResultSubject hasPassthrough() {
		if (!actual.hasPassthrough()) {
			failWithoutActual(simpleFact("expected at least one passthrough result"));
		}
		return this;
	}

	public JsonFilterResultSubject hasMaxPathMatches(int size) {
		if (!actual.hasPropertyKeyValue("maxPathMatches", Integer.toString(size))) {
			failWithoutActual(simpleFact("expected maxPathMatches " + size));
		}
		return this;
	}	

}
