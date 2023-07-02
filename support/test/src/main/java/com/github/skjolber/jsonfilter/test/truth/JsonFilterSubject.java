package com.github.skjolber.jsonfilter.test.truth;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.JsonFilterDirectoryUnitTestCollection;
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


	private String toString(Object other) {
		String otherAsString;
		if(other instanceof byte[]) {
			otherAsString = new String((byte[])other, StandardCharsets.UTF_8);
		} else if(other instanceof String) {
			otherAsString = (String)other;
		} else if(other instanceof char[]) {
			otherAsString = new String((char[])other);
		} else if(other instanceof StringBuilder) {
			otherAsString = other.toString();
		} else {
			failWithActual(simpleFact("expected to be instance of String, StringBuilder, char[] or byte[]"));
			return null;
		}
		return otherAsString;
	}



}
