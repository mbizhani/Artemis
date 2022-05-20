package groovy.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devocative.artemis.ALog;
import org.devocative.artemis.ContextHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Artemis {
	private static final List<String> CHARS = new ArrayList<>();
	private static final Random RANDOM = new Random();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Base64.Encoder B64_ENC = Base64.getEncoder();
	private static final Base64.Decoder B64_DEC = Base64.getDecoder();

	private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	// ------------------------------

	static {
		for (Character c = '0'; c <= '9'; c++) {
			CHARS.add(c.toString());
		}
		for (Character c = 'a'; c <= 'z'; c++) {
			CHARS.add(c.toString());
		}
	}

	// ------------------------------

	public static void setDefaultCharset(Charset chs) {
		DEFAULT_CHARSET = chs;
	}

	public static String generate(int len) {
		return generate(len, CHARS);
	}

	@SafeVarargs
	public static String generate(int len, List<String>... alphaSets) {
		final List<String> alphabets = Arrays.stream(alphaSets)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < len; i++) {
			builder.append(alphabets.get(RANDOM.nextInt(alphabets.size())));
		}
		return builder.toString();
	}

	public static int rand(int bound) {
		return RANDOM.nextInt(bound);
	}

	public static String rand(int bound, String pattern) {
		return format(RANDOM.nextInt(bound), pattern);
	}

	public static String format(Number number, String pattern) {
		final DecimalFormat df = new DecimalFormat(pattern);
		return df.format(number);
	}

	public static String format(Date number, String pattern) {
		final SimpleDateFormat df = new SimpleDateFormat(pattern);
		return df.format(number);
	}

	public static String jsonify(Object obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object objectify(String json) {
		try {
			return MAPPER.readValue(json, Object.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void log(String log) {
		ALog.info("[Groovy] - " + log);
	}

	public static String uuid() {
		return UUID.randomUUID().toString();
	}

	public static String encBase64(String str) {
		return encBase64(str, true);
	}

	public static String encBase64(String str, boolean withPadding) {
		return withPadding ?
			B64_ENC.encodeToString(str.getBytes(DEFAULT_CHARSET)) :
			B64_ENC.withoutPadding().encodeToString(str.getBytes(DEFAULT_CHARSET));
	}

	public static String decBase64(String str) {
		return new String(B64_DEC.decode(str.getBytes(DEFAULT_CHARSET)), DEFAULT_CHARSET);
	}

	public static HttpBuilder http() {
		return HttpBuilder.get();
	}

	public static String readFile(String name) {
		final InputStream in = ContextHandler.loadFile(name);

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			return reader.lines().collect(Collectors.joining("\n"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// --- PKI
}
