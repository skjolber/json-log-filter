package com.github.skjolber.jsonfilter.test.cache;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.test.jackson.JsonCharSizeIterator;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.github.skjolber.jsonfilter.test.pp.PrettyPrintTransformer;

public class JsonFileCache {
	
	private static final JsonFileCache instance = new JsonFileCache();

	private static final JsonFactory jsonFactory = new JsonFactory();
	
	public static JsonFile get(Path file) {
		return instance.get(file);
	}
	
	protected Map<Path, JsonFile> cache = new ConcurrentHashMap<>();

	public JsonFile getJsonInput(Path file) {
		try {
			JsonFile item = cache.get(file);
			if(item == null) {
				String string = getFile(file);

				if(JsonValidator.isWellformed(string)) {
					List<MaxSizeJsonCollection> maxSizePermutations = new ArrayList<>();
					
					JsonCharSizeIterator maxSizeIterator = new JsonCharSizeIterator(jsonFactory, string);
					
					List<String> prettyPrinted = new ArrayList<>();
					
					while(maxSizeIterator.hasNext()) {
						MaxSizeJsonItem next = maxSizeIterator.next();
						String maxSizeValue = next.getContentAsString();
						
						List<MaxSizeJsonItem> items = new ArrayList<>(PrettyPrintTransformer.ALL.size());

						for(PrettyPrintTransformer transformer : PrettyPrintTransformer.ALL) {
							items.add(maxSizeIterator.next(transformer.getPrettyPrinter().createInstance()));
						}
						maxSizePermutations.add(new MaxSizeJsonCollection(maxSizeValue, next.getMark(), next.getLevel(), items));
					}
					
					for(PrettyPrintTransformer transformer : PrettyPrintTransformer.ALL) {
						prettyPrinted.add(transformer.apply(string));
					}

					item = new JsonFile(file, string, prettyPrinted, maxSizePermutations);
				} else {
					item = new JsonFile(file, string, Collections.emptyList(), Collections.emptyList());
				}
				
				cache.put(file, item);
			}
			return item;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getFile(Path path) {
		try {
			try (InputStream in = Files.newInputStream(path)) {
				return IOUtils.toString(in, StandardCharsets.UTF_8);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void clear() {
		cache.clear();
	}

	public static JsonFileCache getInstance() {
		return instance;
	}
}
