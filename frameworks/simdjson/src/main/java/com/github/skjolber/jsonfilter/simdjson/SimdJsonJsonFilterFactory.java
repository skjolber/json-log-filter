package com.github.skjolber.jsonfilter.simdjson;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class SimdJsonJsonFilterFactory extends AbstractJsonFilterFactory {

	public static SimdJsonJsonFilterFactory newInstance() {
		return new SimdJsonJsonFilterFactory();
	}

	@Override
	public JsonFilter newJsonFilter() {
		String pruneJsonValue = this.pruneJsonValue;
		if (pruneJsonValue == null) {
			pruneJsonValue = AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON;
		}
		String anonymizeJsonValue = this.anonymizeJsonValue;
		if (anonymizeJsonValue == null) {
			anonymizeJsonValue = AbstractJsonFilter.FILTER_ANONYMIZE_JSON;
		}
		String truncateStringValue = this.truncateStringValue;
		if (truncateStringValue == null) {
			truncateStringValue = AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;
		}

		String[] pruneFilters = this.pruneFilters.isEmpty() ? null : this.pruneFilters.toArray(new String[0]);
		String[] anonymizeFilters = this.anonymizeFilters.isEmpty() ? null : this.anonymizeFilters.toArray(new String[0]);

		if (isActivePathFilters()) {
			if (!isFullPrefix(anonymizeFilters) && !isFullPrefix(pruneFilters)) {
				return new SimdJsonAnyPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters,
						pruneJsonValue, anonymizeJsonValue, truncateStringValue);
			}
			return new SimdJsonPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters,
					pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		}

		if (isActiveMaxStringLength()) {
			return new SimdJsonMaxStringLengthJsonFilter(maxStringLength, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		}

		return new DefaultJsonFilter();
	}

	protected boolean isFullPrefix(String[] filters) {
		return filters != null && !AbstractPathJsonFilter.hasAnyPrefix(filters);
	}

}
