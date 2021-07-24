package org.devocative.artemis.test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Pair {
	private final String key;
	private final Object value;

	// ------------------------------

	public static Pair pair(String key, Object value) {
		return new Pair(key, value);
	}
}
