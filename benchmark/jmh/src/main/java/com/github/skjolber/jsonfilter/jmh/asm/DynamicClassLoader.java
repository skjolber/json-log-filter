package com.github.skjolber.jsonfilter.jmh.asm;
import java.util.Objects;

public class DynamicClassLoader extends ClassLoader {
	protected byte[] classBytes;
	protected final String className;

	public DynamicClassLoader(ClassLoader contextClassLoader, byte[] classBytes, String className) {
        super(contextClassLoader);
        this.classBytes = classBytes;
        this.className = className;
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        if (Objects.equals(this.className, className)) {
            return defineClass(className, this.classBytes, 0, this.classBytes.length);
        }

        throw new ClassNotFoundException(className);
    }
}