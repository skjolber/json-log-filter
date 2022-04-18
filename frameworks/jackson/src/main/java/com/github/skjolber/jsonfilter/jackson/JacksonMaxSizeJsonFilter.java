package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonMaxSizeJsonFilter extends DefaultJacksonJsonFilter implements JacksonJsonFilter {

	public JacksonMaxSizeJsonFilter(int maxSize) {
		this(maxSize, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, JsonFactory jsonFactory) {
		this(maxSize, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxSize, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(chars.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> output.length());
		} catch(final Exception e) {
			return false;
		}
	}
	
	protected boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
		if(bytes.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output);
		}

		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> output.size());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(bytes.length < offset + length) {
			return false;
		}

		if(maxSize >= length) {
			output.append(new String(bytes, offset, length));
			return true;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> output.length());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		long maxOffset = this.maxSize;

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			} 
			
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} 

			long nextOffset = offsetSupplier.getAsLong();
			if(nextOffset >= maxOffset) {
				// measure accurately the output size
				maxOffset = getMaxOffset(generator, offsetSupplier, outputSizeSupplier) - (nextOffset - offset);
				if(nextOffset >= maxOffset) {
					break;
				}
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		generator.flush(); // don't close
		
		return true;
	}

	protected long getMaxOffset(JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		generator.flush(); // don't close

		long outputSize = outputSizeSupplier.getAsLong();
		
		long left = this.maxSize - outputSize;
		if(left > 0) {
			return offsetSupplier.getAsLong() + left;
		}
		return Integer.MIN_VALUE;
	}
	
	protected static long getTokenSize(JsonParser parser, JsonToken nextToken) throws IOException {
		switch(nextToken) {
		case VALUE_FALSE : return 5;
		case VALUE_TRUE : return 4;
		case VALUE_NUMBER_INT: {
			return lengthToDigitgs(parser.getLongValue());
		}
		case VALUE_NUMBER_FLOAT: { // TODO optimize
			return parser.getValueAsString().length();
		}
		default : {
			return 1;
		}
		}
	}

	protected static int lengthToDigitgs(long c) {
		int aBack = (int)(c >> 32);
		int bBack = (int)c;
		
		return lengthToDigitgs(aBack) + lengthToDigitgs(bBack);
	}
	
	protected static int lengthToDigits(int number) {
		if (number < 100000) {
		    if (number < 100) {
		        if (number < 10) {
		            return 1;
		        } else {
		            return 2;
		        }
		    } else {
		        if (number < 1000) {
		            return 3;
		        } else {
		            if (number < 10000) {
		                return 4;
		            } else {
		                return 5;
		            }
		        }
		    }
		} else {
		    if (number < 10000000) {
		        if (number < 1000000) {
		            return 6;
		        } else {
		            return 7;
		        }
		    } else {
		        if (number < 100000000) {
		            return 8;
		        } else {
		            if (number < 1000000000) {
		                return 9;
		            } else {
		                return 10;
		            }
		        }
		    }
		}
	}
	
}