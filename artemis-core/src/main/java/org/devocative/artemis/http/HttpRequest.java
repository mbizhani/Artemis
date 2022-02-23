package org.devocative.artemis.http;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.devocative.artemis.ALog;
import org.devocative.artemis.StatisticsContext;
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
	private final String rqGlobalId;
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
		ALog.info("RQ: {} - {}{}", request.getMethod(), getUri(), builder.toString());

		final HttpClientContext context = HttpClientContext.create();
		final long start = System.currentTimeMillis();
		try (final CloseableHttpResponse rs = httpClient.execute(request, context)) {
			final long duration = System.currentTimeMillis() - start;
			final int code = rs.getCode();
			final String contentType = rs.getEntity().getContentType();
			final String body = getBody(rs);

			final String cookiesPart;
			if (context.getCookieStore().getCookies().size() > 0) {
				final String cookies = context.getCookieStore().getCookies()
					.stream()
					.map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
					.collect(Collectors.joining(","));
				cookiesPart = String.format("\n\tCookies: %s", cookies);
			} else {
				cookiesPart = "";
			}

			if (!body.isEmpty()) {
				ALog.info("RS: {} ({}) - {} [{} ms]\n\tContentType: {}{}\n\t{}",
					request.getMethod(), code, request.getRequestUri(),
					duration, contentType, cookiesPart, body);
			} else {
				ALog.info("RS: {} ({}) - {} [{} ms]\n\t{} - (EMPTY BODY){}",
					request.getMethod(), code, request.getRequestUri(),
					duration, contentType != null && !contentType.trim().isEmpty() ? "ContentType: " + contentType : "(No ContentType)",
					cookiesPart);
			}

			StatisticsContext.add(rqGlobalId, request.getMethod(), request.getRequestUri(), code, duration);

			responseConsumer.accept(new HttpResponse(code, contentType, body));

		} catch (HttpHostConnectException e) {
			throw new TestFailedException(rqId, String.format("Unknown Host (%s)", getUri()));
		} catch (IOException e) {
			throw new TestFailedException(rqId, String.format("%s (%s)", e.getMessage(), getUri()));
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
