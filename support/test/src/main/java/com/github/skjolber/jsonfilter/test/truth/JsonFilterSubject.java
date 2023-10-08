package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.JsonFilterDirectoryUnitTestCollection;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;
import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterDirectoryUnitTestCollection} subjects.
 *
 */
public class JsonFilterSubject extends Subject {

	// User-defined entry point
	public static JsonFilterSubject assertThat(JsonFilter result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}

	public static Factory<JsonFilterSubject, JsonFilter> jsonFilterResults() {
		return JsonFilterSubject::new;
	}	

	private static final Subject.Factory<JsonFilterSubject, JsonFilter> JSON_SUBJECT_FACTORY = JsonFilterSubject::new;	

	protected final JsonFilter actual;
	protected JsonFilterMetrics metrics; 
	protected JsonFile input;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonFilterSubject(FailureMetadata metadata, JsonFilter map) {
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

	public JsonFilterSubject withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}
	
	public JsonFilterSubject withInputFile(JsonFile input) {
		this.input = input;
		return this;
	}

	
	public JsonFilterSubject isPassthrough() {
		JsonFilterNoopAssertion.newInstance().withFilter(actual).withInput(input).withMetrics(metrics).isNoop();
		return this;
	}
	
	public MaxSizeJsonFilterSubject withMaxSizeJsonFilterFunction(MaxSizeJsonFilterFunction delegate) {
		MaxSizeJsonFilterPair pair = new MaxSizeJsonFilterPair(actual, delegate);
		
		return MaxSizeJsonFilterSubject.assertThatJson(pair).withInputFile(input).withMetrics(metrics);
	}

	public void filtersTo(JsonFile jsonOutput, JsonComparisonType comparison) {
		JsonFilterAssertion.newBuilder().withFilter(actual).withInputFile(input).withMetrics(metrics).isEqualTo(jsonOutput, comparison);
	}

}
