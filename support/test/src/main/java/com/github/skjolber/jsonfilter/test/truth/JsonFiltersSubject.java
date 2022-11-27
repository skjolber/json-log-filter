package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.nio.charset.StandardCharsets;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import com.github.skjolber.jsonfilter.test.JsonFilterResult;
import com.github.skjolber.jsonfilter.test.jackson.JsonComparator;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.google.common.base.Objects;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
/**
 * Propositions for {@link JsonFilterResult} subjects.
 *
 */
public class JsonFiltersSubject extends Subject {

	// User-defined entry point
	public static JsonFiltersSubject assertThat(@NullableDecl String result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(result);
	}
	
	public static JsonFiltersSubject assertThat(@NullableDecl byte[] result) {
		return assertAbout(JSON_SUBJECT_FACTORY).that(new String(result, StandardCharsets.UTF_8));
	}

	public static Factory<JsonFiltersSubject, String> jsonFilterResults() {
		return JsonFiltersSubject::new;
	}	

	// Static method for getting the subject factory (for use with assertAbout())
	public static Subject.Factory<JsonFiltersSubject, String> employees() {
		return JSON_SUBJECT_FACTORY;
	}

	// Boiler-plate Subject.Factory for EmployeeSubject
	private static final Subject.Factory<JsonFiltersSubject, String> JSON_SUBJECT_FACTORY = JsonFiltersSubject::new;	

	protected final String actual;

	/**
	 * Constructor for use by subclasses. If you want to create an instance of this class itself, call
	 * {@link Subject#check(String, Object...) check(...)}{@code .that(actual)}.
	 */

	private JsonFiltersSubject(FailureMetadata metadata, @NullableDecl String map) {
		super(metadata, map);
		this.actual = map;
	}

	@Override
	public final void isEqualTo(@NullableDecl Object other) {
		if(other instanceof byte[]) {
			String otherString = new String((byte[])other, StandardCharsets.UTF_8);
			if (Objects.equal(actual, otherString)) {
				return;
			}
		} else if (Objects.equal(actual, other)) {
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

	/** Fails JSON is not well formed
	 * 
	 * @return this  
	 */
	public final JsonFiltersSubject isWellformed() {
		if (!JsonValidator.isWellformed(actual)) {
			failWithoutActual(simpleFact("expected wellformed"));
		}
		
		return this;
	}
	/** Fails JSON events are not the same
	 * 
	 * @param other JSON
	 * @return this  
	 */
	public final JsonFiltersSubject isEqualEventsAs(String other) {
		if (!JsonComparator.isSameEvents(actual, other)) {
			failWithoutActual(simpleFact("expected equal events"));
		}
		
		return this;
	}

	/** Fails JSON events are not the same
	 * 
	 * @param other JSON
	 * @return this  
	 */
	public final JsonFiltersSubject isEqualEventsAs(byte[] other) {
		if (!JsonComparator.isSameEvents(actual, new String(other, StandardCharsets.UTF_8))) {
			failWithoutActual(simpleFact("expected equal events"));
		}
		
		return this;
	}

}
