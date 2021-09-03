package groovy.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devocative.artemis.ALog;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Artemis {
	private static final List<String> CHARS = new ArrayList<>();
	private static final Random RANDOM = new Random();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		for (Character c = '0'; c <= '9'; c++) {
			CHARS.add(c.toString());
		}
		for (Character c = 'a'; c <= 'z'; c++) {
			CHARS.add(c.toString());
		}
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

	public static void log(String log) {
		ALog.info("[Groovy] - " + log);
	}
}
