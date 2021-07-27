package org.devocative.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.devocative.artemis.xml.*;
import org.devocative.artemis.xml.method.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ArtemisExecutor {
	private final String name;
	private final ObjectMapper mapper;

	// ------------------------------

	public ArtemisExecutor() {
		this("artemis");
	}

	public ArtemisExecutor(String name) {
		this.name = name;
		mapper = new ObjectMapper();
		ContextHandler.init(name);
	}

	// ------------------------------

	public void execute() throws Exception {
		final XStream xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.processAnnotations(new Class[]{XScenario.class, XGet.class, XPost.class, XPut.class, XDelete.class});
		xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

		final XScenario scenario = (XScenario) xStream.fromXML(
			ArtemisExecutor.class.getResourceAsStream(String.format("/%s.xml", name)));
		final XScenario proxy = (XScenario) Proxy.create(scenario);
		log.info("Scenario: {}", proxy.getName());

		for (XBaseRequest rq : proxy.getRequests()) {
			initRq(rq);
			sendRq(rq);
		}
	}

	public String getProfile() {
		return ContextHandler.get().getProfile();
	}

	// ------------------------------

	private void initRq(XBaseRequest rq) {
		final Context ctx = ContextHandler.get();
		int addVars = 0;
		for (XVar var : rq.getVars()) {
			ctx.addVar(var.getName(), var.getTheValue());
			addVars++;
		}
		if (addVars > 0) {
			log.info("RQ({}) - [{}] var(s) added to context", rq.getId(), addVars);
		}

		if (rq.getCall() != null) {
			ContextHandler.invoke(rq.getCall());
			log.info("RQ({}) - call: {}", rq.getId(), rq.getCall());
		}
	}

	private void sendRq(XBaseRequest rq) throws Exception {
		final Context ctx = ContextHandler.get();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			final HttpUriRequestBase httpRq = new HttpUriRequestBase(rq.getMethod().name(), createURI(rq));

			final Map<String, Object> rqAndRs = new HashMap<>();
			ctx.addVar(rq.getId(), rqAndRs);

			final StringBuilder builder = new StringBuilder();

			if (rq.shouldHaveBody()) {
				final XBody body = rq.getBody();
				final List<XParam> params = rq.getParams();

				if (body != null) {
					final String content = body.getContent().trim();
					httpRq.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON, "UTF-8", false));
					//rqAndRs.put("rq", body.isJson() ? json(rq.getId(), content) : content);
					builder
						.append("\n")
						.append(content);
				} else if (!params.isEmpty()) {
					final List<BasicNameValuePair> httpParams = params.stream()
						.map(p -> new BasicNameValuePair(p.getName(), p.getTheValue()))
						.collect(Collectors.toList());
					httpRq.setEntity(new UrlEncodedFormEntity(httpParams));
					//rqAndRs.put("rq", params.stream().collect(Collectors.toMap(XParam::getName, XParam::getValue)));

					builder.append("\n");
					params.forEach(p -> builder.append(p.getName()).append(" = ").append(p.getTheValue()));
				} else {
					log.warn("RQ({}): Sending POST/PUT without body or any param", rq.getId());
				}
			}

			rq.getHeaders().forEach(h -> httpRq.addHeader(h.getName(), h.getTheValue()));

			log.info("RQ({}): {} - {}{}", rq.getId(), rq.getMethod(), rq.getUrl(), builder.toString());
			try (final CloseableHttpResponse rs = httpclient.execute(httpRq)) {
				if (rq.getAssertRs() == null) {
					log.warn("RQ({}) - No <assertRs/>!", rq.getId());
					rq.setAssertRs(new XAssertRs());
				}

				final XAssertRs assertRs = rq.getAssertRs();

				assertCode(rq, rs);

				final String contentType = rs.getEntity().getContentType();
				if (contentType != null && (contentType.contains("text") || contentType.contains("json"))) {
					final String rsBodyAsStr = new BufferedReader(new InputStreamReader(rs.getEntity().getContent(), StandardCharsets.UTF_8))
						.lines()
						.collect(Collectors.joining("\n"));

					log.info("RS({}): {} ({}) - {}\n\tContentType: {}\n\t{}",
						rq.getId(), rq.getMethod(), rs.getCode(), rq.getUrl(), contentType, rsBodyAsStr);

					if (!rsBodyAsStr.isEmpty()) {
						final Object rsAsObject = assertRs.isJson() ? json(rq.getId(), rsBodyAsStr) : rsBodyAsStr;
						rqAndRs.put("rs", rsAsObject);
						assertProperties(rq, rsAsObject);
					}
				}
			}
		}
	}

	private void assertProperties(XBaseRequest rq, Object rsAsObj) {
		final XAssertRs assertRs = rq.getAssertRs();

		if (assertRs.getProperties() != null) {
			final String[] properties = assertRs.getProperties().split(",");

			if (rsAsObj instanceof Map) {
				final Map rsAsMap = (Map) rsAsObj;
				for (String property : properties) {
					final String prop = property.trim();
					if (!rsAsMap.containsKey(prop)) {
						throw new TestFailedException(rq.getId(), "Invalid Property in RS Object: [%s]", prop);
					}
				}
			} else {
				throw new TestFailedException(rq.getId(), "Invalid RS Type for Asserting Properties");
			}
		}
	}

	// ---------------

	private URI createURI(XBaseRequest rq) {
		final Context ctx = ContextHandler.get();
		final String url = ctx.getBaseUrl() + rq.getUrl(); //TODO
		final List<XParam> params = rq.getParams();

		final URI uri;
		if (rq.shouldHaveBody() || params.isEmpty()) {
			uri = URI.create(url);
		} else {
			try {
				final URIBuilder builder = new URIBuilder(url);
				params.forEach(p -> builder.addParameter(p.getName(), p.getTheValue()));
				uri = builder.build();
			} catch (URISyntaxException e) {
				throw new TestFailedException(rq.getId(), "Invalid URI to Build");
			}
		}

		return uri;
	}

	private void assertCode(XBaseRequest rq, CloseableHttpResponse rs) {
		final XAssertRs assertRs = rq.getAssertRs();
		if (assertRs.getStatus() != null && !assertRs.getStatus().equals(rs.getCode())) {
			throw new TestFailedException(rq.getId(), "Invalid RS Code: Expected %s, Got %s",
				assertRs.getStatus(), rs.getCode());
		}
	}

	private Object json(String id, String content) {
		try {
			return mapper.readValue(content, Object.class);
		} catch (JsonProcessingException e) {
			throw new TestFailedException(id, "Invalid JSON Format:\n%s", content);
		}
	}
}
