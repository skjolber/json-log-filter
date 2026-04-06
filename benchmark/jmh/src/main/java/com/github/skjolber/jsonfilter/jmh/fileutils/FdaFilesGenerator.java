package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

/**
 * Generator for FDA adverse events benchmark files.
 * Input: fda2024.json.gz (compact JSON with fda_adverse_events top-level array)
 * Output: Sized files by truncating after complete top-level array objects.
 */
public class FdaFilesGenerator {

	private final File source;
	private final JsonFactory jsonFactory;

	public FdaFilesGenerator(File source) {
		this.source = source;
		this.jsonFactory = new JsonFactory();
	}

	public void generateFileSizes(File directory, int... kbs) throws IOException {
		StringBuilder parameters = new StringBuilder();
		parameters.append("@Param(value={");

		for (int kb : kbs) {
			byte[] output = generateFileSize(kb * 1024);

			File child = new File(directory, (output.length / 1024) + "KB");
			child.mkdirs();

			File destination = new File(child, source.getName() + ".json");
			try (FileOutputStream fout = new FileOutputStream(destination)) {
				fout.write(output);
			}

			parameters.append('"').append(child.getName()).append('"').append(',');
		}
		parameters.setLength(parameters.length() - 1);
		parameters.append("})");
		System.out.println("JMH Parameter: " + parameters);
	}

	public byte[] generateFileSize(int size) throws IOException {
		try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(source))) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try (JsonParser parser = jsonFactory.createParser(gzis);
				 JsonGenerator generator = jsonFactory.createGenerator(bout)) {

				int level = 0;
				do {
					JsonToken nextToken = parser.nextToken();
					if (nextToken == null) break;
					generator.copyCurrentEvent(parser);
					generator.flush();

					if (nextToken == JsonToken.START_OBJECT) {
						level++;
					} else if (nextToken == JsonToken.END_OBJECT) {
						level--;
						// Cut after complete top-level array objects (depth 2: root object + array)
						if (level == 2) {
							if (bout.size() >= size) break;
						}
					}
				} while (true);
			}
			return bout.toByteArray();
		}
	}

	public static void main(String[] args) throws IOException {
		File file = new File("src/test/resources/benchmark/fda2024.json.gz");

		FdaFilesGenerator generator = new FdaFilesGenerator(file);
		generator.generateFileSizes(
			new File(file.getParent(), "fda"),
			0, 4, 12, 20, 30, 50, 70, 100, 200
		);
	}
}
