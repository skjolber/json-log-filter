package com.github.skjolber.jsonfilter.test;

import java.util.Arrays;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;

public class PrettyPrinterFactory {
	
	protected static final int spacesInObjectEntries = 0;
	
	// indent input
	protected static final int withArrayIndenter = 1;
	protected static final int withObjectIndenter = 2;
	
	protected static final Indenter defaultIntenterSpaceLinefeed = new DefaultIndenter("  ", "\n");
	protected static final Indenter defaultIntenterTabLinefeed = new DefaultIndenter("\t", "\n");

	protected static final Indenter defaultIntenterSpaceCarriageReturn = new DefaultIndenter("  ", "\r");
	protected static final Indenter defaultIntenterTabCarriageReturn = new DefaultIndenter("\t", "\r");

	protected static final Indenter defaultIntenterSpaceCarriageReturnLinefeed = new DefaultIndenter("  ", "\r\n");
	protected static final Indenter defaultIntenterTabCarriageReturnLinefeed = new DefaultIndenter("\t", "\r\n");
	
	protected static final Indenter[] intenders = new Indenter[]{defaultIntenterSpaceLinefeed, defaultIntenterTabLinefeed, defaultIntenterSpaceCarriageReturn, defaultIntenterTabCarriageReturn, defaultIntenterSpaceCarriageReturnLinefeed, defaultIntenterTabCarriageReturnLinefeed};

	protected static final int[] MAX_DIMENSIONS = new int[] {
			1, 5, 5
	};
	
	public DefaultPrettyPrinter newInstance(int[] dimensions) {
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		
		spacesInObjectEntries(prettyPrinter, dimensions);
		withArrayIndenter(prettyPrinter, dimensions);
		withObjectIndenter(prettyPrinter, dimensions);
		
		return prettyPrinter;
	}
	
	public void spacesInObjectEntries(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		if(dimensions[spacesInObjectEntries] == 1) {
			prettyPrinter = prettyPrinter.withoutSpacesInObjectEntries();
		} else {
			prettyPrinter = prettyPrinter.withSpacesInObjectEntries();
		}
	}
	
	public void withArrayIndenter(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		prettyPrinter = prettyPrinter.withArrayIndenter(intenders[dimensions[withArrayIndenter]]);
	}

	public void withObjectIndenter(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		prettyPrinter = prettyPrinter.withObjectIndenter(intenders[dimensions[withObjectIndenter]]);
	}
	
}
