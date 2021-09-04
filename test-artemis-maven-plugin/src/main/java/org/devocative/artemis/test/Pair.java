package org.devocative.artemis.test;

public class Pair {
	private final String key;
	private final Object value;

	// ------------------------------

	public Pair(String key, Object value) {
		this.key = key;
		this.value = value;
	}

	// ------------------------------

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public static Pair pair(String key, Object value) {
		return new Pair(key, value);
	}
}
