package com.github.skjolber.jsonfilter.test;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * 
 * Iterator which produces all possible variants of a pretty printer.
 *
 */

public class PrettyPrinterIterator {

	public static List<DefaultPrettyPrinter> getPermutations() {
		
		List<DefaultPrettyPrinter> list = new ArrayList<>();
		
		PrettyPrinterIterator iterator = new PrettyPrinterIterator();
		do {
			list.add(iterator.getConfiguration());
		} while(iterator.nextConfiguration() != -1);
		
		return list;
	}

	private static final int[] RESET = new int[PrettyPrinterFactory.MAX_DIMENSIONS.length];

	protected PrettyPrinterFactory factory = new PrettyPrinterFactory();
	protected int[] state = new int[PrettyPrinterFactory.MAX_DIMENSIONS.length];

	public int nextConfiguration() {
		// next rotation
		return nextConfiguration(state.length - 1);
	}
	
	public int nextConfiguration(int maxIndex) {
		// next rotation
		for(int i = maxIndex; i >= 0; i--) {
			if(state[i] < PrettyPrinterFactory.MAX_DIMENSIONS[i]) {
				state[i]++;

				System.arraycopy(RESET, 0, state, i + 1, state.length - (i + 1));

				return i;
			}
		}

		return -1;
	}
	
	public DefaultPrettyPrinter getConfiguration() {
		return factory.newInstance(state);
	}

}
