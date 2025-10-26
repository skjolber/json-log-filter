package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.path.EndPathItem;
import com.github.skjolber.jsonfilter.base.path.StarPathItem;

public class StarPathItemTest {

	@Test
	public void testPath() {
		EndPathItem endPathItem = new EndPathItem(0, null, null);
		StarPathItem path = new StarPathItem(0, null);
		path.setNext(endPathItem);
		
		assertSame(path.matchPath(0, null), endPathItem);
		assertSame(path.matchPath(0, (char[])null, 0, 0), endPathItem);
		assertSame(path.matchPath(0, (byte[])null, 0, 0), endPathItem);
	}
	
}
