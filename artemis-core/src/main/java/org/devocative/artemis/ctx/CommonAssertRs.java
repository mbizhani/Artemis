package org.devocative.artemis.ctx;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class CommonAssertRs {
	private final LinkedHashMap<String, Consumer<AssertRsData>> handlers;

	public CommonAssertRs(LinkedHashMap<String, Consumer<AssertRsData>> handlers) {
		this.handlers = handlers;
	}

	public CommonAssertRs matches(String regex, Consumer<AssertRsData> consumer) {
		handlers.put(regex, consumer);
		return this;
	}

	public void other(Consumer<AssertRsData> consumer) {
		handlers.put(".*", consumer);
	}
}
