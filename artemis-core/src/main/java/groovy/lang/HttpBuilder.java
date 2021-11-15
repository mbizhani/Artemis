package groovy.lang;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static groovy.lang.Artemis.jsonify;

public class HttpBuilder {
	private static final HttpBuilder INST = new HttpBuilder();

	private HttpBuilder() {
	}

	public static HttpBuilder get() {
		return INST;
	}

	// ---------------

	public Rq get(String url) {
		return new Rq("GET", url);
	}

	public Rq post(String url) {
		return new Rq("POST", url);
	}

	public Rq put(String url) {
		return new Rq("PUT", url);
	}

	public Rq patch(String url) {
		return new Rq("PATCH", url);
	}

	public Rq delete(String url) {
		return new Rq("DELETE", url);
	}

	// ------------------------------

	@RequiredArgsConstructor
	public static class Rq {
		private final String method;
		private final String url;
		private CharSequence body;

		private final Map<String, String> headers = new HashMap<>();
		private final Map<String, String> formParams = new HashMap<>();

		public Rq header(String key, String value) {
			headers.put(key, value);
			return this;
		}

		public Rq body(Object body) {
			return body(jsonify(body));
		}

		public Rq body(CharSequence body) {
			if (!formParams.isEmpty()) {
				throw new RuntimeException("Invalid State: setting both form and body");
			}
			this.body = body;
			return this;
		}

		public Rq form(String key, String value) {
			if (body != null) {
				throw new RuntimeException("Invalid State: setting both form and body");
			}
			formParams.put(key, value);
			return this;
		}

		public Rs send() {
			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
				final HttpUriRequestBase rq = new HttpUriRequestBase(method, URI.create(url));
				if (body != null) {
					rq.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON, "UTF-8", false));
				} else {
					final List<BasicNameValuePair> pairs = formParams
						.entrySet()
						.stream()
						.map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
						.collect(Collectors.toList());
					rq.setEntity(new UrlEncodedFormEntity(pairs));
				}
				headers.forEach(rq::addHeader);

				try (final CloseableHttpResponse rs = httpclient.execute(rq)) {
					final String rsBody = new BufferedReader(new InputStreamReader(rs.getEntity().getContent(), StandardCharsets.UTF_8))
						.lines()
						.collect(Collectors.joining("\n"));
					return new Rs(rs.getCode(), rsBody);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// ------------------------------

	@Getter
	@RequiredArgsConstructor
	public static class Rs {
		private final int code;
		private final String body;

		public Object getBodyAsObject() {
			return Artemis.objectify(body);
		}
	}
}
