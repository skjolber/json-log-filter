package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.*;
import java.util.zip.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

/**
 * Entur public transport lines benchmark files generator.
 *
 * Input: entur2024.json.gz (compact JSON with 399 transport lines from RUT authority)
 * Output: Sized files truncated at complete line boundaries.
 *
 * Structure: {"lines": [{line_obj}]}
 * root=1, line_obj=2, nested=3+. Cut at level==1 after each complete line closes.
 */
public class EnturFilesGenerator {

    private final File source;
    private final JsonFactory jsonFactory;

    public EnturFilesGenerator(File source) {
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
                        // Cut after each complete top-level line object.
                        // Lines are at depth 2 (root=1, line=2),
                        // so level==1 after END_OBJECT means a line just completed.
                        if (level == 1) {
                            if (bout.size() >= size) {
                                break;
                            }
                        }
                    }
                } while (true);
            }
            return bout.toByteArray();
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("src/test/resources/benchmark/entur2024.json.gz");
        EnturFilesGenerator generator = new EnturFilesGenerator(file);
        generator.generateFileSizes(
            new File(file.getParent(), "entur"),
            0, 4, 12, 20, 30, 50, 70, 100, 200
        );
    }
}
