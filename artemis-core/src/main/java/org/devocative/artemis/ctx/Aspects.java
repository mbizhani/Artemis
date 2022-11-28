package org.devocative.artemis.ctx;

import org.devocative.artemis.Context;
import org.devocative.artemis.ContextHandler;
import org.devocative.artemis.http.HttpRequestData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Aspects implements IAspects {
	private Consumer<BeforeSendData> beforeSend;
	private final LinkedHashMap<Pattern, Consumer<AssertRsData>> handlers = new LinkedHashMap<>();

	// ------------------------------

	@Override
	public void createBeforeSend(Consumer<BeforeSendData> beforeSend) {
		if (this.beforeSend == null) {
			this.beforeSend = beforeSend;
		}
	}

	public void callBeforeSend(HttpRequestData data) {
		if (beforeSend != null) {
			final Context ctx = ContextHandler.get();
			beforeSend.accept(new BeforeSendData(ctx, data));
		}
	}

	// ---------------

	@Override
	public void createCommonAssertRs(Consumer<CommonAssertRs> consumer) {
		final CommonAssertRs commonAssertRs = new CommonAssertRs(handlers);
		consumer.accept(commonAssertRs);
	}

	public void callCommonAssertRs(String rqId, Object rsBody) {
		final Context ctx = ContextHandler.get();

		for (Map.Entry<Pattern, Consumer<AssertRsData>> entry : handlers.entrySet()) {
			if (entry.getKey().matcher(rqId).find()) {
				entry.getValue().accept(new AssertRsData(ctx, rqId, rsBody));
				break;
			}
		}
	}
}
