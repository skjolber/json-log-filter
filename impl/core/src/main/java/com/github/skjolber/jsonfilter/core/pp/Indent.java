package com.github.skjolber.jsonfilter.core.pp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Append whitespace to character outputs in levels.<br><br>
 * 
 * Keeps a set of prepared indent strings for improved performance.
 *
 */

public class Indent {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private char character = ' ';
		private int count = 2;
		private int preparedLevels = 32;
		private LinebreakType linebreakType = LinebreakType.LineFeed;
		
		public Builder withCharacter(char character) {
			this.character = character;
			
			return this;
		}

		public Builder withSpace(int count) {
			return withCharacter(' ').withCount(count);
		}

		public Builder withTab() {
			return withCharacter('\t').withCount(1);
		}
		
		public Builder withCount(int count) {
			this.count = count;
			
			return this;
		}
		
		public Builder withPreparedLevels(int preparedLevels) {
			this.preparedLevels = preparedLevels;
			
			return this;
		}
		
		public Builder withoutLinebreak() {
			return withLinebreak(LinebreakType.NONE);
		}

		public Builder withWindowsLinebreak() {
			return withLinebreak(LinebreakType.CarriageReturnLineFeed);
		}

		public Builder withUnixLinebreak() {
			return withLinebreak(LinebreakType.LineFeed);
		}

		public Builder withLinebreak(LinebreakType linebreak) {
			this.linebreakType = linebreak;
			return this;
		}
		
		public Indent build() {
			return new Indent(character, count, preparedLevels, linebreakType);
		}
			
	}
	
	protected final char[][] charIndentations;
	protected final byte[][] byteIndentations;
	protected final char character;
	protected final int count;
	protected final int preparedLevels;
	
	protected final LinebreakType linebreakType;
	
	/**
	 * Construct a new instance
	 * 
	 * @param character whitespace character to use, i.e. usually space or tab
	 * @param count number of characters to each indented level
	 * @param preparedLevels number of prepared (cached) levels
	 * @param linebreak type of linebreak
	 */

	public Indent(char character, int count, int preparedLevels, LinebreakType linebreak) {
		if(count < 0) {
			throw new IllegalArgumentException("Expected non-negative indent count");
		}
		if(preparedLevels < 0) {
			throw new IllegalArgumentException("Expected non-negative prepared level");
		}
		if(linebreak == null) {
			throw new IllegalArgumentException("Expected non-null linebreak parameter");
		}
		this.character = character;
		this.count = count;
		this.preparedLevels = preparedLevels;
		this.linebreakType = linebreak;
		
		this.charIndentations = prepareChars(character, count, linebreak, preparedLevels);
		this.byteIndentations = prepareBytes(character, count, linebreak, preparedLevels);
	}

	protected static byte[][] prepareBytes(char character, int count, LinebreakType linebreak, int levels) {
		byte[][] indentations = new byte[levels + 1][]; // count zero as a level

		indentations[0] = linebreak.characters.getBytes(StandardCharsets.UTF_8);
		
		StringBuilder increment = new StringBuilder(count);
		for(int k = 0; k < count; k++) {
			increment.append(character);
		}
		byte[] incrementBytes = increment.toString().getBytes(StandardCharsets.UTF_8);
		
		ByteArrayOutputStream builder = new ByteArrayOutputStream(levels * count + linebreak.length());
		builder.write(indentations[0], 0, indentations[0].length);
		
		for(int i = 1; i < indentations.length; i++) {
			builder.write(incrementBytes, 0, incrementBytes.length);
			
			indentations[i] = builder.toByteArray();
		}
		return indentations;
	}

	protected static char[][] prepareChars(char character, int count, LinebreakType linebreak, int levels) {
		char[][] indentations = new char[levels + 1][]; // count zero as a level

		indentations[0] = linebreak.characters.toCharArray();
		StringBuilder increment = new StringBuilder(count);
		for(int k = 0; k < count; k++) {
			increment.append(character);
		}
		
		StringBuilder builder = new StringBuilder(levels * count + linebreak.length());
		builder.append(indentations[0]);
		for(int i = 1; i < indentations.length; i++) {
			builder.append(increment);
			
			builder.getChars(0, builder.length(), indentations[i] = new char[builder.length()], 0);
		}
		return indentations;
	}
	
	public void append(ByteArrayOutputStream stream, int level) {
		if(level < charIndentations.length) {
			stream.write(byteIndentations[level], 0, byteIndentations[level].length);
		} else {
			// do not ensure capacity here, leave that up to the caller
			// append a longer intent than we have prepared
			byte[] bs = byteIndentations[level % preparedLevels];
			
			stream.write(bs, 0, bs.length);

			for(int i = 0; i < level / preparedLevels; i++) {
				stream.write(byteIndentations[preparedLevels], linebreakType.length(), byteIndentations[preparedLevels].length - linebreakType.length());
			}
		}
	}

	public void append(StringBuffer buffer, int level) {
		if(level < charIndentations.length) {
			buffer.append(charIndentations[level]);
		} else {
			// do not ensure capacity here, leave that up to the caller
			// append a longer intent than we have prepared
			buffer.append(charIndentations[level % preparedLevels]);

			for(int i = 0; i < level / preparedLevels; i++) {
				buffer.append(charIndentations[preparedLevels], linebreakType.length(), charIndentations[preparedLevels].length - linebreakType.length());
			}
		}
	}

	public void append(StringBuilder buffer, int level) {
		if(level < charIndentations.length) {
			buffer.append(charIndentations[level]);
		} else {
			// do not ensure capacity here, leave that up to the caller
			// append a longer intent than we have prepared
			buffer.append(charIndentations[level % preparedLevels]);

			for(int i = 0; i < level / preparedLevels; i++) {
				buffer.append(charIndentations[preparedLevels], linebreakType.length(), charIndentations[preparedLevels].length - linebreakType.length());
			}
		}
	}
	
	public void append(Writer buffer, int level) throws IOException {
		if(level < charIndentations.length) {
			buffer.write(charIndentations[level]);
		} else {
			// append a longer intent than we have prepared
			buffer.write(charIndentations[level % preparedLevels]);

			for(int i = 0; i < level / preparedLevels; i++) {
				buffer.write(charIndentations[preparedLevels], linebreakType.length(), charIndentations[preparedLevels].length - linebreakType.length());
			}
		}
	}		
	
	public char getCharacter() {
		return character;
	}
	
	public int getCount() {
		return count;
	}
	
	public LinebreakType getLinebreakType() {
		return linebreakType;
	}
	
	public int getPreparedLevels() {
		return preparedLevels;
	}
	
	public String asIndent() {
		StringBuilder increment = new StringBuilder(count);
		for(int k = 0; k < count; k++) {
			increment.append(character);
		}
		return increment.toString();
	}
	
	public String asLinebreak() {
		if(linebreakType != null) {
			return linebreakType.characters;
		}
		return null;
	}

}