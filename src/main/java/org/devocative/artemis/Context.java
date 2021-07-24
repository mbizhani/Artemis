package org.devocative.artemis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class Context {
	private final Map<String, Object> vars = new HashMap<>();
	private final String profile;
	private String baseUrl;

	// ------------------------------

	public void addVar(String name, Object value) {
		vars.put(name, value);
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
}
