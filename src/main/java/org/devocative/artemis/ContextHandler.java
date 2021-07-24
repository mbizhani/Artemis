package org.devocative.artemis;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;

import java.io.InputStreamReader;

public class ContextHandler {
	private static final String ARTEMIS_PROFILE_ENV = "ARTEMIS_PROFILE";
	private static final String ARTEMIS_PROFILE_SYS_PROP = "artemis.profile";

	private final static ThreadLocal<Context> CTX = new ThreadLocal<>();

	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();
	private static final Script MAIN;

	// ------------------------------

	static {
		GroovyShell shell = new GroovyShell();
		MAIN = shell.parse(new InputStreamReader(ContextHandler.class.getResourceAsStream("/artemis.groovy")));
	}

	// ------------------------------

	public static synchronized Context getContext() {
		Context ctx = CTX.get();

		if (ctx == null) {
			CTX.set(ctx = createContext());
		}

		return ctx;
	}

	public static Object eval(String str) {
		try {
			return ENGINE.createTemplate(str).make(getContext().getVars()).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void invoke(String method) {
		MAIN.invokeMethod(method, new Object[]{getContext()});
	}

	// ------------------------------

	private static Context createContext() {
		final String profile;
		if (System.getenv(ARTEMIS_PROFILE_ENV) != null) {
			profile = System.getenv(ARTEMIS_PROFILE_ENV);
		} else if (System.getProperty(ARTEMIS_PROFILE_SYS_PROP) != null) {
			profile = System.getProperty(ARTEMIS_PROFILE_SYS_PROP);
		} else {
			profile = "local";
		}

		final Context ctx = new Context(profile);
		ctx.setBaseUrl("http://localhost:8080");

		MAIN.invokeMethod("init", new Object[]{ctx});

		return ctx;
	}
}
