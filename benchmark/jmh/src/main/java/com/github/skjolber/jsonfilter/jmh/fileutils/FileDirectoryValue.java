package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class FileDirectoryValue {

	private File directory;
	
	private File[] files;
	
	private char[][] valuesAsCharacters;
	private byte[][] valuesAsBytes;
	
	public FileDirectoryValue(File directory, File[] inputFiles, String[] inputValues) {
		this.directory = directory;
		this.files = inputFiles;
		this.valuesAsCharacters = new char[inputValues.length][];
		this.valuesAsBytes = new byte[inputValues.length][];
		for(int i = 0; i < valuesAsCharacters.length; i++) {
			this.valuesAsCharacters[i] = inputValues[i].toCharArray();
			this.valuesAsBytes[i] = inputValues[i].getBytes(StandardCharsets.UTF_8);
		}
	}

	public int size() {
		return files.length;
	}

	public byte[] getValueAsBytes(int index) {
		return valuesAsBytes[index];
	}

	public char[] getValueAsCharacters(int index) {
		return valuesAsCharacters[index];
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
		return valuesAsCharacters;
	}

	public void setValues(char[][] values) {
		this.valuesAsCharacters = values;
	}

	public String getStringValue(int i) {
		return new String(valuesAsCharacters[i]);
	}
	
	
	public File getDirectory() {
		return directory;
	}

	public void setValue(int i, String clean) {
		this.valuesAsCharacters[i] = clean.toCharArray();
	}
	
}
