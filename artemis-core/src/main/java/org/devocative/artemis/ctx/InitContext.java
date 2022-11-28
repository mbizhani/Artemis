package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;

public class InitContext {
	private final Context ctx;
	private final IAspects aspects;

	// ------------------------------

	public InitContext(Context ctx, IAspects aspects) {
		this.ctx = ctx;
		this.aspects = aspects;
	}

	// ------------------------------

	public Context getCtx() {
		return ctx;
	}

	public IAspects getAspects() {
		return aspects;
	}
}
