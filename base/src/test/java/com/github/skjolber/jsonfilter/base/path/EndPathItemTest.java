package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.path.EndPathItem;

public class EndPathItemTest {

	@Test
	public void testPath() {
		EndPathItem endPath = new EndPathItem(0, null, null);
		
	    assertThrows(UnsupportedOperationException.class, () -> {
	       endPath.matchPath(0, null);
	    });
	    assertThrows(UnsupportedOperationException.class, () -> {
		       endPath.matchPath(0, (char[])null, 0, 0);
	    });
	    assertThrows(UnsupportedOperationException.class, () -> {
		       endPath.matchPath(0, (byte[])null, 0, 0);
	    });
	}

	@Test
	public void testHasTypeReturnsTrueOnEndPathItem() {
		EndPathItem endPath = new EndPathItem(0, null, null);
		assertTrue(endPath.hasType());
	}
}
