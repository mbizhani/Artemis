package org.devocative.artemis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class Context {
	private final Map<String, Object> globalVars = new HashMap<>();
	private final Map<String, Object> vars = new HashMap<>();
	private final String profile;
	private String baseUrl;

	// ------------------------------

	public void addVar(String name, Object value) {
		vars.put(name, value);
	}

	public Map<String, Object> getVars() {
		return Collections.unmodifiableMap(vars);
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	// ---------------

	void addGlobalVar(String name, Object value) {
		globalVars.put(name, value);
		vars.put(name, value);
	}

	void clearVars() {
		vars.clear();
		vars.putAll(globalVars);
	}
}
