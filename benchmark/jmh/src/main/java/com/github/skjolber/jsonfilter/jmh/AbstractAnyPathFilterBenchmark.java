package com.github.skjolber.jsonfilter.jmh;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.runner.RunnerException;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMultiAnyPathMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jmh.asm.DynamicClassLoader;
import com.github.skjolber.jsonfilter.jmh.asm.RemoveStaticMethodLabelsClassVisitor;

public abstract class AbstractAnyPathFilterBenchmark {

	protected JacksonBenchmarkRunner jacksonJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> coreMultipleJsonFilter;
	protected BenchmarkRunner<? extends JsonFilter> fastCoreMultipleJsonFilter;
	
	@Setup
	public void init() throws Exception {
		File file = getFile();
		
		String path = getPath();
		FilterType type = getFilterType();
		
		String[] paths = new String[]{path};
		
		jacksonJsonFilter = new JacksonBenchmarkRunner(file, true, new JacksonMultiAnyPathMaxStringLengthJsonFilter(-1, type == FilterType.ANON ? paths : null, type == FilterType.PRUNE ? paths : null), true, false);
		coreMultipleJsonFilter = new BenchmarkRunner<>(file, true, new AnyPathJsonFilter(-1, type == FilterType.ANON ? paths : null, type == FilterType.PRUNE ? paths : null), true, false);

		JsonFilter filter = null;
		
		Constructor<?>[] constructors2 = getClass(AnyPathJsonFilter.class);
		 for(Constructor<?> constructor : constructors2) {
			if(constructor.getParameterCount() == 3) {
				filter = (JsonFilter)constructor.newInstance(-1, type == FilterType.ANON ? paths : null, type == FilterType.PRUNE ? paths : null);
			}
		 }
		

		fastCoreMultipleJsonFilter = new BenchmarkRunner<>(file, true, filter, true, false);
	}

	private Constructor<?>[] getClass(Class c) throws IOException, ClassNotFoundException {
		ClassWriter writer = new ClassWriter(0);
		
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		String className = "Fast" + c.getSimpleName();
		
		 ClassVisitor visitor = new RemoveStaticMethodLabelsClassVisitor(Opcodes.ASM9, writer, className);
		 
		 ClassReader r = new ClassReader(c.getName());

		 ClassRemapper classRemapper = new ClassRemapper(visitor, new SimpleRemapper(c.getName().replace('.', '/'), className));
		 
		 r.accept(classRemapper, 0);
		 
		 byte[] classBytes = writer.toByteArray();

		 
		 Class<?> class1 = new DynamicClassLoader(contextClassLoader, classBytes, className).loadClass(className);
		 
		 Constructor<?>[] constructors = class1.getConstructors();
		return constructors;
	}
	
	protected abstract FilterType getFilterType();

	protected abstract String getPath();

	protected abstract File getFile();

	@Benchmark
	public long jackson_bytes() throws IOException {
		return jacksonJsonFilter.benchmarkBytes();
	}


	@Benchmark
	public long core_multiple_keep_ws_bytes() throws IOException {
		return coreMultipleJsonFilter.benchmarkBytes();
	}
	

	@Benchmark
	public long fast_multiple_keep_ws_bytes() throws IOException {
		return fastCoreMultipleJsonFilter.benchmarkBytes();
	}


	@Benchmark
	public long core_multiple_keep_ws_bytes2() throws IOException {
		return coreMultipleJsonFilter.benchmarkBytes();
	}
	

	@Benchmark
	public long fast_multiple_keep_ws_bytes2() throws IOException {
		return fastCoreMultipleJsonFilter.benchmarkBytes();
	}



	/*
	@Benchmark
	public long jackson_char() throws IOException {
		return jacksonJsonFilter.benchmarkCharacters();
	}
	
	@Benchmark
	public long core_single_keep_ws_char() throws IOException {
		return coreSingleJsonFilter.benchmarkCharacters();
	}
	
	@Benchmark
	public long core_multiple_keep_ws_char() throws IOException {
		return coreMultipleJsonFilter.benchmarkCharacters();
	}	
	*/
}