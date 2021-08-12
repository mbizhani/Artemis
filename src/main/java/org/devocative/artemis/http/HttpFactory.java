package org.devocative.artemis.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.devocative.artemis.TestFailedException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class HttpFactory {
	private final String baseUrl;
	private final CloseableHttpClient httpclient;

	public HttpFactory(String baseUrl) {
		this.baseUrl = baseUrl;
		this.httpclient = HttpClients.createDefault();
	}

	public void shutdown() {
		try {
			httpclient.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public HttpRequest create(String rqId, String method, String url, Map<String, String> urlParams) {
		final String finalUrl;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			finalUrl = url;
		} else {
			finalUrl = baseUrl + url;
		}

		final URI uri;
		if (urlParams.isEmpty()) {
			uri = URI.create(finalUrl);
		} else {
			try {
				final URIBuilder builder = new URIBuilder(finalUrl);
				urlParams.forEach(builder::addParameter);
				uri = builder.build();
			} catch (URISyntaxException e) {
				throw new TestFailedException(rqId, "Invalid URI to Build");
			}
		}

		return new HttpRequest(rqId, new HttpUriRequestBase(method, uri), httpclient);
	}
}
