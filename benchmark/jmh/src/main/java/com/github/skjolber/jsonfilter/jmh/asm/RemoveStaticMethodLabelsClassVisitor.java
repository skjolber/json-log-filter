package com.github.skjolber.jsonfilter.jmh.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RemoveStaticMethodLabelsClassVisitor extends ClassVisitor {

	private String name;
	
	public RemoveStaticMethodLabelsClassVisitor(int api, ClassWriter writer, String name) {
		super(api, writer);
		this.name = name;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// TODO Auto-generated method stub
		super.visit(version, access, this.name, signature, superName, interfaces);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions);
		if((access & Opcodes.ACC_STATIC ) == 0) {
			return visitMethod;
		}
		return new LabelMethodVisitor(api, visitMethod);
	}
	
}
