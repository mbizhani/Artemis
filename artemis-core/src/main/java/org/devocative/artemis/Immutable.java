package org.devocative.artemis;

import java.util.*;

public class Immutable {

	public static Map<String, Object> create(Map<String, Object> map) {
		final Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			final Object value = entry.getValue();
			if (value instanceof Map) {
				result.put(entry.getKey(), create((Map) value));
			} else if (value instanceof List) {
				result.put(entry.getKey(), create((List) value));
			} else {
				result.put(entry.getKey(), value);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	public static List<?> create(List<?> list) {
		final List<Object> result = new ArrayList<>();

		for (Object cell : list) {
			if (cell instanceof Map) {
				result.add(create((Map) cell));
			} else if (cell instanceof List) {
				result.add(create((List<?>) cell));
			} else {
				result.add(cell);
			}
		}

		return Collections.unmodifiableList(result);
	}
}
