package com.github.skjolber.jsonfilter.jmh.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class LabelMethodVisitor extends MethodVisitor {

	public LabelMethodVisitor(int api, MethodVisitor methodVisitor) {
		super(api, methodVisitor);
	}

	public LabelMethodVisitor(int api) {
		super(api);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		// do nothing
	}
	
}
