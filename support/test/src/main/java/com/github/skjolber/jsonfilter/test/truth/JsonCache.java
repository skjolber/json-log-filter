package com.github.skjolber.jsonfilter.test.truth;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.skjolber.jsonfilter.test.JsonFileCache;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;
import com.github.skjolber.jsonfilter.test.pp.PrettyPrintTransformer;

public class JsonCache {
	
	private static final JsonCache instance = new JsonCache();
	
	public static JsonInput get(Path file) {
		return instance.get(file);
	}
	
	protected Map<Path, JsonInput> cache = new ConcurrentHashMap<>();

	public JsonInput getJsonInput(Path file) {
		try {
			JsonInput item = cache.get(file);
			if(item == null) {
				String string = JsonFileCache.getInstance().getFile(file);

				List<String> prettyPrinted;
				if(JsonValidator.isWellformed(string)) {
					prettyPrinted = PrettyPrintTransformer.ALL.stream().map((t) -> t.apply(string)).collect(Collectors.toList());
				} else {
					prettyPrinted = Collections.emptyList();
				}
				item = new JsonInput(file, string, prettyPrinted);
				
				cache.put(file, item);
			}
			return item;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void clear() {
		cache.clear();
	}
}
