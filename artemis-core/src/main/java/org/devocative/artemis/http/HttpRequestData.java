package org.devocative.artemis.http;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class HttpRequestData {
	private final String rqId;
	private final String url;
	private final String method;

	private Map<String, CharSequence> headers;
	private Map<String, CharSequence> formParams;
	private CharSequence body;
}
