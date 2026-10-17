package com.github.skjolber.jsonfilter.base.path.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyPathFilters {

	public static AnyPathFilters create(AnyPathFilter ... any) {
		return create(Arrays.asList(any));
	}

	public static AnyPathFilters create(List<AnyPathFilter> any) {
		// if the same name is both pruned and anonymized, prune takes precedence
		Map<String, FilterType> map = new LinkedHashMap<>();
		for(AnyPathFilter anyPathFilter : any) {
			map.merge(anyPathFilter.pathString, anyPathFilter.filterType, FilterType::strongest);
		}
		if(map.size() < any.size()) {
			// one filter per name, so that all lookups give the same result
			List<AnyPathFilter> unique = new ArrayList<>(map.size());
			for(Map.Entry<String, FilterType> entry : map.entrySet()) {
				unique.add(new AnyPathFilter(entry.getKey(), entry.getValue()));
			}
			any = unique;
		}
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
		// result[0] covers keys whose very first character is a backslash (escape at offset 0).
		// All non-empty filters are candidates in that case.
		result[0] = filters.toArray(new AnyPathFilter[0]);
		return result;
	}

	/** Exact-match lookup: {@code [length][firstByte] → candidates}. */
	protected final AnyPathFilter[][][] exactFiltersBytes;
	protected final AnyPathFilter[][][] exactFiltersChars;

	protected final AnyPathFilter[][] encodingFiltersBytes;
	protected final AnyPathFilter[][] encodingFiltersChars;

	protected final Map<String, FilterType> names;

	/** Whether a filter starts with the byte, or char {@code & 0xFF}, at each index. */
	protected final boolean[] firstBytes;
	protected final boolean[] firstChars;
	
	public AnyPathFilters(AnyPathFilter[][][] exactFiltersBytes, AnyPathFilter[][][] exactFiltersChars,
			AnyPathFilter[][] encodingFiltersBytes, AnyPathFilter[][] encodingFiltersChars, Map<String, FilterType> names) {
		super();
		this.exactFiltersBytes = exactFiltersBytes;
		this.exactFiltersChars = exactFiltersChars;
		this.encodingFiltersBytes = encodingFiltersBytes;
		this.encodingFiltersChars = encodingFiltersChars;
		this.names = names;
		this.firstBytes = getFirsts(exactFiltersBytes);
		this.firstChars = getFirsts(exactFiltersChars);
	}

	private static boolean[] getFirsts(AnyPathFilter[][][] exactFilters) {
		boolean[] result = new boolean[256];
		for(AnyPathFilter[][] lengthBucket : exactFilters) {
			if(lengthBucket != null) {
				for(int i = 0; i < lengthBucket.length; i++) {
					if(lengthBucket[i] != null) {
						result[i] = true;
					}
				}
			}
		}
		return result;
	}

	public FilterType matchPath(String key) {
		return names.get(key);
	}
	
	public FilterType matchPath(final byte[] chars, int start, int end) {
		int length = end - start;
		if(length <= 0) {
			return null;
		}
		// most keys match no filter: an unescaped first byte must be the first of the filter's name,
		// both for an exact and for an encoded match
		if(chars[start] != '\\' && !firstBytes[chars[start] & 0xFF]) {
			return null;
		}
		if(length < exactFiltersBytes.length) {
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

		// check for encoded key (rare: JSON field names with Unicode escapes);
		// a backslash further in than the longest filter's name can not be part of a match
		int encodedEnd = Math.min(end, start + encodingFiltersBytes.length);
		for(int i = start; i < encodedEnd; i++) {
			if(chars[i] == '\\') {
				int readLength = i - start;
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
		if(length <= 0) {
			return null;
		}
		// most keys match no filter: an unescaped first character must be the first of the filter's name,
		// both for an exact and for an encoded match
		if(chars[start] != '\\' && !firstChars[chars[start] & 0xFF]) {
			return null;
		}
		if(length < exactFiltersChars.length) {
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

		// check for encoded key (rare: JSON field names with Unicode escapes);
		// a backslash further in than the longest filter's name can not be part of a match
		int encodedEnd = Math.min(end, start + encodingFiltersChars.length);
		for(int i = start; i < encodedEnd; i++) {
			if(chars[i] == '\\') {
				int readLength = i - start;
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