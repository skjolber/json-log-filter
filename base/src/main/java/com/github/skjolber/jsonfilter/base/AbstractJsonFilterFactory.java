/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

public abstract class AbstractJsonFilterFactory implements JsonFilterFactory {
	
	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	
	protected String[] anonymizeFilters;
	protected String[] pruneFilters;

	protected String pruneMessage;
	protected String anonymizeMessage;
	protected String truncateMessage;
	
	protected boolean isSinglePruneFilter() {
		return (anonymizeFilters == null || anonymizeFilters.length == 0) && pruneFilters != null && pruneFilters.length == 1;
	}

	protected boolean isSingleAnonymizeFilter() {
		return (pruneFilters == null || pruneFilters.length == 0) && anonymizeFilters != null && anonymizeFilters.length == 1;
	}

	protected boolean isActiveMaxStringLength() {
		return maxStringLength != -1;
	}

	protected boolean isActivePathFilters() {
		return (anonymizeFilters != null && anonymizeFilters.length > 0) || (pruneFilters != null && pruneFilters.length > 0);
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
			setPruneFilters();
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
			setAnonymizeFilters();
		}
	}
	
	public String[] getAnonymizeFilters() {
		return anonymizeFilters;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setProperty(String name, Object value) {
		JsonFilterFactoryProperty p = JsonFilterFactoryProperty.parse(name);
		if(p == null) {
			throw new IllegalArgumentException("Unknown property " + name);
		}
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
	
	protected static boolean hasAnyPrefix(String[] filters) {
		if(filters != null) {
			for(String string : filters) {
				if(string.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setPruneMessage(String pruneMessage) {
		this.pruneMessage = pruneMessage;
	}

	public void setAnonymizeMessage(String anonymizeMessage) {
		this.anonymizeMessage = anonymizeMessage;
	}

	public void setTruncateMessage(String truncateMessage) {
		this.truncateMessage = truncateMessage;
	}
	
	
}
