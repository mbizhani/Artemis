package org.devocative.artemis.cfg;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class Config {
	private static final String ARTEMIS_BASE_URL_ENV = "ARTEMIS_BASE_URL";
	private static final String ARTEMIS_BASE_URL_SYS_PROP = "artemis.base.url";
	private static final String ARTEMIS_DEV_MODE_ENV = "ARTEMIS_DEV_MODE";
	private static final String ARTEMIS_DEV_MODE_SYS_PROP = "artemis.dev.mode";
	private static final String ARTEMIS_PARALLEL_ENV = "ARTEMIS_PARALLEL";
	private static final String ARTEMIS_PARALLEL_SYS_PROP = "artemis.parallel";

	// ------------------------------

	private final String name;
	private final String xmlName;
	private final String groovyName;
	private String baseUrl;
	private Boolean devMode;
	private List<String> onlyScenarios = Collections.emptyList();
	private String baseDir;
	private Integer parallel;
	private Integer loop = 1;
	private Boolean consoleLog;
	private Map<String, Object> vars = new HashMap<>();
	private String proxy;

	// ------------------------------

	public Config() {
		this("artemis");
	}

	public Config(String name) {
		this(name, name);
	}

	public Config(String xmlName, String groovyName) {
		this.name = xmlName.toLowerCase().endsWith(".xml") ? xmlName.substring(0, xmlName.length() - 4) : xmlName;
		this.xmlName = xmlName.toLowerCase().endsWith(".xml") ? xmlName : xmlName + ".xml";
		this.groovyName = groovyName.toLowerCase().endsWith(".groovy") ? groovyName : groovyName + ".groovy";
	}

	// ------------------------------

	public Config setParallel(int parallel) {
		this.parallel = parallel > 0 ? parallel : 1;
		return this;
	}

	public Config setLoop(int loop) {
		this.loop = loop > 0 ? loop : 1;
		return this;
	}

	public Config addVar(String name, Object value) {
		vars.put(name, value);
		return this;
	}

	// ---------------

	public void init() {
		if (getBaseUrl() == null) {
			setBaseUrl(findValue(ARTEMIS_BASE_URL_ENV, ARTEMIS_BASE_URL_SYS_PROP, "http://localhost:8080"));
		}

		if (getDevMode() == null) {
			setDevMode(Boolean.valueOf(findValue(ARTEMIS_DEV_MODE_ENV, ARTEMIS_DEV_MODE_SYS_PROP, "false")));
		}

		if (getParallel() == null) {
			setParallel(Integer.parseInt(findValue(ARTEMIS_PARALLEL_ENV, ARTEMIS_PARALLEL_SYS_PROP, "1")));
		}
	}

	// ------------------------------

	private String findValue(String envVar, String sysVar, String def) {
		if (System.getenv(envVar) != null) {
			return System.getenv(envVar);
		} else if (System.getProperty(sysVar) != null) {
			return System.getProperty(sysVar);
		}
		return def;
	}
}
