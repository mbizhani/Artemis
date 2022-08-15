package org.devocative.artemis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.http.HttpRequestData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Context {
	private final Map<String, Object> globalVars = new HashMap<>();
	private final Map<String, Object> scenarioVars = new HashMap<>();
	private final Map<String, Object> vars = new HashMap<>();
	private final ContextConfig config;

	private Map<String, String> cookies = Collections.emptyMap();

	@JsonIgnore
	private EVarScope scope;

	// ------------------------------

	public Context() {
		this(null);
	}

	public Context(Context parent) {
		if (parent != null) {
			this.globalVars.putAll(parent.globalVars);
			this.vars.putAll(parent.globalVars);
			this.config = parent.config;
			setCookies(parent.cookies);
		} else {
			this.config = new ContextConfig();
		}
	}

	// ------------------------------

	public void addVar(String name, Object value) {
		addVar(name, value, false);
	}

	public void addVar(String name, Object value, boolean store) {
		if (vars.containsKey(name)) {
			throw new RuntimeException("Duplicate Var for Context: " + name);
		}

		if (scope == null) {
			throw new RuntimeException("Null scope in 'addVar'");
		}

		if (scope == EVarScope.Assert) {
			throw new RuntimeException("Can't 'addVar' in <assertRs/> call");
		}

		if (store) {
			ALog.warn("[Groovy] Added Var Globally: {}", name);
			addVarByScope(name, value, EVarScope.Global);
		} else {
			addVarByScope(name, value, scope);
		}
	}

	public Map<String, Object> getVars() {
		return Immutable.create(vars);
	}

	public Map<String, String> getCookies() {
		return cookies;
	}

	public void setCookies(Map<String, String> cookies) {
		this.cookies = Collections.unmodifiableMap(cookies);
	}

	public ContextConfig getConfig() {
		return config;
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
		if (name == null || name.trim().isEmpty()) {
			throw new TestFailedException("Var Attribute's Name Required");
		}

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

	// ------------------------------

	@Getter
	@Setter
	public static class ContextConfig {
		private Consumer<HttpRequestData> beforeSend;
	}
}
