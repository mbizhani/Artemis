package org.devocative.artemis.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.devocative.artemis.TestFailedException;
import org.devocative.artemis.xml.XBaseRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.devocative.artemis.Util.asMap;

public class HttpFactory {
	private static final ThreadLocal<CloseableHttpClient> CURRENT_CLIENT = new ThreadLocal<>();

	private final String baseUrl;

	// ------------------------------

	public HttpFactory(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	// ------------------------------

	public void shutdown() {
		try {
			final CloseableHttpClient httpclient = CURRENT_CLIENT.get();
			httpclient.close();
			CURRENT_CLIENT.remove();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public HttpRequest create(XBaseRequest rq) {
		final String url = rq.getUrl();
		final Map<String, String> urlParams = asMap(rq.getUrlParams());

		final String finalUrl;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			finalUrl = url;
		} else if (!url.matches("^[/?].*")) {
			throw new TestFailedException(rq.getId(), "Invalid URL: '%s'", url);
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
				throw new TestFailedException(rq.getId(), "Invalid URI to Build");
			}
		}

		CloseableHttpClient httpclient = CURRENT_CLIENT.get();
		if (httpclient == null) {
			httpclient = HttpClients.createDefault();
			CURRENT_CLIENT.set(httpclient);
		}

		return new HttpRequest(rq.getId(), rq.getGlobalId(), new HttpUriRequestBase(rq.getMethod().name(), uri), httpclient);
	}
}
