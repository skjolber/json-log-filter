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
		
		AnyPathFilter[][] exactFiltersBytes = fillBytes(any, maxKeyLengthBytes);
		AnyPathFilter[][] exactFiltersChars = fillChars(any, maxKeyLengthChars);
		
		AnyPathFilter[][] encodedFiltersChars = fillEncoded(exactFiltersChars);
		AnyPathFilter[][] encodedFiltersBytes = fillEncoded(exactFiltersBytes);
		
		return new AnyPathFilters(exactFiltersBytes, exactFiltersChars, encodedFiltersBytes, encodedFiltersChars, map);
	}
	

	private static AnyPathFilter[][] fillEncoded(AnyPathFilter[][] exactFiltersChars) {
		AnyPathFilter[][] result = new AnyPathFilter[exactFiltersChars.length + 1][];

		List<AnyPathFilter> filters = new ArrayList<>();
		
		for(int i = exactFiltersChars.length - 1; i >= 0; i--) {
			if(exactFiltersChars[i] != null) {
				for(AnyPathFilter f : exactFiltersChars[i]) {
					filters.add(f);
				}
			}
			result[i + 1] = filters.toArray(new AnyPathFilter[filters.size()]);
		}
		return result;
	}

	private static AnyPathFilter[][] fillBytes(List<AnyPathFilter> any, int length) {
		List<AnyPathFilter>[] output = new List[length+1];
		for(int i = 0; i < output.length; i++) {
			output[i] = new ArrayList<>();
		}
		
		for(AnyPathFilter filter : any) {
			output[filter.pathBytes.length].add(filter);
		}
		
		AnyPathFilter[][] result = new AnyPathFilter[length + 1][];
		for(int i = 0; i < result.length; i++) {
			if(!output[i].isEmpty()) {
				result[i] = output[i].toArray(new AnyPathFilter[output[i].size()]);
			}
		}
	
		 return result;
	}
	
	private static AnyPathFilter[][] fillChars(List<AnyPathFilter> any, int length) {
		List<AnyPathFilter>[] output = new List[length+1];
		for(int i = 0; i < output.length; i++) {
			output[i] = new ArrayList<>();
		}
		
		for(AnyPathFilter filter : any) {
			output[filter.pathChars.length].add(filter);
		}
		
		AnyPathFilter[][] result = new AnyPathFilter[length + 1][];
		for(int i = 0; i < result.length; i++) {
			if(!output[i].isEmpty()) {
				result[i] = output[i].toArray(new AnyPathFilter[output[i].size()]);
			}
		}
	
		 return result;
	}
	
	// for exact match
	protected final AnyPathFilter[][] exactFiltersBytes;
	protected final AnyPathFilter[][] exactFiltersChars;

	protected final AnyPathFilter[][] encodingFiltersBytes;
	protected final AnyPathFilter[][] encodingFiltersChars;

	protected final Map<String, FilterType> names;
	
	public AnyPathFilters(AnyPathFilter[][] exactFiltersBytes, AnyPathFilter[][] exactFiltersChars,
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
		if(length < exactFiltersBytes.length && exactFiltersBytes[length] != null) {
			FilterType type = unencodedMatchAnyElements(exactFiltersBytes[length], chars, start, end);
			if(type != null) {
				return type;
			}
		}
		
		// check for encoded key
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
		if(length < exactFiltersChars.length && exactFiltersChars[length] != null) {
			FilterType type = unencodedMatchAnyElements(exactFiltersChars[length], chars, start, end);
			if(type != null) {
				return type;
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
	

	protected static FilterType unencodedMatchAnyElements(AnyPathFilter[] anyPathFilters, final byte[] chars, int start, int end) {
		main:
		for(int i = 0; i < anyPathFilters.length; i++) {
			byte[] pathBytes = anyPathFilters[i].pathBytes;
			for(int k = 0; k < pathBytes.length; k++) {
				if(pathBytes[k] != chars[start + k]) {
					continue main;
				}
			}
			return anyPathFilters[i].filterType;
		}
		return null;
	}

	protected static FilterType unencodedMatchAnyElements(AnyPathFilter[] anyPathFilters, final char[] chars, int start, int end) {
		
		main:
		for(int i = 0; i < anyPathFilters.length; i++) {
			char[] pathBytes = anyPathFilters[i].pathChars;
			for(int k = 0; k < pathBytes.length; k++) {
				if(pathBytes[k] != chars[start + k]) {
					continue main;
				}
			}
			return anyPathFilters[i].filterType;
		}
		return null;
	}

}