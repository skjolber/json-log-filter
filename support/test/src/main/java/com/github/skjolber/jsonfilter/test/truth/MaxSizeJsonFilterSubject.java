package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.JsonFilterDirectoryUnitTestCollection;
import com.github.skjolber.jsonfilter.test.cache.JsonFile;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparisonType;
import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterDirectoryUnitTestCollection} subjects.
 *
 */
public class MaxSizeJsonFilterSubject extends Subject {

	// User-defined entry point
	public static MaxSizeJsonFilterSubject assertThatJson(MaxSizeJsonFilterPair result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}

	public static Factory<MaxSizeJsonFilterSubject, MaxSizeJsonFilterPair> jsonFilterResults() {
		return MaxSizeJsonFilterSubject::new;
	}	

	private static final Subject.Factory<MaxSizeJsonFilterSubject, MaxSizeJsonFilterPair> JSON_SUBJECT_FACTORY = MaxSizeJsonFilterSubject::new;	

	protected final MaxSizeJsonFilterPair actual;
	protected JsonFilterMetrics metrics;

	private List<JsonFile> inputs = new ArrayList<>(); 
	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private MaxSizeJsonFilterSubject(FailureMetadata metadata, MaxSizeJsonFilterPair actual) {
		super(metadata, actual);
		this.actual = actual;
	}

	@Override
	public final void isEqualTo(Object other) {
		if (Objects.equal(actual, other)) {
			return;
		}

		failWithActual(simpleFact("expected to be equal"));
	}

	public MaxSizeJsonFilterSubject withMetrics(JsonFilterMetrics metrics) {
		this.metrics = metrics;
		return this;
	}

	public MaxSizeJsonFilterSubject withInputFiles(Collection<JsonFile> input) {
		this.inputs.addAll(input);
		return this;
	}

	public MaxSizeJsonFilterSubject withInputFile(JsonFile input) {
		this.inputs.add(input);
		return this;
	}
	
	public MaxSizeJsonFilterSubject isPassthrough() {
		for(JsonFile input : inputs) {
			MaxSizeJsonFilterNoopAssertion.newInstance().withMaxSizeJsonFilterPair(actual).withInput(input).withMetrics(metrics).isPassthrough();
		}
		return this;
	}

	public MaxSizeJsonFilterSubject filtersTo(JsonFile output) {
		return filtersTo(output, JsonComparisonType.LITERAL);
	}

	public MaxSizeJsonFilterSubject filtersTo(JsonFile output, JsonComparisonType type) {
		for(JsonFile input : inputs) {
			MaxSizeJsonFilterAssertion.newInstance().withMaxSizeJsonFilterPair(actual).withMetrics(metrics).withInputFile(input).filters(output, type);
		}
		return this;
	}

}
