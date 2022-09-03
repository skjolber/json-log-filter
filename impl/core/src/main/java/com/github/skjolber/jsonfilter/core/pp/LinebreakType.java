package com.github.skjolber.jsonfilter.core.pp;

public enum LinebreakType {
	
	NONE(""), CarriageReturnLineFeed("\r\n"), LineFeed("\n"), CarriageReturn("\r"),;
	
	String characters;

	private LinebreakType(String characters) {
		this.characters = characters;
	}
	
	public String getCharacters() {
		return characters;
	}
	
	public int length() {
		return characters.length();
	}
	
	public static LinebreakType parse(String string) {
		for (LinebreakType linebreakType : values()) {
			if(linebreakType.characters.equals(string)) {
				return linebreakType;
			}
		}
		throw new IllegalArgumentException();
	}
}