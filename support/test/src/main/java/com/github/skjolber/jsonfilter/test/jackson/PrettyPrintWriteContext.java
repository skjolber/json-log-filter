package com.github.skjolber.jsonfilter.test.jackson;

import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultPrettyPrinter;

/**
 * Reusable {@link ObjectWriteContext} implementations for pretty-printing JSON generators.
 */
public final class PrettyPrintWriteContext {

	/**
	 * A shared write context that produces output with the default pretty printer.
	 */
	public static final ObjectWriteContext DEFAULT = new ObjectWriteContext.Base() {
		@Override
		public PrettyPrinter getPrettyPrinter() {
			return new DefaultPrettyPrinter();
		}
		@Override
		public boolean hasPrettyPrinter() {
			return true;
		}
	};

	/**
	 * Creates an {@link ObjectWriteContext} that uses the given {@link PrettyPrinter}.
	 *
	 * @param prettyPrinter the pretty printer to use
	 * @return a new write context wrapping the given pretty printer
	 */
	public static ObjectWriteContext of(PrettyPrinter prettyPrinter) {
		return new ObjectWriteContext.Base() {
			@Override
			public PrettyPrinter getPrettyPrinter() {
				return prettyPrinter;
			}
			@Override
			public boolean hasPrettyPrinter() {
				return true;
			}
		};
	}

	private PrettyPrintWriteContext() {
	}
}
