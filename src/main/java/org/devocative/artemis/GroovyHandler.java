package org.devocative.artemis;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;

import java.io.InputStreamReader;

public class GroovyHandler {
	private final static ThreadLocal<Context> CTX = new ThreadLocal<>();

	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();
	private static final Script MAIN;

	// ------------------------------

	static {
		GroovyShell shell = new GroovyShell();
		MAIN = shell.parse(new InputStreamReader(GroovyHandler.class.getResourceAsStream("/artemis.groovy")));
	}

	// ------------------------------

	public static synchronized Context getContext() {
		Context ctx = CTX.get();

		if (ctx == null) {
			ctx = new Context();
			MAIN.invokeMethod("init", new Object[]{ctx});
			CTX.set(ctx);
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
}
