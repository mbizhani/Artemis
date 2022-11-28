package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;

import java.util.HashMap;
import java.util.Map;

public class AssertRsData {
	private final Context ctx;
	private final String rqId;
	private final Object rsBody;

	private final Map<String, String> rqIdVars = new HashMap<>();

	// ------------------------------

	public AssertRsData(Context ctx, String rqId, Object rsBody) {
		this.ctx = ctx;
		this.rqId = rqId;
		this.rsBody = rsBody;

		final String[] parts = rqId.split("[_]");
		for (String part : parts) {
			final String[] kv = part.split("[$]");
			if (kv.length == 2) {
				rqIdVars.put(kv[0], kv[1]);
			}
		}
	}

	// ------------------------------

	public Context getCtx() {
		return ctx;
	}

	public String getRqId() {
		return rqId;
	}

	public Object getRsBody() {
		return rsBody;
	}

	public String rqIdVar(String key) {
		if (rqIdVars.containsKey(key)) {
			return rqIdVars.get(key);
		} else {
			throw new RuntimeException(String.format("Pattern Not Found in Request Id (%s): '%s$VALUE'", rqId, key));
		}
	}
}
