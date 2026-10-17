import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.AnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.core.MaxSizeFromEndJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxStringLengthMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.PathMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonJsonLogFilterBuilder;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonMaxSizeMaxStringLengthJsonFilter;
import com.github.skjolber.jsonfilter.jackson.JacksonPathMaxSizeMaxStringLengthJsonFilter;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;

/**
 * JIT snapshot of one filter configuration, see PLAN.md: runs the filter in-process with JFR's compilation, inlining,
 * deoptimization and allocation events, then prints what HotSpot did with the library's methods, sorted so that two
 * builds can be diffed.
 *
 * Usage: java -cp benchmarks.jar JitProbe.java <config>[-chars|-bytes] <document dir> [seconds]
 * Configs mirror CveFilterBenchmark and CvePathMaxSizeMaxStringLengthFilterBenchmark: max size is half the document
 * (the documents are cut), except for maxSizeFromEnd (3/4, the backward scan's range); max string length is 64.
 */
public class JitProbe {

	static final String PACKAGE = "com.github.skjolber.jsonfilter";
	/** -Dprofile=true adds execution samples, every millisecond, by method and line */
	static final boolean PROFILE = Boolean.getBoolean("profile");
	static final int MAX_STRING_LENGTH = 64;
	static final String[] ANON = {"/CVE_Items/cve/affects/vendor/vendor_data/vendor_name"};
	static final String[] PRUNE = {"/CVE_Items/cve/references", "//version"};

	static final Map<String, Function<Integer, JsonFilter>> CONFIGS = new LinkedHashMap<>();
	static {
		CONFIGS.put("maxSize", size -> new MaxSizeJsonFilter(size / 2));
		CONFIGS.put("maxSizeFromEnd", size -> new MaxSizeFromEndJsonFilter(size * 3 / 4));
		CONFIGS.put("maxStringLength", size -> new MaxStringLengthJsonFilter(MAX_STRING_LENGTH));
		CONFIGS.put("maxStringLengthMaxSize", size -> new MaxStringLengthMaxSizeJsonFilter(MAX_STRING_LENGTH, size / 2));
		CONFIGS.put("path", size -> new DefaultJsonLogFilterBuilder().withMaxStringLength(MAX_STRING_LENGTH).withAnonymize(ANON).withPrune(PRUNE).build());
		CONFIGS.put("pathMaxSize", size -> new PathMaxSizeMaxStringLengthJsonFilter(MAX_STRING_LENGTH, size / 2, -1, ANON, PRUNE));
		CONFIGS.put("anyPath", size -> new AnyPathJsonFilter(-1, new String[] {"//version"}, null));
		CONFIGS.put("removeWhitespace", size -> new RemoveWhitespaceJsonFilter());
		CONFIGS.put("maxStringLengthMaxSizeWs", size -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(MAX_STRING_LENGTH, size / 2));
		CONFIGS.put("pathMaxSizeWs", size -> new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(MAX_STRING_LENGTH, size / 2, -1, ANON, PRUNE));
		CONFIGS.put("jacksonMaxSize", size -> new JacksonMaxSizeJsonFilter(size / 2));
		CONFIGS.put("jacksonMaxStringLength", size -> new JacksonJsonLogFilterBuilder().withMaxStringLength(MAX_STRING_LENGTH).build());
		CONFIGS.put("jacksonMaxStringLengthMaxSize", size -> new JacksonMaxSizeMaxStringLengthJsonFilter(MAX_STRING_LENGTH, size / 2));
		CONFIGS.put("jacksonPathMaxSize", size -> new JacksonPathMaxSizeMaxStringLengthJsonFilter(MAX_STRING_LENGTH, size / 2, ANON, PRUNE));
	}

	public static void main(String[] args) throws Exception {
		String spec = args[0];
		boolean bytesInput = !spec.endsWith("-chars");
		String config = spec.replaceAll("-(chars|bytes)$", "");
		File[] files = new File(args[1]).listFiles((d, n) -> n.endsWith(".json"));
		int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 6;
		if(!CONFIGS.containsKey(config) || files == null || files.length == 0) {
			System.err.println("Configs: " + CONFIGS.keySet() + " with -chars or -bytes; then a directory with .json documents");
			System.exit(1);
		}
		byte[] bytes = Files.readAllBytes(files[0].toPath());
		char[] chars = new String(bytes, StandardCharsets.UTF_8).toCharArray();
		JsonFilter filter = CONFIGS.get(config).apply(bytes.length);

		Path file = Files.createTempFile("jit-probe", ".jfr");
		long calls = 0;
		long sink = 0;
		try (Recording recording = new Recording()) {
			recording.enable("jdk.Compilation").withThreshold(Duration.ZERO);
			recording.enable("jdk.CompilerInlining");
			recording.enable("jdk.Deoptimization");
			recording.enable("jdk.CompilationFailure");
			recording.enable("jdk.ObjectAllocationSample").with("throttle", "1000/s").withStackTrace();
			if(PROFILE) {
				recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(1)).withStackTrace();
			}
			recording.start();
			long end = System.nanoTime() + seconds * 1_000_000_000L;
			while(System.nanoTime() < end) {
				for(int i = 0; i < 100; i++) {
					if(bytesInput) {
						byte[] output = filter.process(bytes);
						sink += output == null ? -1 : output.length;
					} else {
						String output = filter.process(chars);
						sink += output == null ? -1 : output.length();
					}
				}
				calls += 100;
			}
			recording.stop();
			recording.dump(file);
		}
		List<RecordedEvent> events = RecordingFile.readAllEvents(file);
		Files.delete(file);

		System.out.println("# " + spec + " on " + files[0].getName() + " (" + bytes.length + " bytes), " + seconds + " s, "
				+ calls + " calls, output " + (sink / Math.max(1, calls)) + " per call, " + System.getProperty("java.vm.version"));

		// C2 compilations of library methods: the last one per method, without OSR, and whether there was an OSR one
		Map<Integer, String> tier4 = new TreeMap<>();
		Map<String, String> compiled = new TreeMap<>();
		Map<String, Integer> recompiled = new TreeMap<>();
		for(RecordedEvent e : events) {
			if(!e.getEventType().getName().equals("jdk.Compilation") || e.getShort("compileLevel") != 4) {
				continue;
			}
			RecordedMethod m = e.getValue("method");
			if(!isLibrary(m)) {
				continue;
			}
			tier4.put(e.getInt("compileId"), name(m));
			String key = name(m) + (e.getBoolean("isOsr") ? " (OSR)" : "");
			compiled.put(key, String.format("code %6d B, inlined %5d B", e.getLong("codeSize"), e.getLong("inlinedBytes")));
			recompiled.merge(key, 1, Integer::sum);
		}
		System.out.println("\n## C2 compilations (last one per method; xN = compiled N times)");
		compiled.forEach((k, v) -> System.out.println(v + "  " + k + (recompiled.get(k) > 1 ? "  x" + recompiled.get(k) : "")));

		// inlining which C2 refused, within the library's C2 compilations
		Map<String, Integer> notInlined = new TreeMap<>();
		for(RecordedEvent e : events) {
			if(!e.getEventType().getName().equals("jdk.CompilerInlining") || e.getBoolean("succeeded") || !tier4.containsKey(e.getInt("compileId"))) {
				continue;
			}
			RecordedMethod caller = e.getValue("caller");
			RecordedObject callee = e.getValue("callee");
			String calleeName = callee.getString("type").replace('/', '.') + "." + callee.getString("name");
			notInlined.merge(shorten(name(caller)) + " -> " + shorten(calleeName) + ": " + e.getString("message"), 1, Integer::sum);
		}
		System.out.println("\n## Not inlined by C2 (caller -> callee: reason, in how many C2 compilations)");
		notInlined.forEach((k, v) -> System.out.println(k + (v > 1 ? "  x" + v : "")));

		Map<String, Integer> deopts = new TreeMap<>();
		for(RecordedEvent e : events) {
			if(e.getEventType().getName().equals("jdk.Deoptimization") && isLibrary(e.getValue("method"))) {
				deopts.merge(shorten(name(e.getValue("method"))) + " bci " + e.getInt("bci") + ": " + e.getString("reason") + ", " + e.getString("action"), 1, Integer::sum);
			}
		}
		System.out.println("\n## Deoptimizations");
		deopts.forEach((k, v) -> System.out.println(k + (v > 1 ? "  x" + v : "")));

		// allocation samples, by allocated class and the innermost library frame; weight is the sampled bytes
		Map<String, Long> allocations = new TreeMap<>();
		long total = 0;
		for(RecordedEvent e : events) {
			if(!e.getEventType().getName().equals("jdk.ObjectAllocationSample") || e.getStackTrace() == null) {
				continue;
			}
			String site = null;
			for(RecordedFrame frame : e.getStackTrace().getFrames()) {
				if(isLibrary(frame.getMethod())) {
					site = shorten(name(frame.getMethod())) + ":" + frame.getLineNumber();
					break;
				}
			}
			if(site == null) {
				continue;
			}
			long weight = e.getLong("weight");
			total += weight;
			allocations.merge(e.getClass("objectClass").getName() + " at " + site, weight, Long::sum);
		}
		long totalWeight = Math.max(1, total);
		System.out.println("\n## Allocation samples (share of sampled bytes)");
		allocations.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(15)
			.forEach(e -> System.out.printf("%5.1f%%  %s%n", 100.0 * e.getValue() / totalWeight, e.getKey()));

		if(PROFILE) {
			// execution samples after the first third, i.e. after warm-up: the innermost frame, and the innermost library frame
			long start = Long.MAX_VALUE, end = 0;
			for(RecordedEvent e : events) {
				if(e.getEventType().getName().equals("jdk.ExecutionSample")) {
					start = Math.min(start, e.getStartTime().toEpochMilli());
					end = Math.max(end, e.getStartTime().toEpochMilli());
				}
			}
			long from = start + (end - start) / 3;
			Map<String, Integer> leaf = new TreeMap<>();
			Map<String, Integer> library = new TreeMap<>();
			int samples = 0;
			for(RecordedEvent e : events) {
				if(!e.getEventType().getName().equals("jdk.ExecutionSample") || e.getStartTime().toEpochMilli() < from || e.getStackTrace() == null || e.getStackTrace().getFrames().isEmpty()) {
					continue;
				}
				samples++;
				List<RecordedFrame> frames = e.getStackTrace().getFrames();
				leaf.merge(shorten(name(frames.get(0).getMethod())), 1, Integer::sum);
				for(RecordedFrame frame : frames) {
					if(isLibrary(frame.getMethod())) {
						library.merge(shorten(name(frame.getMethod())) + ":" + frame.getLineNumber(), 1, Integer::sum);
						break;
					}
				}
			}
			int sampleCount = Math.max(1, samples);
			System.out.println("\n## Execution samples after warm-up (" + samples + "), innermost frame");
			leaf.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(15)
				.forEach(e -> System.out.printf("%5.1f%%  %s%n", 100.0 * e.getValue() / sampleCount, e.getKey()));
			System.out.println("\n## Execution samples after warm-up, innermost library frame and line");
			library.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(20)
				.forEach(e -> System.out.printf("%5.1f%%  %s%n", 100.0 * e.getValue() / sampleCount, e.getKey()));
		}
	}

	static boolean isLibrary(RecordedMethod m) {
		return m != null && m.getType().getName().startsWith(PACKAGE) && !m.getType().getName().startsWith(PACKAGE + ".jmh");
	}

	static String name(RecordedMethod m) {
		return m.getType().getName() + "." + m.getName() + m.getDescriptor().replaceAll("L[a-z/]*/([A-Za-z0-9$]+);", "$1;");
	}

	static String shorten(String name) {
		return name.replace(PACKAGE + ".", "");
	}
}
