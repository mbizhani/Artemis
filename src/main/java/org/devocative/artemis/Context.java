package org.devocative.artemis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Context {
	private final Map<String, Object> globalVars = new HashMap<>();
	private final Map<String, Object> vars = new HashMap<>();

	// ------------------------------

	public void addVar(String name, Object value) {
		if (name == null || "_".equals(name)) {
			throw new RuntimeException("Invalid Var for Context: " + name);
		}
		vars.put(name, value);
	}

	public Map<String, Object> getVars() {
		return Collections.unmodifiableMap(vars);
	}

	// ---------------

	boolean containsVar(String name) {
		return vars.containsKey(name);
	}

	Object removeVar(String name) {
		return vars.remove(name);
	}

	void addGlobalVar(String name, Object value) {
		globalVars.put(name, value);
		vars.put(name, value);
	}

	void clearVars() {
		vars.clear();
		vars.putAll(globalVars);
	}
}
