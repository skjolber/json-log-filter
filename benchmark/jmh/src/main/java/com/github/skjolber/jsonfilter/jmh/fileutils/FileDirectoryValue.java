package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.File;

public class FileDirectoryValue {

	private File directory;
	
	private File[] files;
	
	private char[][] values;
	
	public FileDirectoryValue(File directory, File[] inputFiles, String[] inputValues) {
		this.directory = directory;
		this.files = inputFiles;
		this.values = new char[inputValues.length][];
		for(int i = 0; i < values.length; i++) {
			this.values[i] = inputValues[i].toCharArray();
		}
	}

	public int size() {
		return files.length;
	}
	
	public char[] getValue(int index) {
		return values[index];
	}

	public File getFile(int index) {
		return files[index];
	}

	public File[] getFiles() {
		return files;
	}

	public void setFiles(File[] files) {
		this.files = files;
	}

	public char[][] getValues() {
		return values;
	}

	public void setValues(char[][] values) {
		this.values = values;
	}

	public String getStringValue(int i) {
		return new String(values[i]);
	}
	
	
	public File getDirectory() {
		return directory;
	}

	public void setValue(int i, String clean) {
		this.values[i] = clean.toCharArray();
	}
	
}
