package com.github.skjolber.jsonfilter.test.pp;

import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.DefaultPrettyPrinter.Indenter;
import tools.jackson.core.util.Separators;

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
		
		prettyPrinter = spacesInObjectEntries(prettyPrinter, dimensions);
		prettyPrinter = withArrayIndenter(prettyPrinter, dimensions);
		prettyPrinter = withObjectIndenter(prettyPrinter, dimensions);
		
		return prettyPrinter;
	}
	
	public DefaultPrettyPrinter spacesInObjectEntries(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		if(dimensions[spacesInObjectEntries] == 1) {
			return prettyPrinter.withSeparators(Separators.createDefaultInstance()
					.withObjectNameValueSpacing(Separators.Spacing.NONE));
		} else {
			return prettyPrinter.withSeparators(Separators.createDefaultInstance()
					.withObjectNameValueSpacing(Separators.Spacing.BOTH));
		}
	}
	
	public DefaultPrettyPrinter withArrayIndenter(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		return prettyPrinter.withArrayIndenter(intenders[dimensions[withArrayIndenter]]);
	}

	public DefaultPrettyPrinter withObjectIndenter(DefaultPrettyPrinter prettyPrinter, int[] dimensions) {
		return prettyPrinter.withObjectIndenter(intenders[dimensions[withObjectIndenter]]);
	}
	
}
