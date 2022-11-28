package org.devocative.artemis.ctx;

import java.util.function.Consumer;

public interface IAspects {
	void createBeforeSend(Consumer<BeforeSendData> beforeSend);

	void createCommonAssertRs(Consumer<CommonAssertRs> consumer);
}
