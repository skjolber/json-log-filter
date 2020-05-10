package com.github.skjolber.jsonfilter.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * 
 * Test file generator. Input approximate output targets.
 *
 */

public class CveFilesGenerator {

	private File source;
	private JsonFactory jsonFactory;

	public CveFilesGenerator(File source) {
		super();
		this.source = source;
		
		this.jsonFactory = new JsonFactory();
	}
	public void generateFileSizes(File directory, int ... kbs) throws FileNotFoundException, IOException {
		StringBuilder parameters = new StringBuilder();
		parameters.append("@Param(value={");
		
		for(int kb : kbs) {
			
			byte[] output = generateFileSize(kb * 1024);

			File child = new File(directory, (output.length/1024) + "KB");
			child.mkdirs();

			File destination = new File(child, source.getName() + ".json");
			
			try (FileOutputStream fout = new FileOutputStream(destination)) {
				fout.write(output);
			}
			
			parameters.append('"');
			parameters.append(child.getName());
			parameters.append('"');
			parameters.append(',');
		}
		parameters.setLength(parameters.length() - 1);
		parameters.append("})");
		
		System.out.println("JMH Parameter: " + parameters);
	}
	
	public byte[] generateFileSize(int size) throws FileNotFoundException, IOException {
		try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(source));) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try (
				JsonParser parser = jsonFactory.createParser(gzis);
				JsonGenerator generator = jsonFactory.createGenerator(bout)) {
				
				int level = 0;
				do {
					
					JsonToken nextToken = parser.nextToken();
					if(nextToken == null) {
						break;
					}
					
					generator.copyCurrentEvent(parser);
					generator.flush();
					
					if(nextToken == JsonToken.START_OBJECT) {
						level++;
					} else if(nextToken == JsonToken.END_OBJECT) {
						level--;
						
						// cut on CVE
						if(level == 2) {
							if(bout.size() >= size) {
								break;
							}
						}
					}
	
				} while(true);
			}

			return bout.toByteArray();
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File file = new File("src/test/resources/benchmark/cve2006.json.gz");
		
		CveFilesGenerator generator = new CveFilesGenerator(file);
		
		generator.generateFileSizes(new File(file.getParent(), "cves"), 0, 4, 12, 20, 30, 50, 70, 100, 200);
		
	}
}
