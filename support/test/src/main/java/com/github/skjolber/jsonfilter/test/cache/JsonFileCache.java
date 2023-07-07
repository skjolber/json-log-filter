package com.github.skjolber.jsonfilter.test.cache;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.github.skjolber.jsonfilter.test.pp.PrettyPrintTransformer;

public class JsonFileCache {
	
	private static final JsonFileCache instance = new JsonFileCache();
	
	public static JsonFile get(Path file) {
		return instance.get(file);
	}
	
	protected Map<Path, JsonFile> cache = new ConcurrentHashMap<>();

	public JsonFile getJsonInput(Path file) {
		try {
			JsonFile item = cache.get(file);
			if(item == null) {
				String string = getFile(file);

				List<String> prettyPrinted;
				if(JsonValidator.isWellformed(string)) {
					prettyPrinted = PrettyPrintTransformer.ALL.stream().map((t) -> t.apply(string)).collect(Collectors.toList());
				} else {
					prettyPrinted = Collections.emptyList();
				}
				item = new JsonFile(file, string, prettyPrinted);
				
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
				return IOUtils.toString(in, "UTF-8");
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
