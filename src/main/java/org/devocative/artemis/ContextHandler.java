package org.devocative.artemis;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;

@Slf4j
public class ContextHandler {
	private static final String ARTEMIS_PROFILE_ENV = "ARTEMIS_PROFILE";
	private static final String ARTEMIS_PROFILE_SYS_PROP = "artemis.profile";

	private static final ThreadLocal<Context> CTX = new ThreadLocal<>();
	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();

	private static Script MAIN;

	// ------------------------------

	public static void init(String name) {
		final GroovyShell shell = new GroovyShell();
		MAIN = shell.parse(new InputStreamReader(
			ContextHandler.class.getResourceAsStream(String.format("/%s.groovy", name))));
	}

	public static synchronized Context get() {
		Context ctx = CTX.get();

		if (ctx == null) {
			CTX.set(ctx = createContext());
		}

		return ctx;
	}

	public static Object eval(String str) {
		try {
			return ENGINE.createTemplate(str).make(get().getVars()).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void invoke(String method) {
		MAIN.invokeMethod(method, new Object[]{get()});
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
		ctx.addVar("_", MAIN);

		log.info("Context Handler: env=[{}] system=[{}]",
			System.getenv(ARTEMIS_PROFILE_ENV), System.getProperty(ARTEMIS_PROFILE_SYS_PROP));
		log.info("Context Handler: PROFILE=[{}] BASE_URL=[{}]", ctx.getProfile(), ctx.getBaseUrl());

		return ctx;
	}
}
