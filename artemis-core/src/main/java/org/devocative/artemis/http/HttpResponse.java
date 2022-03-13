package org.devocative.artemis.http;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class HttpResponse {
	private final int code;
	private final String contentType;
	private final String body;
	private final Map<String, String> cookies;
}
