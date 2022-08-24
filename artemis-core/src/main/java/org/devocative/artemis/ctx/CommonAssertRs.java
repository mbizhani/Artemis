package org.devocative.artemis.ctx;

import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CommonAssertRs {
	private final LinkedHashMap<Pattern, Consumer<AssertRsData>> handlers;

	public CommonAssertRs(LinkedHashMap<Pattern, Consumer<AssertRsData>> handlers) {
		this.handlers = handlers;
	}

	public CommonAssertRs matches(String regex, Consumer<AssertRsData> consumer) {
		handlers.put(Pattern.compile(regex), consumer);
		return this;
	}

	public void other(Consumer<AssertRsData> consumer) {
		handlers.put(Pattern.compile(".*"), consumer);
	}
}
