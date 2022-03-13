package org.devocative.artemis.http;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class HttpRequestData {
	private Map<String, String> headers;
	private Map<String, String> formParams;
	private String body;
}
