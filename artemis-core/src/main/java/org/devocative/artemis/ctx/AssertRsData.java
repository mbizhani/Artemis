package org.devocative.artemis.ctx;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.devocative.artemis.Context;

@Getter
@RequiredArgsConstructor
public class AssertRsData {
	private final Context ctx;
	private final String rqId;
	private final Object rsBody;
}
