package org.devocative.artemis.ctx;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.devocative.artemis.Context;
import org.devocative.artemis.http.HttpRequestData;

@Getter
@RequiredArgsConstructor
public class BeforeSendData {
	private final Context ctx;
	private final HttpRequestData data;
}
