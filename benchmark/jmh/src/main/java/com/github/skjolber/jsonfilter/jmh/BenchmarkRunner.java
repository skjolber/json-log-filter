package com.github.skjolber.jsonfilter.jmh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryCache;
import com.github.skjolber.jsonfilter.jmh.fileutils.FileDirectoryValue;

public class BenchmarkRunner<T extends JsonFilter> {
	
	protected List<FileDirectoryValue> directories;
	protected T jsonFilter;

	protected StringBuilder builder = new StringBuilder(256 * 1000);
	protected ByteArrayOutputStream outputstream = new ByteArrayOutputStream(256 * 1000);
	
	protected boolean newBuilder;

	public BenchmarkRunner(File file, boolean recursive, T filter) throws IOException {
		this(file, recursive, filter, false);
	}

	public BenchmarkRunner(File file, boolean recursive, T filter, boolean newBuilder) throws IOException {
		this(file, recursive);
		this.newBuilder = newBuilder;
		
		setJsonFilter(filter);
	}

	public BenchmarkRunner(File file, boolean recursive) throws IOException {
		directories = new FileDirectoryCache().getValue(file, new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".json");
			}
		}, recursive);
	}

	public JsonFilter getJsonFilter() {
		return jsonFilter;
	}

	public void setJsonFilter(T filter) {
		this.jsonFilter = filter;
	}

	public long benchmarkCharacters() {

		// warmup
		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				char[] chars = directory.getValueAsCharacters(i);
				
				StringBuilder builder;
				if(newBuilder) {
					builder = new StringBuilder(chars.length);
				} else {
					builder = this.builder;
				}
				if(jsonFilter.process(chars, 0, chars.length, builder)) {
					sizeSum += builder.length();
				} else {
					throw new RuntimeException("Unable to filter using " + jsonFilter + " for source " + directory.getFile(i));
				}
				
				// reset builder for next iteration
				builder.setLength(0);
			}
		}
		if(sizeSum == 0) {
			throw new IllegalArgumentException();
		}
		return sizeSum;
	}
	
	public long benchmarkBytesViaChars() {

		// warmup
		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				byte[] bytes = directory.getValueAsBytes(i);
				
				StringBuilder builder;
				if(newBuilder) {
					builder = new StringBuilder(bytes.length);
				} else {
					builder = this.builder;
				}
				String text = new String(bytes, StandardCharsets.UTF_8);
				char[] chars = text.toCharArray();
				if(jsonFilter.process(chars, 0, chars.length, builder)) {
					sizeSum += builder.length();
				} else {
					throw new RuntimeException("Unable to filter using " + jsonFilter + " for source " + directory.getFile(i));
				}
				
				// reset builder for next iteration
				builder.setLength(0);
			}
		}
		if(sizeSum == 0) {
			throw new IllegalArgumentException();
		}
		return sizeSum;
	}	
	
	public long benchmarkBytes() throws IOException {

		// warmup
		long sizeSum = 0;
		for(FileDirectoryValue directory : directories) {
			
			for(int i = 0; i < directory.size(); i++) {
				byte[] bytes = directory.getValueAsBytes(i);
				
				ByteArrayOutputStream builder;
				if(newBuilder) {
					builder = new ByteArrayOutputStream(bytes.length);
				} else {
					builder = this.outputstream;
				}
				if(jsonFilter.process(bytes, 0, bytes.length, builder)) {
					sizeSum += builder.toString().length(); // note: string output
				} else {
					throw new RuntimeException("Unable to filter using " + jsonFilter + " for source " + directory.getFile(i));
				}
				
				// reset builder for next iteration
				builder.reset();
			}
		}
		if(sizeSum == 0) {
			throw new IllegalArgumentException();
		}
		return sizeSum;
	}	
}
