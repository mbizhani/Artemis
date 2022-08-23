package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;
import org.devocative.artemis.ContextHandler;
import org.devocative.artemis.http.HttpRequestData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ContextConfig {
	private Consumer<HttpRequestData> beforeSend;
	private final LinkedHashMap<String, Consumer<AssertRsData>> handlers = new LinkedHashMap<>();

	// ------------------------------

	public void createBeforeSend(Consumer<HttpRequestData> beforeSend) {
		if (this.beforeSend == null) {
			this.beforeSend = beforeSend;
		}
	}

	public void callBeforeSend(HttpRequestData data) {
		if (beforeSend != null) {
			beforeSend.accept(data);
		}
	}

	// ---------------

	public void createCommonAssertRs(Consumer<CommonAssertRs> consumer) {
		final CommonAssertRs commonAssertRs = new CommonAssertRs(handlers);
		consumer.accept(commonAssertRs);
	}

	public void callCommonAssertRs(String rqId, Object rsBody) {
		final Context ctx = ContextHandler.get();

		for (Map.Entry<String, Consumer<AssertRsData>> entry : handlers.entrySet()) {
			if (rqId.matches(entry.getKey())) {
				entry.getValue().accept(new AssertRsData(ctx, rqId, rsBody));
				break;
			}
		}
	}
}
