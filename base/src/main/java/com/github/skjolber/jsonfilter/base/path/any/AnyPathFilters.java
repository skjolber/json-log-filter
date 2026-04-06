package com.github.skjolber.jsonfilter.base.path.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyPathFilters {

	public static AnyPathFilters create(AnyPathFilter ... any) {
		return create(Arrays.asList(any));
	}

	public static AnyPathFilters create(List<AnyPathFilter> any) {
		Map<String, FilterType> map = new HashMap<>();
		int maxKeyLengthBytes = 0;
		int maxKeyLengthChars = 0;
		for(int i = 0; i < any.size(); i++) {
			AnyPathFilter anyPathFilter = any.get(i);
			if(maxKeyLengthBytes < anyPathFilter.pathBytes.length) {
				maxKeyLengthBytes = anyPathFilter.pathBytes.length;
			}
			if(maxKeyLengthChars < anyPathFilter.pathChars.length) {
				maxKeyLengthChars = anyPathFilter.pathChars.length;
			}
			map.put(anyPathFilter.pathString, anyPathFilter.filterType);
		}

		// flat structures (length → filters) are needed by fillEncoded
		AnyPathFilter[][] flatFiltersBytes = fillFlat(any, maxKeyLengthBytes, false);
		AnyPathFilter[][] flatFiltersChars = fillFlat(any, maxKeyLengthChars, true);

		// 3D structures add a first-byte dimension for O(1) exact lookup
		AnyPathFilter[][][] exactFiltersBytes = fillExact(any, maxKeyLengthBytes, false);
		AnyPathFilter[][][] exactFiltersChars = fillExact(any, maxKeyLengthChars, true);

		AnyPathFilter[][] encodedFiltersBytes = fillEncoded(flatFiltersBytes);
		AnyPathFilter[][] encodedFiltersChars = fillEncoded(flatFiltersChars);
		
		return new AnyPathFilters(exactFiltersBytes, exactFiltersChars, encodedFiltersBytes, encodedFiltersChars, map);
	}

	/** Builds a flat length-indexed structure used by {@link #fillEncoded}. */
	@SuppressWarnings("unchecked")
	private static AnyPathFilter[][] fillFlat(List<AnyPathFilter> any, int maxLength, boolean useChars) {
		List<AnyPathFilter>[] output = new List[maxLength + 1];
		for(int i = 0; i < output.length; i++) {
			output[i] = new ArrayList<>();
		}
		for(AnyPathFilter filter : any) {
			int len = useChars ? filter.pathChars.length : filter.pathBytes.length;
			output[len].add(filter);
		}
		AnyPathFilter[][] result = new AnyPathFilter[maxLength + 1][];
		for(int i = 0; i < result.length; i++) {
			if(!output[i].isEmpty()) {
				result[i] = output[i].toArray(new AnyPathFilter[0]);
			}
		}
		result[0] = filters.toArray(new AnyPathFilter[0]);
		
		return result;
	}

	/**
	 * Builds a 3D structure: {@code [length][firstByte & 0xFF] → AnyPathFilter[]}.
	 * <p>Within each length bucket, filters are partitioned by the first byte (or char)
	 * of their path value.  A lookup then needs to scan only the sub-bucket whose first
	 * byte matches the candidate key, reducing comparisons from O(N) to O(N / alphabet).
	 */
	private static AnyPathFilter[][][] fillExact(List<AnyPathFilter> any, int maxLength, boolean useChars) {
		AnyPathFilter[][][] result = new AnyPathFilter[maxLength + 1][][];
		for(AnyPathFilter filter : any) {
			int len  = useChars ? filter.pathChars.length : filter.pathBytes.length;
			int first = useChars
					? (filter.pathChars.length > 0 ? filter.pathChars[0] & 0xFF : 0)
					: (filter.pathBytes.length > 0 ? filter.pathBytes[0] & 0xFF : 0);
			if(result[len] == null) {
				result[len] = new AnyPathFilter[256][];
			}
			AnyPathFilter[] existing = result[len][first];
			if(existing == null) {
				result[len][first] = new AnyPathFilter[]{filter};
			} else {
				AnyPathFilter[] updated = Arrays.copyOf(existing, existing.length + 1);
				updated[existing.length] = filter;
				result[len][first] = updated;
			}
		}
		return result;
	}

	private static AnyPathFilter[][] fillEncoded(AnyPathFilter[][] flatFilters) {
		AnyPathFilter[][] result = new AnyPathFilter[flatFilters.length + 1][];
		List<AnyPathFilter> filters = new ArrayList<>();
		for(int i = flatFilters.length - 1; i >= 0; i--) {
			if(flatFilters[i] != null) {
				for(AnyPathFilter f : flatFilters[i]) {
					filters.add(f);
				}
			}
			result[i + 1] = filters.toArray(new AnyPathFilter[0]);
		}
		return result;
	}

	/** Exact-match lookup: {@code [length][firstByte] → candidates}. */
	protected final AnyPathFilter[][][] exactFiltersBytes;
	protected final AnyPathFilter[][][] exactFiltersChars;

	protected final AnyPathFilter[][] encodingFiltersBytes;
	protected final AnyPathFilter[][] encodingFiltersChars;

	protected final Map<String, FilterType> names;
	
	public AnyPathFilters(AnyPathFilter[][][] exactFiltersBytes, AnyPathFilter[][][] exactFiltersChars,
			AnyPathFilter[][] encodingFiltersBytes, AnyPathFilter[][] encodingFiltersChars, Map<String, FilterType> names) {
		super();
		this.exactFiltersBytes = exactFiltersBytes;
		this.exactFiltersChars = exactFiltersChars;
		this.encodingFiltersBytes = encodingFiltersBytes;
		this.encodingFiltersChars = encodingFiltersChars;
		this.names = names;
	}

	public FilterType matchPath(String key) {
		return names.get(key);
	}
	
	public FilterType matchPath(final byte[] chars, int start, int end) {
		int length = end - start;
		if(length > 0 && length < exactFiltersBytes.length) {
			AnyPathFilter[][] lengthBucket = exactFiltersBytes[length];
			if(lengthBucket != null) {
				AnyPathFilter[] candidates = lengthBucket[chars[start] & 0xFF];
				if(candidates != null) {
					FilterType type = unencodedMatchBytes(candidates, chars, start);
					if(type != null) {
						return type;
					}
				}
			}
		}

		// check for encoded key (rare: JSON field names with Unicode escapes)
		for(int i = start; i < end; i++) {
			if(chars[i] == '\\') {
				int readLength = i - start;
				if(readLength >= encodingFiltersBytes.length) {
					return null;
				}
				for(AnyPathFilter filter : encodingFiltersBytes[readLength]) {
					if(AbstractMultiPathJsonFilter.matchesEncoded(chars, start, end, filter.pathBytes)) {
						return filter.getFilterType();
					}
				}
			}
		}
		
		return null;
	}
	
	public FilterType matchPath(final char[] chars, int start, int end) {
		int length = end - start;
		if(length > 0 && length < exactFiltersChars.length) {
			AnyPathFilter[][] lengthBucket = exactFiltersChars[length];
			if(lengthBucket != null) {
				AnyPathFilter[] candidates = lengthBucket[chars[start] & 0xFF];
				if(candidates != null) {
					FilterType type = unencodedMatchChars(candidates, chars, start);
					if(type != null) {
						return type;
					}
				}
			}
		}

		// check for encoded key, which should be quite rare
		for(int i = start; i < end; i++) {
			if(chars[i] == '\\') {
				int readLength = i - start;
				if(readLength >= encodingFiltersChars.length) {
					return null;
				}
				for(AnyPathFilter filter : encodingFiltersChars[readLength]) {
					if(AbstractMultiPathJsonFilter.matchesEncoded(chars, start, end, filter.pathChars)) {
						return filter.getFilterType();
					}
				}
			}
		}
		
		return null;
	}

	private static FilterType unencodedMatchBytes(AnyPathFilter[] candidates, final byte[] chars, int start) {
		main:
		for(int i = 0; i < candidates.length; i++) {
			byte[] pathBytes = candidates[i].pathBytes;
			for(int k = 0; k < pathBytes.length; k++) {
				if(pathBytes[k] != chars[start + k]) {
					continue main;
				}
			}
			return candidates[i].filterType;
		}
		return null;
	}

	private static FilterType unencodedMatchChars(AnyPathFilter[] candidates, final char[] chars, int start) {
		main:
		for(int i = 0; i < candidates.length; i++) {
			char[] pathChars = candidates[i].pathChars;
			for(int k = 0; k < pathChars.length; k++) {
				if(pathChars[k] != chars[start + k]) {
					continue main;
				}
			}
			return candidates[i].filterType;
		}
		return null;
	}

}