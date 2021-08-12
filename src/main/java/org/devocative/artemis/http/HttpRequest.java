package org.devocative.artemis.http;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.devocative.artemis.ALog;
import org.devocative.artemis.TestFailedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class HttpRequest {
	private final String rqId;
	private final HttpUriRequestBase request;
	private final CloseableHttpClient httpClient;

	private final StringBuilder builder = new StringBuilder();

	// ------------------------------

	public void setHeaders(Map<String, String> headers) {
		headers.forEach(request::addHeader);
		if (!headers.isEmpty()) {
			builder.append("\n").append("HEADERS = ").append(headers);
		}
	}

	public void setBody(String body) {
		request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON, "UTF-8", false));
		builder.append("\n").append(body);
	}

	public void setFormParams(Map<String, String> formParams) {
		if (!formParams.isEmpty()) {
			final List<BasicNameValuePair> pairs = formParams
				.entrySet()
				.stream()
				.map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
			request.setEntity(new UrlEncodedFormEntity(pairs));
			builder.append("\n").append("FORM = ").append(formParams);
		}
	}

	public void send(Consumer<HttpResponse> responseConsumer) {
		ALog.info("RQ({}): {} - {}{}", rqId, request.getMethod(), getUri(), builder.toString());

		final long start = System.currentTimeMillis();
		try (final CloseableHttpResponse rs = httpClient.execute(request)) {
			final int code = rs.getCode();
			final String contentType = rs.getEntity().getContentType();
			final String body = getBody(rs);

			ALog.info("RS({}): {} ({}) - {} [{} ms]\n\tContentType: {}\n\t{}",
				rqId, request.getMethod(), code, request.getRequestUri(),
				System.currentTimeMillis() - start, contentType, body);

			responseConsumer.accept(new HttpResponse(code, contentType, body));

		} catch (HttpHostConnectException e) {
			throw new TestFailedException(rqId, "Unknown Host");
		} catch (IOException e) {
			throw new TestFailedException(rqId, e.getMessage());
		}
	}

	// ------------------------------

	private String getUri() {
		try {
			return request.getUri().toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public String getBody(CloseableHttpResponse response) {
		try {
			return new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			//throw new TestFailedException(rqId, e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
