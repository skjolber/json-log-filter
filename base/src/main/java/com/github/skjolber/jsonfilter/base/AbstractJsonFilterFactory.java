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

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilterFactory;
import com.github.skjolber.jsonfilter.JsonFilterFactoryProperty;

public abstract class AbstractJsonFilterFactory implements JsonFilterFactory {
	
	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	protected int maxSize = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<String>();
	protected List<String> pruneFilters = new ArrayList<String>();

	/** Raw JSON */
	protected String pruneJsonValue;
	/** Raw JSON */
	protected String anonymizeJsonValue;
	
	/** Raw (escaped) JSON string */
	protected String truncateStringValue;
	protected boolean removeWhitespace;
	
	protected boolean isSinglePruneFilter() {
		return 
				anonymizeFilters.isEmpty()
				&& !pruneFilters.isEmpty() 
				&& pruneFilters.size() == 1;
	}

	protected boolean isSingleAnonymizeFilter() {
		return 
				pruneFilters.isEmpty()
				&& !anonymizeFilters.isEmpty() 
				&& anonymizeFilters.size() == 1;
	}

	protected boolean isActiveMaxStringLength() {
		return maxStringLength != -1;
	}
	
	protected boolean isActiveMaxSize() {
		return maxSize != -1;
	}

	protected boolean isActivePathFilters() {
		return 
				!anonymizeFilters.isEmpty() || !pruneFilters.isEmpty();
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
	
	public void setRemoveWhitespace(boolean removeWhitespace) {
		this.removeWhitespace = removeWhitespace;
	}
	
	public boolean isRemoveWhitespace() {
		return removeWhitespace;
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
	
	public void setPrune(String ... filters) {
		if(filters != null) {
			AbstractPathJsonFilter.validatePruneExpressions(filters);
		}
		for(String filter : filters) {
			this.pruneFilters.add(filter);
		}
	}
	
	/**
	 * Set prune expressions
	 * 
	 * @param filters list of prune expressions
	 */
	
	public void setPrune(List<String> filters) {
		if(filters != null) {
			AbstractPathJsonFilter.validatePruneExpressions(filters);
		}
		this.pruneFilters.addAll(filters);
	}
	
	public void addPrune(String filter) {
		AbstractPathJsonFilter.validatePruneExpression(filter);
		
		this.pruneFilters.add(filter);
	}

	public void addAnonymize(String filter) {
		AbstractPathJsonFilter.validateAnonymizeExpression(filter);
		
		this.anonymizeFilters.add(filter);
	}

	public List<String> getPrune() {
		return pruneFilters;
	}
	
	/**
	 * Set anonymize filters
	 * 
	 * @param filters array of anonymize filters
	 */
	
	public void setAnonymize(String ... filters) {
		if(filters != null) {
			AbstractPathJsonFilter.validateAnonymizeExpressions(filters);
		}
		for(String filter : filters) {
			this.anonymizeFilters.add(filter);
		}
	}
	
	/**
	 * 
	 * Set anonymize filters
	 * 
	 * @param filters list of anonymize filters
	 */
	
	public void setAnonymize(List<String> filters) {
		if(filters != null && !filters.isEmpty()) {
			AbstractPathJsonFilter.validateAnonymizeExpressions(filters);
		}
		for(String filter : filters) {
			this.anonymizeFilters.add(filter);
		}
	}
	
	public List<String> getAnonymize() {
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

	@SuppressWarnings("unchecked")
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
				setPrune((String[]) value);
			} else if(value instanceof String) {
				setPrune((String) value);
			} else if(value instanceof List) {
				setPrune((List<String>) value);
			} else {
				throw new IllegalArgumentException("Cannot set prunes, unexpected value type");
			}
			break;
		}
		case ANONYMIZE : {
			if(value instanceof String[]) {
				setAnonymize((String[]) value);
			} else if(value instanceof String) {
				setAnonymize((String) value);
			} else if(value instanceof List) {
				setAnonymize((List<String>) value);
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
		case REMOVE_WHITESPACE : {
			if(value instanceof String) {
				setRemoveWhitespace(Boolean.parseBoolean((String)value));
			} else if(value instanceof Boolean) {
				setRemoveWhitespace((Boolean)value);
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
