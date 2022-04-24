package com.github.skjolber.jsonfilter.base;

import static com.github.skjolber.jsonfilter.test.JsonFilterConstants.INVALID_PATH;
import static com.github.skjolber.jsonfilter.test.JsonFilterConstants.PASSTHROUGH_XPATH;
import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.test.JsonFilterRunner;

public class AbstractSinglePathJsonFilterTest {

	public class DefaultSinglePathJsonFilter extends AbstractSinglePathJsonFilter {
	
		public DefaultSinglePathJsonFilter(int maxScalarLength, String expression, FilterType type) {
			super(maxScalarLength, -1, -1, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			if(JsonFilterRunner.isWellformed(new String(chars, offset, length))) {
				output.append(chars, offset, length);
				
				return true;
			}
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
			if(JsonFilterRunner.isWellformed(new String(chars, offset, length))) {
				output.write(chars, offset, length);
				
				return true;
			}
			return false;
		}
		
	};

	@Test
	public void construct_invalidAnonymizePath_throwsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new DefaultSinglePathJsonFilter(-1, INVALID_PATH, FilterType.ANON);
		});
	}
	
	@Test
	public void construct_invalidPrunePath_throwsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new DefaultSinglePathJsonFilter(-1, INVALID_PATH, FilterType.PRUNE);
		});
	}
	
	@Test
	public void construct_validPrunePath_constructed() {
		DefaultSinglePathJsonFilter filter = new DefaultSinglePathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.PRUNE);
		assertThat(filter.getPruneFilters()[0]).isEqualTo(PASSTHROUGH_XPATH);
		assertThat(filter.getAnonymizeFilters().length).isEqualTo(0);
	}

	@Test
	public void construct_validAnonymizePath_constructed() {
		DefaultSinglePathJsonFilter filter = new DefaultSinglePathJsonFilter(-1, PASSTHROUGH_XPATH, FilterType.ANON);

		assertThat(filter.getAnonymizeFilters()[0]).isEqualTo(PASSTHROUGH_XPATH);
		assertThat(filter.getPruneFilters().length).isEqualTo(0);
	}
	
	
}
