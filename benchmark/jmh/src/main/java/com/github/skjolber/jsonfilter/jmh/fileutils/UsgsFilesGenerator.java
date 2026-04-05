package com.github.skjolber.jsonfilter.jmh.fileutils;

import java.io.*;
import java.util.zip.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

/**
 * Benchmark file generator for USGS earthquake GeoJSON data.
 *
 * Input: usgs2024.json.gz (compact GeoJSON FeatureCollection with ~11K earthquake features)
 * Output: Sized files truncated at complete feature boundaries.
 *
 * Level tracking differs from CveFilesGenerator: USGS features close at level 1
 * (root=1, feature=2, properties/geometry=3), so we cut when level==1 after
 * writing a complete feature's closing brace. The Jackson generator auto-closes
 * the open features array and root object, producing valid JSON output.
 */
public class UsgsFilesGenerator {

    private final File source;
    private final JsonFactory jsonFactory;

    public UsgsFilesGenerator(File source) {
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
                        // Cut after complete top-level feature objects.
                        // USGS features are at depth 2 (root=1, feature=2),
                        // so level==1 after END_OBJECT means a top-level object
                        // (metadata or a feature) just completed.
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
        File file = new File("src/test/resources/benchmark/usgs2024.json.gz");
        UsgsFilesGenerator generator = new UsgsFilesGenerator(file);
        generator.generateFileSizes(
            new File(file.getParent(), "usgs"),
            0, 4, 12, 20, 30, 50, 70, 100, 200
        );
    }
}
