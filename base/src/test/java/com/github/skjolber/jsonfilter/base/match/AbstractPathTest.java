package com.github.skjolber.jsonfilter.base.match;

import java.nio.charset.StandardCharsets;

public abstract class AbstractPathTest {

	protected String a = "a";
	protected String b = "b";
	protected String c = "c";
	protected String d = "d";
	protected String e = "e";
	protected String f = "f";
	protected String g = "g";
	protected String h = "h";

	protected char[] aChars  = "a".toCharArray();
	protected char[] bChars = "b".toCharArray();
	protected char[] cChars = "c".toCharArray();
	protected char[] dChars = "d".toCharArray();
	protected char[] eChars = "e".toCharArray();
	protected char[] fChars = "f".toCharArray();
	protected char[] gChars = "g".toCharArray();
	protected char[] hChars = "h".toCharArray();
	
	protected byte[] aBytes = "a".getBytes(StandardCharsets.UTF_8);
	protected byte[] bBytes = "b".getBytes(StandardCharsets.UTF_8);
	protected byte[] cBytes = "c".getBytes(StandardCharsets.UTF_8);
	protected byte[] dBytes = "d".getBytes(StandardCharsets.UTF_8);
	protected byte[] eBytes = "e".getBytes(StandardCharsets.UTF_8);
	protected byte[] fBytes = "f".getBytes(StandardCharsets.UTF_8);
	protected byte[] gBytes = "g".getBytes(StandardCharsets.UTF_8);
	protected byte[] hBytes = "h".getBytes(StandardCharsets.UTF_8);

}
