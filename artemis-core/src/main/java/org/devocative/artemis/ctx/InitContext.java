package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;

public class InitContext {
	private final Context ctx;
	private final ContextConfig config;

	public InitContext(Context ctx, ContextConfig config) {
		this.ctx = ctx;
		this.config = config;
	}

	public Context getCtx() {
		return ctx;
	}

	public ContextConfig getConfig() {
		return config;
	}
}
