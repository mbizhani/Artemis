package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;

public class InitContext {
	private final Context ctx;
	private final Aspects aspects;

	public InitContext(Context ctx, Aspects aspects) {
		this.ctx = ctx;
		this.aspects = aspects;
	}

	public Context getCtx() {
		return ctx;
	}

	public Aspects getAspects() {
		return aspects;
	}
}
