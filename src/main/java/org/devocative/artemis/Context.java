package org.devocative.artemis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Context {
	private final Map<String, Object> globalVars = new HashMap<>();
	private final Map<String, Object> scenarioVars = new HashMap<>();
	private final Map<String, Object> vars = new HashMap<>();

	private final String profile;

	private EVarScope scope;

	// ------------------------------

	public Context() {
		this("default");
	}

	public Context(String profile) {
		this.profile = profile;
	}

	// ------------------------------

	public void addVar(String name, Object value) {
		if (vars.containsKey(name)) {
			throw new RuntimeException("Duplicate Var for Context: " + name);
		}

		addVarByScope(name, value, scope);
	}

	public Map<String, Object> getVars() {
		return Collections.unmodifiableMap(vars);
	}

	public String getProfile() {
		return profile;
	}

	// ---------------

	void runAtScope(EVarScope scope, Runnable code) {
		this.scope = scope;
		code.run();
		this.scope = null;
	}

	boolean containsVar(String name, EVarScope scope) {
		switch (scope) {
			case Global:
				return globalVars.containsKey(name);
			case Scenario:
				return scenarioVars.containsKey(name);
			case Request:
				break;
		}
		return vars.containsKey(name);
	}

	Object removeVar(String name, EVarScope scope) {
		Object result;
		result = vars.remove(name);

		switch (scope) {
			case Global:
				result = globalVars.remove(name);
				break;
			case Scenario:
				result = scenarioVars.remove(name);
				break;
		}

		return result;
	}

	void addVarByScope(String name, Object value, EVarScope scope) {
		vars.put(name, value);

		switch (scope) {
			case Global:
				globalVars.put(name, value);
				break;
			case Scenario:
				scenarioVars.put(name, value);
				break;
		}
	}

	void clearVars(EVarScope scope) {
		vars.clear();
		vars.putAll(globalVars);

		switch (scope) {
			case Scenario:
				scenarioVars.clear();
				break;
			case Request:
				vars.putAll(scenarioVars);
				break;
		}
	}
}
