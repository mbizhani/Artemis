package org.devocative.artemis;

import java.util.HashMap;
import java.util.Map;

public class Context {
	private final Map<String, Object> vars = new HashMap<>();
	private String baseUrl;

	// ------------------------------

	public void addVar(String name, Object value) {
		vars.put(name, value);
	}

	public Map<String, Object> getVars() {
		return new HashMap<>(vars);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
}
