package com.github.skjolber.jsonfilter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResizableByteArrayOutputStreamTest {

	private ResizableByteArrayOutputStream stream;
	private String string = "Test data";
	private byte[] data = string.getBytes(StandardCharsets.UTF_8);
	
	@BeforeEach
	public void init() {
		stream = new ResizableByteArrayOutputStream(1024);
	}
	
	@Test
	public void testConstructor() {
		try {
			new ResizableByteArrayOutputStream(-1);
			fail();
		} catch(Exception e) {
			// pass
		}
	}
	
	@Test
	public void testBufferIncrements() {
		assertEquals(1024, stream.getBufferSize());
		
		stream.ensureCapacity(1024 + 1);
		
		assertEquals(1024 + ResizableByteArrayOutputStream.MIN_INCREMENT, stream.getBufferSize());
	}
	
	@Test
	public void testWrite1() {
		stream.write(data);
		assertArrayEquals(data, stream.toByteArray());
		assertEquals(string, stream.toString());
		assertEquals(string, stream.toString(StandardCharsets.UTF_8));
	}
	
	@Test
	public void testWrite2() {
		stream.write(data, 0, data.length);
		assertArrayEquals(data, stream.toByteArray());
		assertEquals(string, stream.toString());
	}
	
	@Test
	public void testWrite3() {
		for(byte b : data) {
			stream.write(b);
		}
		assertArrayEquals(data, stream.toByteArray());
		assertEquals(string, stream.toString());
	}
	
	@Test
	public void testWrite4() {
		stream.writeBytes(data);
		assertArrayEquals(data, stream.toByteArray());
		assertEquals(string, stream.toString());
	}
	
	@Test
	public void testReset() {
		stream.write(data);
		assertEquals(data.length, stream.toByteArray().length);
		
		stream.reset();
		
		assertEquals(0, stream.size());
		
		assertEquals(0, stream.toByteArray().length);
	}
}
