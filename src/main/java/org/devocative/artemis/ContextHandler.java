package org.devocative.artemis;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.util.HashMap;

@Slf4j
public class ContextHandler {
	private static final String ARTEMIS_PROFILE_ENV = "ARTEMIS_PROFILE";
	private static final String ARTEMIS_PROFILE_SYS_PROP = "artemis.profile";
	private static final String ARTEMIS_BASE_URL_ENV = "ARTEMIS_BASE_URL";
	private static final String ARTEMIS_BASE_URL_SYS_PROP = "artemis.base.url";

	private static final ThreadLocal<Context> CTX = new ThreadLocal<>();
	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();

	private static Script MAIN;
	private static Config CONFIG;

	// ------------------------------

	public static void init(String name, Config config) {
		final GroovyShell shell = new GroovyShell();
		MAIN = shell.parse(new InputStreamReader(
			ContextHandler.class.getResourceAsStream(String.format("/%s.groovy", name))));

		if (config.getProfile() == null) {
			config.setProfile(findValue(ARTEMIS_PROFILE_ENV, ARTEMIS_PROFILE_SYS_PROP, "local"));
		}

		if (config.getBaseUrl() == null) {
			config.setBaseUrl(findValue(ARTEMIS_BASE_URL_ENV, ARTEMIS_BASE_URL_SYS_PROP, "http://localhost:8080"));
		}

		CONFIG = config;
	}

	public static synchronized Context get() {
		Context ctx = CTX.get();

		if (ctx == null) {
			CTX.set(ctx = createContext());
		}

		return ctx;
	}

	public static void shutdown() {
		CTX.remove();
	}

	public static Object eval(String str) {
		try {
			return ENGINE
				.createTemplate(str)
				.make(new HashMap<>(get().getVars()))
				.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void invoke(String method) {
		MAIN.invokeMethod(method, new Object[]{get()});
	}

	// ------------------------------

	private static Context createContext() {
		final Context ctx = new Context(CONFIG.getProfile());
		ctx.addVarByScope("_", MAIN, EVarScope.Global);

		ctx.runAtScope(EVarScope.Global, () -> MAIN.invokeMethod("before", new Object[]{ctx}));

		return ctx;
	}

	private static String findValue(String envVar, String sysVar, String def) {
		if (System.getenv(envVar) != null) {
			return System.getenv(envVar);
		} else if (System.getProperty(sysVar) != null) {
			return System.getProperty(sysVar);
		}
		return def;
	}
}
