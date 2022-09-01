package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

public class FlexibleOutputStream extends ByteArrayOutputStream {

	public void setCount(int count) {
		this.count = count;
	}
	
	public byte getByte(int index) {
		return buf[index];
	}
	
}
