/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.github.skjolber.jsonfilter.base;

import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilterFactory;
import com.github.skjolber.jsonfilter.JsonFilterFactoryProperty;

public abstract class AbstractJsonFilterFactory implements JsonFilterFactory {
	
	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	protected int maxSize = -1;
	
	protected String[] anonymizeFilters;
	protected String[] pruneFilters;

	/** Raw JSON */
	protected String pruneJsonValue;
	/** Raw JSON */
	protected String anonymizeJsonValue;
	
	/** Raw (escaped) JSON string */
	protected String truncateStringValue;
	
	protected boolean isSinglePruneFilter() {
		return 
				(anonymizeFilters == null || anonymizeFilters.length == 0)
				&& pruneFilters != null 
				&& pruneFilters.length == 1;
	}

	protected boolean isSingleAnonymizeFilter() {
		return 
				(pruneFilters == null || pruneFilters.length == 0)
				&& anonymizeFilters != null 
				&& anonymizeFilters.length == 1;
	}

	protected boolean isActiveMaxStringLength() {
		return maxStringLength != -1;
	}
	
	protected boolean isActiveMaxSize() {
		return maxSize != -1;
	}

	protected boolean isActivePathFilters() {
		return 
				(anonymizeFilters != null && anonymizeFilters.length > 0) 
				|| (pruneFilters != null && pruneFilters.length > 0);
	}

	/**
	 * 
	 * Set maximum text length.
	 * 
	 * Note that truncation of nodes below 25 chars will not reduce node size.
	 * 
	 * @param length max text value length
	 */

	
	public void setMaxStringLength(int length) {
		this.maxStringLength = length;
	}
	
	public void setMaxSize(int length) {
		this.maxSize = length;
	}
	
	
	public int getMaxStringLength() {
		return maxStringLength;
	}
	
	public void setMaxPathMatches(int maxPathMatches) {
		this.maxPathMatches = maxPathMatches;
	}
	
	public int getMaxPathMatches() {
		return maxPathMatches;
	}

	/**
	 * Set prune expressions
	 * 
	 * @param filters array of prune expressions
	 */
	
	public void setPruneFilters(String ... filters) {
		if(filters != null) {
			AbstractPathJsonFilter.validateAnonymizeExpressions(filters);
		}
		
		this.pruneFilters = filters;
	}
	
	/**
	 * Set prune expressions
	 * 
	 * @param filters list of prune expressions
	 */
	
	public void setPruneFilters(List<String> filters) {
		if(filters != null && !filters.isEmpty()) {
			setPruneFilters(filters.toArray(new String[filters.size()]));
		} else {
			this.pruneFilters = new String[]{};
		}
	}
	
	public String[] getPruneFilters() {
		return pruneFilters;
	}
	
	/**
	 * Set anonymize filters
	 * 
	 * @param filters array of anonymize filters
	 */
	
	public void setAnonymizeFilters(String ... filters) {
		if(filters != null) {
			AbstractPathJsonFilter.validateAnonymizeExpressions(filters);
		}
		
		this.anonymizeFilters = filters;
	}
	
	/**
	 * 
	 * Set anonymize filters
	 * 
	 * @param filters list of anonymize filters
	 */
	
	public void setAnonymizeFilters(List<String> filters) {
		if(filters != null && !filters.isEmpty()) {
			setAnonymizeFilters(filters.toArray(new String[filters.size()]));
		} else {
			this.anonymizeFilters = new String[]{};
		}
	}
	
	public String[] getAnonymizeFilters() {
		return anonymizeFilters;
	}

	@Override
	public void setProperty(String name, Object value) {
		JsonFilterFactoryProperty p = JsonFilterFactoryProperty.parse(name);
		if(p == null) {
			throw new IllegalArgumentException("Unknown property " + name);
		}
		setProperty(p, value);
	}

	public void setProperty(JsonFilterFactoryProperty p, Object value) {
		switch (p) {
		case MAX_STRING_LENGTH: {
			if(value instanceof Integer) {
				setMaxStringLength((Integer) value);
			} else if(value instanceof String) {
				setMaxStringLength(Integer.parseInt((String) value));
			} else {
				throw new IllegalArgumentException("Cannot set max string length, unexpected value type");
			}
			break;
		}
		case MAX_SIZE: {
			if(value instanceof Integer) {
				setMaxSize((Integer) value);
			} else if(value instanceof String) {
				setMaxSize(Integer.parseInt((String) value));
			} else {
				throw new IllegalArgumentException("Cannot set max size, unexpected value type");
			}
			break;
		}		
		case PRUNE : {
			if(value instanceof String[]) {
				setPruneFilters((String[]) value);
			} else if(value instanceof String) {
				setPruneFilters((String) value);
			} else if(value instanceof List) {
				setPruneFilters((List<String>) value);
			} else {
				throw new IllegalArgumentException("Cannot set prunes, unexpected value type");
			}
			break;
		}
		case ANONYMIZE : {
			if(value instanceof String[]) {
				setAnonymizeFilters((String[]) value);
			} else if(value instanceof String) {
				setAnonymizeFilters((String) value);
			} else if(value instanceof List) {
				setAnonymizeFilters((List<String>) value);
			} else {
				throw new IllegalArgumentException("Cannot set anonymize, unexpected value type");
			}
			break;
		}
		case MAX_PATH_MATCHES : {
			if(value instanceof Integer) {
				setMaxPathMatches((Integer) value);
			} else if(value instanceof String) {
				setMaxPathMatches(Integer.parseInt((String) value));
			} else {
				throw new IllegalArgumentException("Cannot set max path matches, unexpected value type");
			}
			break;
		}
		case PRUNE_MESSAGE : {
			if(value instanceof String) {
				setPruneStringValue((String) value);
			} else {
				throw new IllegalArgumentException("Cannot set prune message, unexpected value type");
			}
			break;
		}
		case ANON_MESSAGE : {
			if(value instanceof String) {
				setAnonymizeStringValue((String) value);
			} else {
				throw new IllegalArgumentException("Cannot set anonymize message, unexpected value type");
			}
			break;
		}
		case TRUNCATE_MESSAGE : {
			if(value instanceof String) {
				setTruncateStringValue((String) value);
			} else {
				throw new IllegalArgumentException("Cannot set truncate message, unexpected value type");
			}
			break;
		}
		default : {
			throw new RuntimeException(); // should never happen
		}
		}
	}

	@Override
	public boolean isPropertySupported(String name) {
		for (JsonFilterFactoryProperty p : JsonFilterFactoryProperty.values()) {
			if(name.equals(p.getPropertyName())) {
				return true;
			}
		}
		return false;
	}

	public void setPruneStringValue(String pruneMessage) {
		StringBuilder stringBuilder = new StringBuilder(pruneMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(pruneMessage, stringBuilder);
		stringBuilder.append('"');
		setPruneJsonValue(stringBuilder.toString());
	}

	public void setAnonymizeStringValue(String anonymizeMessage) {
		StringBuilder stringBuilder = new StringBuilder(anonymizeMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(anonymizeMessage, stringBuilder);
		stringBuilder.append('"');
		setAnonymizeJsonValue(stringBuilder.toString());
	}

	public void setTruncateStringValue(String truncateMessage) {
		StringBuilder stringBuilder = new StringBuilder(truncateMessage.length() * 2);
		AbstractJsonFilter.quoteAsString(truncateMessage, stringBuilder);
		setTruncateJsonStringValue(stringBuilder.toString());
	}
	
	public void setTruncateJsonStringValue(String escaped) {
		this.truncateStringValue = escaped;
	}
	
	public void setPruneJsonValue(String raw) {
		this.pruneJsonValue = raw;
	}

	public void setAnonymizeJsonValue(String raw) {
		this.anonymizeJsonValue = raw;
	}
	
	public String getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	public String getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	public String getTruncateJsonStringValue() {
		return truncateStringValue;
	}
	
	
}
