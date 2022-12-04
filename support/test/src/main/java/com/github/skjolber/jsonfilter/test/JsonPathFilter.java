package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JsonPathFilter implements JsonFilter {

	private static JsonPath ANY = JsonPath.compile("$..*");
	
	public static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";
	
    private static final ParseContext CONTEXT = JsonPath.using(
            Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .mappingProvider(new JacksonMappingProvider())
                    .options(Option.SUPPRESS_EXCEPTIONS)
                    .options(Option.ALWAYS_RETURN_LIST)
                    .build());
    
    public static Builder newBuilder() {
    	return new Builder();
    }
    
    public static class Builder {
    	protected List<String> anonymizeFilters = new ArrayList<>();
    	protected List<String> pruneFilters = new ArrayList<>();
    	
    	/** Raw JSON */
    	protected String pruneJsonValue;
    	/** Raw JSON */
    	protected String anonymizeJsonValue;
    	
    	/** Raw (escaped) JSON string */
    	protected String truncateStringValue;

    	protected int maxStringLength = -1;

    	public Builder withMaxStringLength(int length) {
    		this.maxStringLength = length;
    		return this;
    	}	

    	public Builder withPrune(String ... filters) {
    		Collections.addAll(pruneFilters, filters);
    		return this;
    	}
    	
    	public Builder withAnonymize(String ... filters) {
    		Collections.addAll(anonymizeFilters, filters);
    		return this;
    	}

    	public JsonPathFilter build() {
    		return new JsonPathFilter(maxStringLength, anonymizeFilters.toArray(new String[anonymizeFilters.size()]), pruneFilters.toArray(new String[pruneFilters.size()]));
    	}
    }

    private final String[] anonPaths;
    private final String[] prunePaths;

    private final List<JsonPath> anon;
    private final List<JsonPath> prune;
    private final int maxStringLength;

    public JsonPathFilter(int maxStringLength, String[] anonymize, String[] prune) {
    	List<String> anonymizeFilters = anonymize != null ? Arrays.asList(anonymize) : Collections.emptyList();
    	List<String> pruneFilters = prune != null ? Arrays.asList(prune) : Collections.emptyList();
    	
		this.anon = anonymizeFilters.stream().map(a -> JsonPath.compile(convert(a))).collect(Collectors.toList());
		this.prune = pruneFilters.stream().map(a -> JsonPath.compile(convert(a))).collect(Collectors.toList());
		
		this.anonPaths = anonymizeFilters.toArray(new String[anonymizeFilters.size()]);
		this.prunePaths = pruneFilters.toArray(new String[pruneFilters.size()]);
		this.maxStringLength = maxStringLength; 
	}

	private String convert(String str) {
		if(!str.startsWith("$")) {
			str = '$' + str;
		}
		String result = str.replace('/', '.');

		return result;
	}

	@Override
	public String process(char[] chars) {
		return process(new String(chars));
	}

	private String filter(DocumentContext original) {
		original.limit(Integer.MAX_VALUE);
		
		for(JsonPath jsonPath : anon) {
			original = original.map(jsonPath, (currentValue, configuration) -> {
				if(currentValue instanceof ContainerNode) {
					ContainerNode<?> containerNode = (ContainerNode<?>)currentValue;
					anonymize(containerNode);
					
					return containerNode;
				}
				return FILTER_ANONYMIZE;
			});
		}
		for(JsonPath jsonPath : prune) {
			original = original.map(jsonPath, (currentValue, configuration) -> {
				return FILTER_PRUNE_MESSAGE;
			});
		}
		
		if(maxStringLength != -1) {
			original = original.map(ANY, (currentValue, configuration) -> {
				String value;
				if(currentValue instanceof String) {
					value = (String)currentValue;
				} else if(currentValue instanceof TextNode) {
					TextNode textNode = (TextNode)currentValue;
					value = textNode.asText();
				} else {
					return currentValue;
				}

				if(value.length() > maxStringLength) {
					// A high surrogate precedes a low surrogate. Together they make up a codepoint.
					int maxStringLength = this.maxStringLength;
					if(Character.isLowSurrogate(value.charAt(maxStringLength))) {
						maxStringLength--;
					}
					int lost = value.length() - maxStringLength;
					return value.subSequence(0, maxStringLength) + FILTER_TRUNCATE_MESSAGE + lost;

				}
				return value;
			});
		}
		
		return original.jsonString();
	}
	
	private void anonymize(ContainerNode<?> containerNode) {
		if(containerNode instanceof ArrayNode) {
			ArrayNode arrayNode = (ArrayNode)containerNode;
			
			for(int i = 0; i < arrayNode.size(); i++) {
				JsonNode jsonNode = arrayNode.get(i);
				
				if(jsonNode instanceof ContainerNode) {
					anonymize((ContainerNode<?>)jsonNode);
				} else if(jsonNode instanceof ValueNode) {
					arrayNode.set(i, TextNode.valueOf(FILTER_ANONYMIZE));
				}
			}
		} else if(containerNode instanceof ObjectNode) {
			ObjectNode objectNode = (ObjectNode)containerNode;
			
			Iterator<String> fieldNames = objectNode.fieldNames();
			while(fieldNames.hasNext()) {
				String next = fieldNames.next();

				JsonNode jsonNode = objectNode.findValue(next);
				if(jsonNode instanceof ContainerNode) {
					anonymize((ContainerNode<?>)jsonNode);
				} else {
					objectNode.put(next, FILTER_ANONYMIZE);
				}
			}
		}
	}

	@Override
	public String process(String chars) {
		try {
			DocumentContext parse = CONTEXT.parse(chars);
			return filter(parse);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public boolean process(String chars, StringBuilder output) {
		try {
			DocumentContext parse = CONTEXT.parse(chars);
			String filter = filter(parse);
			output.append(filter);
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(chars.length < offset + length) {
			return false;
		}
		return process(new String(chars, offset, length), output);
	}

	@Override
	public byte[] process(byte[] chars) {
		return process(chars, 0, chars.length);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		String process = process(new String(chars, StandardCharsets.UTF_8));
		if(process != null) {
			return process.getBytes(StandardCharsets.UTF_8);
		}
		return null;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		if(chars.length < offset + length) {
			return false;
		}
		byte[] process = process(chars, offset, length);
		if(process != null) {
			output.write(process, offset, length);
			return true;
		}
		return false;
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}
	
	
	public String[] getAnonymizeFilters() {
		return anonPaths;
	}

	public String[] getPruneFilters() {
		return prunePaths;
	}

	@Override
	public String process(char[] chars, JsonFilterMetrics metrics) {
		return process(chars);
	}

	@Override
	public String process(String chars, JsonFilterMetrics metrics) {
		return process(chars);
	}

	@Override
	public boolean process(String chars, StringBuilder output, JsonFilterMetrics metrics) {
		return process(chars, output);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics metrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics metrics) {
		return process(chars);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics metrics) {
		return process(chars, offset, length);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output,
			JsonFilterMetrics metrics) {
		return process(chars, offset, length, output);
	}
	
	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}
	
	@Override
	public boolean isValidating() {
		return true;
	}
	
}
