package com.github.skjolber.jsonfilter.jmh.asm;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class LabelStripperCustomClassWriter {

    ClassReader reader;
    ClassWriter writer;
    RemoveStaticMethodLabelsClassVisitor visitor;

    public LabelStripperCustomClassWriter(Class c, String name) throws IOException {
        reader = new ClassReader(c.getName());
        writer = new ClassWriter(0);
        visitor = new RemoveStaticMethodLabelsClassVisitor(Opcodes.ASM9, writer, name);
    }
    
    public byte[] write() {
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
    
    
}
